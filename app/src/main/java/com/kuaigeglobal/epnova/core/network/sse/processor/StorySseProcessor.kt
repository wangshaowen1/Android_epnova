package com.kuaigeglobal.epnova.core.network.sse.processor

import com.kuaigeglobal.epnova.core.network.sse.SseChunkProcessor
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 故事内容解析器
 */
class StorySseProcessor(
    private val scope: CoroutineScope,
    private val onTitleBack: (String) -> Unit,
    private val onStoryUpdate: (String) -> Unit, // 只关注 story 内容
    private val onStart: () -> Unit,
    private val onComplete: (String) -> Unit,    // 完成时返回完整 JSON
    private val onErrorCallback: (Throwable?) -> Unit
) : SseChunkProcessor {

    private val fullSourceAccumulator = StringBuilder()

    private val parser = JsonSseFieldStreamer(
        onTitleUpdate = {title->
            onTitleBack.invoke(title)
        },
        onStoryUpdate = { content ->
            scope.launch(Dispatchers.Main.immediate) {
                Logger.d("[SSE]", "content: $content")
                onStoryUpdate(content)
            }
        },
        MD_THROTTLE_MS = 20L
    )

    override fun onSSEStart() {
        fullSourceAccumulator.setLength(0)
        scope.launch(Dispatchers.Main.immediate) { onStart() }
    }

    override fun onSSEChunk(chunk: String) {
        Logger.d("[SSE]", "Chunk: $chunk")
        fullSourceAccumulator.append(chunk) // 记录原始数据
        parser.onChunk(chunk)               // 解析字段
    }

    override fun onSSEComplete() {
        parser.finish()
        val fullSource = fullSourceAccumulator.toString()
        scope.launch(Dispatchers.Main.immediate) { onComplete(fullSource) }
    }

    override fun onSSEError(e: Throwable?) {
        scope.launch(Dispatchers.Main.immediate) { onErrorCallback(e) }
    }
}