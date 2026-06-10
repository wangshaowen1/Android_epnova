package com.kuaigeglobal.epnova.core.google.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.jsbridge.BridgeDataUtils
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.model.PushMessageInfo
import com.kuaigeglobal.epnova.feature.splash.SplashActivity
import com.nhsd.basic.utils.Logger


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.d("MyFirebaseMessagingService", "onMessageReceived: ${message.data}")
        val notification: RemoteMessage.Notification? = message.getNotification()
        val title = notification?.title ?: ""
        val body = notification?.body ?: ""
        val data: MutableMap<String?, String?>? = message.getData()
        val custom = data?.get("custom")
        Logger.d("MyFirebaseMessagingService", "onMessageReceived custom: $custom")
        val messageInfo = BridgeDataUtils.parseJson(custom, PushMessageInfo::class.java)
        Logger.d("MyFirebaseMessagingService", "onMessageReceived messageInfo: $messageInfo")
        messageInfo?.let {
            getNotification(it)
        }
        showNotification(title, body, messageInfo?.pushMsgJumpUri)
    }

    fun getNotification(messageInfo: PushMessageInfo?) {
        if (messageInfo != null) {
            if (messageInfo.pushMsgType == PushMessageInfo.WARNING) {
//                EventBus.getDefault().post(PushEvent(PushEvent.MSG_WARNING, messageInfo.pushMsgContent, messageInfo.msgId))
            }
            try {
                val uri = messageInfo.pushMsgJumpUri.toUri()
                val host = uri.host
                if (Constants.APP_SCHEME == uri.scheme && !TextUtils.isEmpty(host)) {
                    when (host) {
//                        "musicGenerated" -> {
//                            launchJump(uri)
//                            EventBus.getDefault().post(TrackCreatedEvent(uri.getQueryParameter("musicId")))
//                        }
//
//                        "musicGenerationFailed" -> EventBus.getDefault().post(TrackCreatedEvent())
//                        "publishMusic" ->                             //发布动态失败，刷新歌曲列表
//                            EventBus.getDefault().post(TrackPublishEvent())
//
//                        "unlockMusician" -> {
//                            launchJump(uri)
//                            EventBus.getDefault().post(UnlockArtistEvent())
//                        }
//
//                        "timbreCloneSuccess" -> {
//                            //kuaige://timbreCloneSuccess?name=音色名&avatar=音色头像   //AI音色克隆成功弹窗
//                            launchJump(uri)
//                            EventBus.getDefault().post(TimbreCloneSuccessEvent())
//                        }
//
//                        "timbreSwapSuccess" -> {
//                            //kuaige://timbreSwapSuccess?title=歌曲名&cover=歌曲封面&id=歌曲id&name=音色名     //换声成功弹窗
//                            launchJump(uri)
//                            EventBus.getDefault().post(CloneMusicSuccessEvent(uri.getQueryParameter("id")))
//                        }

                        else -> {}
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(title: String, body: String, scheme: String?) {
        val channelId = "default_notification_channel_id"
        val channelName = "Push Notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Android 8.0+ 必须先创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "App Push Notifications"
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_push", true)
            putExtra("scheme", scheme)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 构建通知
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    override fun onNewToken(token: String) {
        //1. 不是每次启动都会回调
        //onNewToken() 只会在以下情况下触发：
        //应用第一次在设备上安装并向 FCM 注册；
        //用户清除应用数据（或删除并重装应用）；
        //令牌被 Google 判定需要刷新（可能几天到几周一次）；
        //你手动调用了 FirebaseMessaging.getInstance().deleteToken() 或 FirebaseMessaging.getInstance().token。
        //如果设备上已经有有效 token，启动时不会重复回调。

        Logger.d("MyFirebaseMessagingService", "onNewToken received")
        super.onNewToken(token)

        // 推送 token 的保存、用户态判断、接口重试统一收敛到 EventManager；
        // Service 只处理 Firebase 生命周期回调，避免这里再次直调业务接口。
        EventManager.reportFcmToken(token)
    }
}
