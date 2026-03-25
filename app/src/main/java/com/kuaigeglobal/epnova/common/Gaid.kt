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
}