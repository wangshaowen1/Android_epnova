package com.kuaigeglobal.epnova.common

import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.kuaigeglobal.epnova.App

object Gaid {
    private var gaid: String? = null

    @JvmStatic
    fun getGaid(): String? {
        val gaid = gaid
        if (gaid.isNullOrEmpty()) {
            val adInfo: AdvertisingIdClient.Info = AdvertisingIdClient.getAdvertisingIdInfo(App.instance)
            this.gaid = adInfo.id
        }
        return gaid
    }

    /**
     * 兼容没有google服务的手机
     * 保证发起请求
     */
    @JvmStatic
    fun safeGetGaid(): String {
        val gaid = gaid ?: ""
        if (gaid.isEmpty()) {
            try {
                val adInfo: AdvertisingIdClient.Info = AdvertisingIdClient.getAdvertisingIdInfo(App.instance)
                this.gaid = adInfo.id
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return gaid
    }
}