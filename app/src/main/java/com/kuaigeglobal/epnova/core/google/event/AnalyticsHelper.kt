package com.kuaigeglobal.epnova.core.google.event

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.kuaigeglobal.epnova.App
import com.nhsd.basic.utils.Logger

/**
 * Firebase埋点工具类（无需手动 init）
 *
 * 调用示例：
 * AnalyticsHelper.logEvent(context, "play_song", mapOf("song_id" to "123"))
 */
object AnalyticsHelper {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    private fun getAnalytics(): FirebaseAnalytics {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(App.instance)
        }
        return firebaseAnalytics!!
    }

    /** 上报事件 */
    fun logEvent(eventName: String, params: Map<String, Any?>? = null) {
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putString(key, value.toString())
            }
        }
        Logger.d("FirebaseAnalytics埋点", "logEvent: $eventName, $params")
        getAnalytics().logEvent(eventName, bundle)
    }

    /** 设置用户属性 */
    fun setUserProperty(name: String, value: String?) {
        getAnalytics().setUserProperty(name, value)
    }

    /** 设置用户ID */
    fun setUserId(userId: String?) {
        getAnalytics().setUserId(userId)
    }
}
