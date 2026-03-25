package com.kuaigeglobal.epnova.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 模拟进度条工具类
 * 逻辑：每隔 intervalMs 增加 step%，最高卡在 maxProgress，直到调用 finish()
 *
 * @param scope 协程作用域 (通常传 viewModelScope)
 * @param onProgress 进度回调 (0-100)
 */
class ProgressSimulator(
    private val scope: CoroutineScope,
    private val onProgress: (Int) -> Unit
) {
    private var progressJob: Job? = null
    private var currentProgress = 0
    private val isRunning = AtomicBoolean(false)

    /**
     * 开始模拟进度
     * @param startProgress 起始进度
     * @param step 每次增加的幅度
     * @param intervalMs 每次增加的时间间隔
     * @param maxFakeProgress 假进度的上限（未完成前卡在这个值，默认99）
     */
    fun start(
        startProgress: Int = 0,
        step: Int = 5,
        intervalMs: Long = 1000L,
        maxFakeProgress: Int = 99
    ) {
        stop() // 防止重复启动
        isRunning.set(true)
        currentProgress = startProgress
        
        progressJob = scope.launch {
            // 立即回调一次初始值
            onProgress(currentProgress)
            
            while (isActive && isRunning.get()) {
                delay(intervalMs)
                
                if (currentProgress < maxFakeProgress) {
                    currentProgress += step
                    // 修正边界，防止溢出上限
                    if (currentProgress > maxFakeProgress) {
                        currentProgress = maxFakeProgress
                    }
                    onProgress(currentProgress)
                }
            }
        }
    }

    /**
     * 任务完成，快速冲刺到 100% 并结束
     * @param onFinished 动画结束后执行的回调（通常用于关闭弹窗）
     */
    fun finish(onFinished: () -> Unit) {
        // 如果任务根本没开始，直接回调结束
        if (!isRunning.get()) {
            onFinished()
            return
        }
        
        // 取消循环任务，开启冲刺任务
        progressJob?.cancel()
        progressJob = scope.launch {
            // 1. 强制设为 100%
            currentProgress = 100
            onProgress(100)
            
            // 2. 稍微停留一下，让用户看清 "100%" (提升体验，200ms 比较合适)
            delay(200) 
            
            // 3. 标记结束并回调
            isRunning.set(false)
            onFinished()
        }
    }

    /**
     * 异常或强制停止（不显示 100%，直接销毁）
     */
    fun stop() {
        isRunning.set(false)
        progressJob?.cancel()
        progressJob = null
    }
}