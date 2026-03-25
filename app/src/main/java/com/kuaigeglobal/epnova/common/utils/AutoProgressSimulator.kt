package com.kuaigeglobal.epnova.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 自动多阶段进度模拟器 (纯整数版)
 */
class AutoProgressSimulator(
    private val scope: CoroutineScope,
    // 回调：(是否显示, 标题, 进度百分比整数)
    private val onUpdate: (Boolean, String, Int) -> Unit
) {
    private var progressJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    @Volatile
    private var currentTitle: String = ""

    /**
     * 启动自动流程
     * 核心参数：每 100ms 增加 1%，即每秒 10%
     */
    fun start(vararg titles: String, step: Int = 10, intervalMs: Long = 1000L) {
        if (titles.isEmpty()) return
        stop()
        isRunning.set(true)

        progressJob = scope.launch {
            val titleList = titles.toList()

            // 1. 处理前面的阶段 (目标 100%)
            for (i in 0 until titleList.size - 1) {
                if (!coroutineContext.isActive || !isRunning.get()) return@launch

                runStageLoop(titleList[i], targetMax = 100, step = step, intervalMs = intervalMs)

                if (isRunning.get()) delay(200) // 衔接停顿
            }

            // 2. 处理最后一个阶段 (目标 99%，跑完卡住)
            if (coroutineContext.isActive && isRunning.get()) {
                runStageLoop(titleList.last(), targetMax = 99, step = step, intervalMs = intervalMs)
            }
        }
    }

    private suspend fun runStageLoop(title: String, targetMax: Int, step: Int, intervalMs: Long) {
        currentTitle = title
        var progress = 0

        onUpdate(true, title, progress)

        while (currentCoroutineContext().isActive && isRunning.get()) {
            if (progress >= targetMax) {
                delay(100)
                if (targetMax == 100) break
                continue
            }

            delay(intervalMs)
            progress += step

            if (progress > targetMax) progress = targetMax

            onUpdate(true, title, progress)
        }
    }

    fun finish(onFinished: (() -> Unit)?=null) {
        if (!isRunning.get()) {
            onFinished?.invoke()
            return
        }
        progressJob?.cancel()
        progressJob = scope.launch {
            onUpdate(true, currentTitle, 100) // 强制 100%
            delay(200)
            isRunning.set(false)
            onUpdate(false, "", 0)
            onFinished?.invoke()
        }
    }

    fun stop() {
        if (isRunning.get()) {
            onUpdate(false, "", 0)
        }
        isRunning.set(false)
        progressJob?.cancel()
        progressJob = null
    }
}