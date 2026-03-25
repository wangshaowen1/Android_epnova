package com.kuaigeglobal.epnova.common.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kuaigeglobal.epnova.R
import me.leolin.shortcutbadger.ShortcutBadger
import java.lang.reflect.Field
import java.lang.reflect.Method

object ShortcutBadgerUtils {

    private const val NOTIFICATION_ID = 10001
    private const val CHANNEL_ID = "badge_only"
    private const val CHANNEL_NAME = "Badge Count"

    // 缓存 Launcher 类名，避免多次查询 PackageManager
    private var launcherClassName: String? = null

    // 标记部分机型是否支持（避免反复尝试失败的方法）
    private var mIsHuaweiSupportedBadge: Boolean = true
    private var mIsHonorSupportedBadge: Boolean = true

    @JvmStatic
    fun applyCount(context: Context, badgeCount: Int) {
        // 1. 针对特定厂商的优先处理
        if (Build.MANUFACTURER.equals("Xiaomi", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ 小米推荐使用原生通知方式
                setCommonBadge(context, badgeCount)
            } else {
                setXiaomiBadge(context, badgeCount)
            }
            return
        } else if (Build.MANUFACTURER.equals("Huawei", true)) {
            if (mIsHuaweiSupportedBadge) {
                setHuaweiBadge(context, badgeCount)
                return
            }
        } else if (Build.MANUFACTURER.equals("Honor", true)) {
            if (mIsHonorSupportedBadge) {
                setHonorBadge(context, badgeCount)
                return
            }
        }

        // 2. 尝试使用 ShortcutBadger 库（处理三星、索尼、HTC等）
        try {
            val isSuccess = ShortcutBadger.applyCount(context, badgeCount)
            if (isSuccess) return
        } catch (e: Exception) {
            // ShortcutBadger 可能会抛出异常，捕获以防崩溃
            e.printStackTrace()
        }

        // 3. 兜底方案：使用 Android 8.0+ 系统原生通知角标
        // 很多新系统（如 Pixel, Moto, 新版 OV）依赖通知来显示点或数字
        setCommonBadge(context, badgeCount)
    }

    /**
     * 通用方案：通过发送一个“静默”通知来更新角标
     * 适用于 Android 8.0+ 以及部分厂商（如小米 Android 11+）
     */
    private fun setCommonBadge(context: Context, badgeCount: Int) {
        val notificationManager = NotificationManagerCompat.from(context)

        // 如果数量为0，取消通知，角标通常会自动消失
        if (badgeCount <= 0) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 优化：仅当 Channel 不存在时创建
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_MIN // MIN 级别：无声音、无震动、状态栏不显示图标（但在部分系统中仍可显示角标）
                ).apply {
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }

            // 动态获取启动 Intent，不要硬编码 Activity
            val intent = getLauncherIntent(context)

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = if (intent != null) {
                PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)
            } else null

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("新消息") // 建议放入 strings.xml: context.getString(R.string.new_message)
                .setContentText("您有 $badgeCount 条未读消息")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setNumber(badgeCount)
                .setOnlyAlertOnce(true)
                .setSilent(true)

            if (pendingIntent != null) {
                builder.setContentIntent(pendingIntent)
            }

            // 权限检查 (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            } else {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    private fun setHuaweiBadge(context: Context, count: Int) {
        try {
            val launchClassName = getLauncherClassName(context) ?: return
            val bundle = Bundle()
            bundle.putString("package", context.packageName)
            bundle.putString("class", launchClassName)
            bundle.putInt("badgenumber", count)

            context.contentResolver.call(
                Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                "change_badge",
                null,
                bundle
            )
        } catch (e: Exception) {
            mIsHuaweiSupportedBadge = false
            e.printStackTrace()
            // 失败后尝试通用方案
            setCommonBadge(context, count)
        }
    }

    private fun setHonorBadge(context: Context, count: Int) {
        try {
            val launchClassName = getLauncherClassName(context) ?: return
            // 荣耀分离后，URI 发生了变化，优先尝试新的
            val uriNew = Uri.parse("content://com.hihonor.android.launcher.settings/badge/")
            val uriOld = Uri.parse("content://com.huawei.android.launcher.settings/badge/")

            var targetUri = uriNew
            // 简单的检测逻辑：如果 contentResolver 无法识别新 URI 的 type，可能需要回退到旧 URI
            // 但直接 call 通常更直接，如果报错则捕获

            val extra = Bundle()
            extra.putString("package", context.packageName)
            extra.putString("class", launchClassName)
            extra.putInt("badgenumber", count)

            try {
                context.contentResolver.call(targetUri, "change_badge", null, extra)
            } catch (e: Exception) {
                // 如果新 URI 失败，尝试旧 URI
                context.contentResolver.call(uriOld, "change_badge", null, extra)
            }

        } catch (e: Exception) {
            mIsHonorSupportedBadge = false
            e.printStackTrace()
            setCommonBadge(context, count)
        }
    }

    private fun setXiaomiBadge(context: Context, count: Int) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 即使是反射设置，也建议使用和 setCommonBadge 相同的 Channel 和 ID，防止通知打架
            // 注意：小米旧系统可能不需要 Channel，但为了兼容性还是构建一个完整的 Notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("未读消息")
                .setContentText("您有 $count 条未读消息")
                .setAutoCancel(true)

            val notification = builder.build()

            // 反射修改 extraNotification
            val field: Field = notification.javaClass.getDeclaredField("extraNotification")
            val extraNotification: Any = field.get(notification)
            val method: Method = extraNotification.javaClass.getDeclaredMethod("setMessageCount", Int::class.javaPrimitiveType)
            method.invoke(extraNotification, count)

            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            // 失败回退通用
            setCommonBadge(context, count)
        }
    }

    /**
     * 动态获取当前 App 的启动 Activity 类名
     * 解决硬编码 SplashActivity 的问题
     */
    private fun getLauncherClassName(context: Context): String? {
        if (!launcherClassName.isNullOrEmpty()) {
            return launcherClassName
        }

        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setPackage(context.packageName)

        val pm = context.packageManager
        val list = pm.queryIntentActivities(intent, 0)

        // 通常取第一个即可
        val resolveInfo = list.firstOrNull()
        if (resolveInfo != null) {
            launcherClassName = resolveInfo.activityInfo.name
            return launcherClassName
        }
        return null
    }

    private fun getLauncherIntent(context: Context): Intent? {
        return context.packageManager.getLaunchIntentForPackage(context.packageName)
    }
}