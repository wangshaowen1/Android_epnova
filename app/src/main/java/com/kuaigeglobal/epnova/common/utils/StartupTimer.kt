package com.kuaigeglobal.epnova.common.utils

import android.os.SystemClock
import android.util.Log

object StartupTimer {
    private const val TAG = "AppStartup"
    private var startTime = 0L
    private var lastStepTime = 0L

    /**
     * 在 Application 的最开始调用
     */
    fun start() {
        startTime = SystemClock.elapsedRealtime()
        lastStepTime = startTime
        Log.d(TAG, "🏁 ======= 启动性能监控开始 =======")
    }

    /**
     * 记录一个步骤的耗时
     * @param stepName 步骤名称
     */
    fun step(stepName: String) {
        val now = SystemClock.elapsedRealtime()
        val stepCost = now - lastStepTime
        val totalCost = now - startTime
        
        // 打印格式：[步骤名] 耗时: xx ms | 累计: xx ms
        // 使用 Log.e 或 w 让它在 Logcat 中变色，更容易看到
        Log.w(TAG, "⏱️ [$stepName] \t耗时: ${stepCost}ms \t| 累计: ${totalCost}ms")
        
        lastStepTime = now
    }

    /**
     * 结束监控
     */
    fun end() {
        val total = SystemClock.elapsedRealtime() - startTime
        Log.d(TAG, "🏁 ======= 启动结束，总耗时: ${total}ms =======")
    }
}