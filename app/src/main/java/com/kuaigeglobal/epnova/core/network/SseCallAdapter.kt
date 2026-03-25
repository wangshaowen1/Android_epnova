package com.kuaigeglobal.epnova.core.network

import cn.android.security.APISecurity
import com.blankj.utilcode.util.ConvertUtils
import com.nhsd.basic.utils.Logger
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.core.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okio.Buffer
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class SseCallAdapter(private val responseType: Type) :
    CallAdapter<EventSource, Flow<SSEvent>> { // 返回 Flow

    /**
     * 请求体加密
     *
     * @param newBuilder 新请求Builder
     * @param method     请求Method
     * @param originBody 原请求未加密body
     */
    @Throws(java.lang.Exception::class)
    private fun encryptRequestBody(newBuilder: Request.Builder, method: String, originBody: RequestBody?) {
        if (originBody != null) {
            val requestBuffer = Buffer()
            originBody.writeTo(requestBuffer)
            val bytes = requestBuffer.readByteArray()
            requestBuffer.close()
            newBuilder.method(
                method,
                RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    APISecurity.sign(App.instance, User.get().id, ConvertUtils.bytes2String(bytes))
                )
            )
        }
    }


    override fun adapt(call: Call<EventSource>): Flow<SSEvent> = callbackFlow {
        val factory = RetrofitClient.sseEventFactory
        var eventSource: EventSource? = null
        val request = call.request()
        val listener = object : EventSourceListener() {
            override fun onClosed(eventSource: EventSource) {
                Logger.e("[SSE] 服务器主动关闭连接")
                trySendBlocking(SSEvent.Close(true))
                close() // 正常关闭通道
            }

            override fun onOpen(source: EventSource, response: Response) {
                try {
                    if (!isClosedForSend) { // 检查通道状态
                        trySendBlocking(SSEvent.Open(response))
                    }
                } catch (e: Exception) {
                    Logger.e("[SSE] onOpen发送异常: ${e.stackTraceToString()}")
                    trySendBlocking(SSEvent.Close(false))
                    close(e)
                }
            }

            override fun onEvent(source: EventSource, id: String?, type: String?, data: String) {
                try {
                    if (!isClosedForSend) { // 检查通道状态
                        val result =trySendBlocking(SSEvent.Message(data, type, id))
                        if (result.isFailure) {
                            Logger.e("[SSE] 警告：Flow 缓冲区已满，可能发生丢包！")
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("[SSE] onEvent发送异常: ${e.stackTraceToString()}")
                }
            }

            override fun onFailure(source: EventSource, t: Throwable?, response: Response?) {
                try {
                    Logger.e("[SSE] 链接失败 ：onFailure=>response=${response},e=${t}")
                    val exception = t ?: IOException("Unknown error")
                    when {
                        response?.code == 204 -> {
                            trySendBlocking(SSEvent.Close(true))
                            close()
                        }

                        exception is StreamResetException && exception.errorCode == ErrorCode.CANCEL -> {
                            Logger.e("连接已提前关闭，忽略异常StreamResetException")
                            trySendBlocking(SSEvent.Close(true))
                            close()
                        }

                        // 明确处理 Socket 已关闭的情况
                        exception.message?.contains("Socket is closed") == true -> {
                            Logger.e("连接已提前关闭，作为异常处理并关闭通道")
                            trySendBlocking(SSEvent.Error(exception)) // 1. 通知下游报错
                            close(exception) // 2. 必须关闭 Flow 通道！释放内存
                        }

                        exception.message?.contains("Socket closed") == true -> {
                            Logger.e("连接已提前关闭，作为异常处理并关闭通道")
                            trySendBlocking(SSEvent.Error(exception)) // 1. 通知下游报错
                            close(exception) // 2. 必须关闭 Flow 通道！释放内存
                        }

                        else -> {
                            trySendBlocking(SSEvent.Error(exception))
                            close(exception)
//                            CrashReport.postCatchedException(exception)
                        }
                    }
                } catch (e: Exception) {
//                    CrashReport.postCatchedException(e)
                    Logger.e("[SSE] onFailure处理异常: ${e.stackTraceToString()}")
                }
            }
        }

        try {
            eventSource = factory.newEventSource(request, listener)
        } catch (e: Exception) {
            Logger.e("[SSE] 初始化异常: ${e.stackTraceToString()}")
            close(e)
        }

        awaitClose {
            try {
                if (!isClosedForSend) { // 双重检查
                    eventSource?.cancel()
                    Logger.e("[SSE] 客户端主动关闭连接")
                }
            } catch (e: Exception) {
                Logger.e("[SSE] awaitClose异常: ${e.stackTraceToString()}")
            }
        }
    }

    override fun responseType(): Type = responseType
}

sealed class SSEvent {
    data class Open(val response: Response) : SSEvent()
    data class Message(val data: String, val type: String?, val id: String?) : SSEvent()
    data class Error(val throwable: Throwable) : SSEvent()
    data class Close(val isServiceClose: Boolean) : SSEvent()
}

class SseCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        // 检查返回类型是否为 Flow<SSEvent>
        if (getRawType(returnType) != Flow::class.java) return null
        val flowType = getParameterUpperBound(0, returnType as ParameterizedType)
        if (getRawType(flowType) != SSEvent::class.java) return null
        return SseCallAdapter(flowType)
    }
}