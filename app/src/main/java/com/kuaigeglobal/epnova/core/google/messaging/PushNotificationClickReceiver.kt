package com.kuaigeglobal.epnova.core.google.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.router.AppRouter

class PushNotificationClickReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_FROM_PUSH = "from_push"
        const val EXTRA_SCHEME = "scheme"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_FROM_PUSH, false)) {
            EventManager.onFirebaseTrack(FirebaseEvent.PUSH_CLICK)
        }

        val scheme = intent.getStringExtra(EXTRA_SCHEME)
        if (!scheme.isNullOrEmpty()) {
            // 复用 AppLaunchJump：App 存活时拉回前台后跳转，冷启动时等待启动完成再跳转。
            AppRouter.jump(scheme)
            return
        }

        // 无跳转 URI 的普通通知只恢复 App，不强制重走 Splash 或清空任务栈。
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }?.let(context::startActivity)
    }
}
