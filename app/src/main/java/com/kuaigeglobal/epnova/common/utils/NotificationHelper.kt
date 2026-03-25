package com.kuaigeglobal.epnova.common.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.AppUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.nhsd.basic.ext.getResString

object NotificationHelper {
    private const val CHANNEL_ID = "recovery_channel"
    private const val CHANNEL_NAME = "应用通知"

    fun sendRecoveryNotification(content: String?, title: String? = R.string.app_name.getResString()) {
        if (AppUtils.isAppForeground()) {
            return
        }
        val context = topActivity ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 创建渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        // 2. 关键点：获取启动 Intent
        // getLaunchIntentForPackage 会根据 AndroidManifest 找到带有 LAUNCHER 属性的 Activity
        // 并且它默认包含了 FLAG_ACTIVITY_NEW_TASK，这是恢复任务栈的关键
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            // 确保不会创建多个实例，而是把已有的任务栈推到前台
            // 如果 App 进程已被杀，系统会自动走正常的冷启动流程
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

        // 3. 适配 Android 12+ 的 PendingIntent 标志位
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        // 如果 intent 为空（极少见），则不执行
        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(context, 0, intent, flags)
        } else null

        // 4. 构建并发送通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // 必须有图标
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 点击后消失
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationId = System.currentTimeMillis().toInt()
        manager.notify(notificationId, builder.build())
//        manager.notify(1, builder.build())
    }
}