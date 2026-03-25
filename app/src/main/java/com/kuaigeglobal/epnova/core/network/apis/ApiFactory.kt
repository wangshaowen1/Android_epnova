package com.kuaigeglobal.epnova.core.network.apis

import com.kuaigeglobal.epnova.BuildConfig
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.nhsd.basic.ext.contain

object ApiFactory {
    val apiService: ApiService by lazy {
        val baseUrl = ((Config.get().baseUrlCustom ?: ApiConstants.BASE_URL) + "/snap-film/api/")
        RetrofitClient.create(baseUrl, ApiService::class.java)
    }

    val eventService: EventService by lazy {
        val remoteServer = arrayOf("preview", "production").contain(BuildConfig.FLAVOR_env)
        val baseUrl = if (remoteServer) "https://track.kuaigeai.cn" else {
            Config.get().baseUrlCustom ?: ApiConstants.BASE_URL
        }
        RetrofitClient.create(baseUrl, EventService::class.java)
    }
}