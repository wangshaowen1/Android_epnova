package com.kuaigeglobal.epnova.core.manager

import android.text.TextUtils
import com.nhsd.basic.Session
import com.kuaigeglobal.epnova.common.Constants

object AdStrategyManager {

    @JvmStatic
    fun getDefaultObjectId(): String {
        return getObjectId(Session.getInstance().channel)
    }

    fun getObjectId(channel: String): String {
        if (TextUtils.isEmpty(channel)) {
            return Constants.DEFAULT_CHANNEL_ID
        }
        if (channel.startsWith(Session.AdsChannel.TOUTIAO)) {
            return "11"
        } else if (channel.startsWith(Session.AdsChannel.KUAISHOU)) {
            return "12"
        } else if (channel.startsWith(Session.AdsChannel.BAIDU)) {
            return "13"
        } else if (channel.startsWith(Session.AdsChannel.WANGYI)) {
            return "14"
        } else if (channel.startsWith(Session.AdsChannel.TENCENT)) {
            return "15"
        } else {
            return when (channel) {
                "qq" -> "2"
                "huawei" -> "3"
                "xiaomi" -> "4"
                "vivo" -> "5"
                "oppo" -> "6"
                "baidu" -> "7"
                "qihu360", "qh360" -> "8"
                "xsc" -> "9"
                "rongyao" -> "10"
                "admin" -> "1000"
                else -> Constants.DEFAULT_CHANNEL_ID
            }
        }
    }
}