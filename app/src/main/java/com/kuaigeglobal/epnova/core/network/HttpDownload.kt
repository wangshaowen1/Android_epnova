package com.kuaigeglobal.epnova.core.network

import com.nhsd.basic.ext.launchNoCrash
import kotlinx.coroutines.MainScope
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import okio.use
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object HttpDownload {
    private val progressListeners by lazy { HashMap<String?, ProgressListener>() }
    private val client by lazy {
        RetrofitClient.httpClient.newBuilder()
            .addNetworkInterceptor { chain ->
                val url = chain.request().url.toString()
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(originalResponse.body) { bytesRead, contentLength, done ->
                        MainScope().launchNoCrash {
                            progressListeners[url]?.onProgress(bytesRead, contentLength)
                        }
                    })
                    .build()
            }
            .build()
    }

    fun download(
        url: String,
        targetFile: File,
        progressListener: ProgressListener
    ) {
        progressListeners[url] = progressListener
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(url, false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onResult(url, false, "HTTP error ${response.code}")
                    return
                }
                try {
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    onResult(url, true, null)
                } catch (e: IOException) {
                    onResult(url, false, e.message)
                }
            }
        })
    }

    private fun onResult(url: String?, success: Boolean, reason: String?) {
        MainScope().launchNoCrash {
            val listener = progressListeners[url]
            progressListeners.remove(url)
            listener?.onResult(success, reason)
        }
    }
}

interface ProgressListener {
    fun onProgress(bytesRead: Long, contentLength: Long)
    fun onResult(success: Boolean, reason: String?)
}

class ProgressResponseBody(
    private val responseBody: ResponseBody?,
    private val progressListener: (bytesRead: Long, contentLength: Long, done: Boolean) -> Unit
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = responseBody?.contentType()
    override fun contentLength(): Long = responseBody?.contentLength() ?: 0

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = responseBody?.source()?.let { source(it).buffer() }
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                // bytesRead == -1 when finished
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener(totalBytesRead, contentLength(), bytesRead == -1L)
                return bytesRead
            }
        }
    }
}