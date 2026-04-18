package com.kuaigeglobal.epnova.core.network

import android.os.Build
import android.text.TextUtils
import cn.android.security.APISecurity
import com.blankj.utilcode.util.CollectionUtils
import com.blankj.utilcode.util.ConvertUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.utils.EncryptUtils
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.ApiConstants
import com.kuaigeglobal.epnova.core.network.apis.AppHeader
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.Proxy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    private val debug = App.instance.isDebug

    private val mRetrofits = HashMap<String, Retrofit>()
    private val gson: Gson by lazy {
        GsonBuilder().registerTypeAdapter(Any::class.java, ObjectTypeAdapter()).create()
    }

    // OkHttpClient实例 - 使用lazy确保线程安全初始化
     val httpClient: OkHttpClient by lazy { initializeOkHttpClient() }
     val sseClient: OkHttpClient by lazy { initializeSseClient() }

     val sseEventFactory: EventSource.Factory by lazy {
        EventSources.createFactory(sseClient)
    }

    private fun initializeSseClient(): OkHttpClient {
        val protocols = listOf(Protocol.HTTP_1_1)
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                if ("GET" == original.method) {
                    return@addInterceptor chain.proceed(original)
                }

                val userToken = User.get().id ?: ""
                val requestBuilder = original.newBuilder()
                    .header("Cache-Control", "no-cache")
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream;charset=UTF-8")
                    .header("userToken", userToken)
                    .header("appHeader", gson.toJson(AppHeader()))

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .protocols(protocols)
            .readTimeout(0, TimeUnit.SECONDS) // SSE需要长连接，不设置读取超时
            .connectTimeout(10, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
            .build()
    }

    private fun initializeOkHttpClient(): OkHttpClient {
        val cache = Cache(File(App.instance.cacheDir, "HttpCache"), 1024 * 1024 * 100L)

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(MyTimeoutInterceptor())
            .addInterceptor { chain ->
                val original = chain.request()
                if ("GET" == original.method) {
                    return@addInterceptor chain.proceed(original)
                }

                val userToken = User.get().id ?: ""
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("userToken", userToken)
                    .header("userToken", userToken)
                    .header("X-Timezone", TimeZone.getDefault().id)
                    .header("language", Config.get().getSavedLanguage())
                    .header("appHeader", gson.toJson(AppHeader()))

                try {
                    //TODO 请求提加密,先调试的时候先关闭
                    //encryptRequestBody(requestBuilder, original.method, original.body)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .apply {
                // 调试模式下添加日志拦截器
                if (debug) {
                    val interceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(interceptor)
                }
            }
            .apply {
                // Android 29以下版本设置自定义SSL
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Q = Android 10 (API 29)
                    sslSocketFactory(createSSLContext().socketFactory, TrustAllCertsManager())
                }
            }
            .hostnameVerifier { hostname, session ->
                try {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                } catch (e: Throwable) {
                    true // 验证失败时允许连接（用于调试）
                }
            }
            .retryOnConnectionFailure(true)
            .proxy(if (debug) null else Proxy.NO_PROXY) // 非调试模式下禁用代理
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun createSSLContext(): SSLContext {
        val protocols = arrayOf("TLSv1.2", "TLSv1.1", "TLS", "SSL")

        for (protocol in protocols) {
            try {
                return SSLContext.getInstance(protocol).apply {
                    init(null, arrayOf(TrustAllCertsManager()), SecureRandom())
                }
            } catch (e: Exception) {
                // 继续尝试下一个协议
                continue
            }
        }

        throw RuntimeException("Failed to create SSL context. Tried: ${protocols.joinToString()}")
    }

    private fun encryptRequestBody(
        newBuilder: Request.Builder,
        method: String,
        originBody: RequestBody?
    ) {
        originBody?.let { body ->
            Buffer().use { buffer ->
                body.writeTo(buffer)
                val bytes = buffer.readByteArray()
                val userId = User.get().id ?: ""
                val dataString = ConvertUtils.bytes2String(bytes)
                if (debug) {
                    HttpLoggingInterceptor.Logger.DEFAULT.log("↓ BODY: $dataString")
                }
                val signedData = APISecurity.sign(App.instance, userId, dataString)
                newBuilder.method(
                    method,
                    signedData.toRequestBody("application/json".toMediaTypeOrNull())
                )
            }
        }
    }

    @Suppress("unused")
    private fun decodeString(str: String): String {
        return if (!TextUtils.isEmpty(str)) {
            try {
                URLEncoder.encode(str, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                ""
            }
        } else {
            ""
        }
    }

    fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addCallAdapterFactory(SseCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson)) // 使用自定义Gson
            .build()
    }

    fun <T> create(baseUrl: String, serviceClass: Class<T>): T {
        return mRetrofits.getOrPut(baseUrl) { createRetrofit(baseUrl) }.create(serviceClass)
    }

    // 内联函数简化API创建
    inline fun <reified T> create(baseUrl: String): T {
        return create(baseUrl, T::class.java)
    }

    // Gson TypeAdapter for handling number conversion issues
     class ObjectTypeAdapter : TypeAdapter<Any?>() {

        override fun write(out: JsonWriter, value: Any?) {
            when {
                value == null -> out.nullValue()
                else -> {
                    val typeAdapter = gson.getAdapter(value.javaClass) as TypeAdapter<Any?>
                    if (typeAdapter is ObjectTypeAdapter) {
                        out.beginObject()
                        out.endObject()
                    } else {
                        typeAdapter.write(out, value)
                    }
                }
            }
        }

        override fun read(reader: JsonReader): Any? {
            return when (val token = reader.peek()) {
                JsonToken.BEGIN_ARRAY -> readArray(reader)
                JsonToken.BEGIN_OBJECT -> readObject(reader)
                JsonToken.STRING -> reader.nextString()
                JsonToken.NUMBER -> parseNumber(reader.nextString())
                JsonToken.BOOLEAN -> reader.nextBoolean()
                JsonToken.NULL -> {
                    reader.nextNull()
                    null
                }
                else -> throw IllegalStateException("Unexpected token: $token")
            }
        }

        private fun readArray(reader: JsonReader): List<Any?> {
            val list = mutableListOf<Any?>()
            reader.beginArray()
            while (reader.hasNext()) {
                list.add(read(reader))
            }
            reader.endArray()
            return list
        }

        private fun readObject(reader: JsonReader): Map<String, Any?> {
            val map = LinkedTreeMap<String, Any?>()
            reader.beginObject()
            while (reader.hasNext()) {
                map[reader.nextName()] = read(reader)
            }
            reader.endObject()
            return map
        }

        private fun parseNumber(numberStr: String): Number {
            return if (numberStr.contains(".")) {
                numberStr.toDouble()
            } else {
                // 避免Long溢出，先转Double再转Long
                numberStr.toDouble().toLong()
            }
        }
    }

    class TrustAllCertsManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}