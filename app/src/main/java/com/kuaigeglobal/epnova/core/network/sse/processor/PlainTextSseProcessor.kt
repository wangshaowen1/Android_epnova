package com.kuaigeglobal.epnova.core.network.sse.processor

import android.util.Log
import kotlinx.coroutines.CoroutineScope

/**
 * 通用解析器
 */
class PlainTextSseProcessor(
    scope: CoroutineScope,
    throttleMs: Long = 20L,
    private val onStart: () -> Unit,
    private val onUpdate: (String) -> Unit,
    private val onComplete: () -> Unit,
    private val onErrorCallback: (Throwable?) -> Unit
) : BaseThrottleSseProcessor(scope, throttleMs) {

    private val buffer = StringBuilder(1024)

    // 1. 在同步方法中立刻清空 buffer，防止被后面的数据冲刷
    override fun onSyncInitInternal() {
        buffer.setLength(0)
    }

    // 2. 在异步方法中只做纯 UI 回调
    override fun onStartInternal() {
        onStart()
    }


    override fun onSSEChunk(chunk: String) {
        Log.e("[SSE]", "onSSEChunk=${chunk}")
        buffer.append(chunk)
        tryEmit()
    }

    override fun buildSnapshot(): String = buffer.toString()

    override fun onEmit(content: String) {
        onUpdate(content)
        Log.e("[SSE]", "onUpdate=${content}")
    }

    override fun onCompleteInternal() {
        onComplete()
    }

    override fun onSSEError(e: Throwable?) {
        onErrorCallback(e)
    }
}



