package com.kuaigeglobal.epnova.core.network.sse.processor

import com.kuaigeglobal.epnova.core.network.sse.SseChunkProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseThrottleSseProcessor(
    private val scope: CoroutineScope,
    private val throttleMs: Long
) : SseChunkProcessor {

    @Volatile
    private var lastEmitTime = 0L

    @Volatile
    private var completed = false

    protected open fun onSyncInitInternal() {}

    protected abstract fun buildSnapshot(): String?
    protected abstract fun onEmit(content: String)

    protected open fun onStartInternal() {}
    protected open fun onCompleteInternal() {}

    override fun onSSEStart() {
        completed = false
        lastEmitTime = 0L

        //数据层面的清理必须立刻、同步执行！绝不能切线程！
        onSyncInitInternal()

        scope.launch(Dispatchers.Main.immediate) {
            onStartInternal()
        }
    }

    protected fun tryEmit(force: Boolean = false) {
        if (completed) return

        val now = System.currentTimeMillis()
        if (!force && now - lastEmitTime < throttleMs) return

        val snapshot = buildSnapshot() ?: return
        lastEmitTime = now

        scope.launch(Dispatchers.Main.immediate) {
            onEmit(snapshot)
        }
    }

    override fun onSSEComplete() {
        tryEmit(force = true)
        completed = true
        scope.launch(Dispatchers.Main.immediate) {
            onCompleteInternal()
        }
    }

    override fun onSSEError(e: Throwable?) {
        // 默认不处理
    }
}

