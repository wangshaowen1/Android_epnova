package com.kuaigeglobal.epnova.core.network.sse

import android.util.Log
import com.kuaigeglobal.epnova.core.db.entity.SSEMessage
import com.kuaigeglobal.epnova.core.network.SSEvent
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

class SSEStreamHandler(
    private val scope: CoroutineScope,
    private val processor: SseChunkProcessor
) {

    private var job: Job? = null

    // 优化1：使用 AtomicBoolean 彻底根除多线程并发回调导致的状态错乱
    private val isFinished = AtomicBoolean(false)
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    // 1. 新增一个标志位，记录当前流是否已经触发过 Start
    private var hasTriggeredStart = false

    private var hasReceivedBusinessOver = false

    private fun ensureStartedSafely() {
        if (!hasTriggeredStart) {
            hasTriggeredStart = true
            processor.onSSEStart()
        }
    }

    fun start(flow: Flow<SSEvent>) {
        cancel() // 开始前先清理上一次的资源
        isFinished.set(false)
        hasTriggeredStart = false
        _isStreaming.value = true
        hasReceivedBusinessOver = false
        job = scope.launch(Dispatchers.Main) {
            try {
                flow.collect { event ->
                    // 只要标记结束，立即丢弃后续僵尸数据
                    if (isFinished.get()) return@collect

                    when (event) {
                        is SSEvent.Open -> {
                            // 3. 正常收到 Open 时触发
                            ensureStartedSafely()
                        }

                        is SSEvent.Message -> {
                            ensureStartedSafely()
                            val data = event.data
                            try {
                                val sseMessage = Json.decodeFromString<SSEMessage>(data)
                                if (sseMessage.code == SSEMessage.TYPE_ERROR) {
                                    Log.e("[SSE]", "receive msg: parsing error for ${sseMessage.content ?: ""}")
                                    handleError(ApiException(-1, sseMessage.content))
                                } else {
                                    if (sseMessage.isOver()) {
                                        hasReceivedBusinessOver = true
                                        Log.e("[SSE]", "receive msg: sseMessage.isOver")
                                        handleComplete()
                                    } else {
                                        processor.onSSEChunk(sseMessage.content ?: "")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("[SSE]", "receive msg: parsing error for $data", e)
                                handleError(e)
                            }
                        }

                        is SSEvent.Error -> {
                            handleError(event.throwable)
                        }

                        is SSEvent.Close -> {
                            if (hasReceivedBusinessOver) {
                                handleComplete()
                            } else {
                                Log.e("[SSE]", "❌ HTTP 流已关闭，但未收到 isOver，属于异常中断！")
                                handleError(ApiException(-1, "服务端异常断开连接，内容未生成完整"))
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 协程被 cancel 时的正常抛出，无需作为报错处理
                Log.d("[SSE]", "Stream cancelled normally")
            } catch (e: Exception) {
                // 兜底外层异常
                handleError(e)
            } finally {
                // 优化2：兜底处理服务端静默断开（EOF）的情况
                // 如果走到 finally 时既没有触发报错，也没有触发 Complete，说明是异常中断，在此强行触发结束
                if (isFinished.compareAndSet(false, true)) {
                    if (hasReceivedBusinessOver) {
                        processor.onSSEComplete()
                    } else {
                        processor.onSSEError(ApiException(-1, "流异常意外终止"))
                    }
                }
                _isStreaming.value = false
            }
        }
    }

    private fun handleComplete() {
        // compareAndSet：如果当前是 false，则设为 true 并返回 true；否则返回 false
        if (isFinished.compareAndSet(false, true)) {
            _isStreaming.value = false
            processor.onSSEComplete()
            // 彻底断开流连接
//            job?.cancel()
        }
    }

    private fun handleError(e: Throwable?) {
        if (isFinished.compareAndSet(false, true)) {
            _isStreaming.value = false
            processor.onSSEError(e)
            // 发生错误时切断连接
            job?.cancel()
        }
    }

    // 优化3：统一的 Cancel 收口逻辑
    fun cancel() {
        // 如果是外部主动调用 Cancel，将其强行标记为已结束，阻止 finally 块误触发 Complete 回调
        isFinished.set(true)
        _isStreaming.value = false
        job?.cancel()
        job = null
    }
}