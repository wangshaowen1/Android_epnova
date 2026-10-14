package com.kuaigeglobal.epnova.core.network.sse

import android.util.Log
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryStreamEventData
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

/**
 * 适配 V1.8.5 task-output SSE 协议。
 *
 * 只解析 SSEEvent.Message.data 的业务 JSON，并按 Message.type 分发 chunk/final/error。
 * 不在这里转换 PollSummaryData/PollScriptData，避免网络层依赖页面业务模型。
 */
class TaskOutputSseHandler(
    private val scope: CoroutineScope,
    private val onEvent: (TaskOutputEvent) -> Unit
) {

    private var job: Job? = null

    // 外部 cancel、final/error、异常断开都通过该标记防止重复回调。
    private val isFinished = AtomicBoolean(false)

    // 只有收到 final/error 后服务端关闭连接才算正常关闭。
    private var hasReceivedTerminalEvent = false
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    fun start(flow: Flow<SSEvent>) {
        cancel()
        isFinished.set(false)
        hasReceivedTerminalEvent = false
        _isStreaming.value = true
        job = scope.launch(Dispatchers.Main) {
            try {
                flow.collect { event ->
                    if (isFinished.get()) return@collect
                    when (event) {
                        is SSEvent.Open -> onEvent(TaskOutputEvent.Open)
                        is SSEvent.Message -> handleMessage(event)
                        is SSEvent.Error -> handleConnectionError(event.throwable)
                        is SSEvent.Close -> handleClose()
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Stream cancelled normally")
            } catch (e: Exception) {
                handleConnectionError(e)
            } finally {
                _isStreaming.value = false
            }
        }
    }

    private fun handleMessage(event: SSEvent.Message) {
        Log.d(TAG, "receive event type=${event.type}, id=${event.id}, data=${event.data.previewLog()}")
        when (event.type) {
            EVENT_CHUNK -> {
                // chunk 的业务 JSON 只取 content；续传 id 使用 SSE 帧上的 event.id。
                // 不能从 data 里猜 id，否则遇到 heartbeat/final 的分片日志会丢续传点。
                val payload = decodePayload(event)
                onEvent(TaskOutputEvent.Chunk(event.id, payload.content.orEmpty()))
            }

            EVENT_HEARTBEAT -> onEvent(TaskOutputEvent.Heartbeat(event.id))

            EVENT_FINAL -> {
                // final 是业务正常终止事件。这里不解析 result 的业务结构，交给 Step1/Step2 的 mapper。
                val payload = decodePayload(event)
                hasReceivedTerminalEvent = true
                Log.d(TAG, "receive final id=${event.id}, hasResult=${payload.result != null}")
                onEvent(TaskOutputEvent.Final(event.id, payload))
                finish()
            }

            EVENT_ERROR -> {
                // event:error 是后端业务失败，和 SSEEvent.Error 网络异常分开处理。
                // errorCode/errorType/hitWords 保留下发给页面，首版页面主要展示 errorMessage。
                val payload = decodePayload(event)
                hasReceivedTerminalEvent = true
                Log.d(
                    TAG,
                    "receive business error id=${event.id}, errorCode=${payload.errorCode}, errorType=${payload.errorType}, errorMessage=${payload.errorMessage}"
                )
                onEvent(
                    TaskOutputEvent.Error(
                        lastEventId = event.id,
                        errorCode = payload.errorCode,
                        errorType = payload.errorType,
                        errorMessage = payload.errorMessage,
                        hitWords = payload.hitWords
                    )
                )
                finish()
            }

            else -> {
                Log.w(TAG, "Ignore unknown event type: ${event.type}")
            }
        }
    }

    private fun decodePayload(event: SSEvent.Message): StoryStreamEventData {
        return runCatching {
            // 只解析 data: 后面的大括号 JSON。event/type/id 都已经在 SSEEvent.Message 上。
            taskStreamJson.decodeFromString(StoryStreamEventData.serializer(), event.data)
        }.onSuccess { payload ->
            Log.d(
                TAG,
                "decode success type=${event.type}, id=${event.id}, code=${payload.code}, contentLength=${payload.content?.length ?: 0}, hasResult=${payload.result != null}"
            )
        }.onFailure { throwable ->
            Log.e(TAG, "decode failed type=${event.type}, id=${event.id}, data=${event.data.previewLog()}", throwable)
        }.getOrThrow()
    }

    private fun handleClose() {
        if (hasReceivedTerminalEvent) {
            finish()
            onEvent(TaskOutputEvent.Close)
        } else {
            // 服务端在 final/error 前断开，说明内容没有完整结束；页面重试时应回到 session 续连。
            handleConnectionError(ApiException(-1, "服务端异常断开连接，内容未生成完整"))
        }
    }

    private fun handleConnectionError(throwable: Throwable?) {
        if (isFinished.compareAndSet(false, true)) {
            Log.e(TAG, "connection error: ${throwable?.message}", throwable)
            _isStreaming.value = false
            onEvent(TaskOutputEvent.ConnectionError(throwable))
            job?.cancel()
        }
    }

    private fun finish() {
        if (isFinished.compareAndSet(false, true)) {
            _isStreaming.value = false
            job?.cancel()
        }
    }

    fun cancel() {
        // 页面退出或用户重新生成时主动取消，只断开前端连接，不向后端取消任务。
        isFinished.set(true)
        _isStreaming.value = false
        job?.cancel()
        job = null
    }

    companion object {
        private const val TAG = "TaskOutputSseHandler"
        private const val EVENT_CHUNK = "CHUNK"
        private const val EVENT_HEARTBEAT = "HEARTBEAT"
        private const val EVENT_FINAL = "FINAL"
        private const val EVENT_ERROR = "ERROR"

        const val EVENT_ERROR_CODE_STREAM_HEARTBEAT_TIMEOUT = "STREAM_HEARTBEAT_TIMEOUT"

        private val taskStreamJson = Json {
            ignoreUnknownKeys = true
        }

        private fun String.previewLog(): String {
            return if (length <= MAX_LOG_LENGTH) this else take(MAX_LOG_LENGTH) + "...(truncated, length=$length)"
        }

        private const val MAX_LOG_LENGTH = 1000
    }
}
