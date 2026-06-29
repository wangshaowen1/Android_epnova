package com.kuaigeglobal.epnova.common.utils

import com.kuaigeglobal.epnova.BuildConfig

object DebugUtils {
    val ipsName by lazy {
        arrayOf(
            "廖嘉宁：192.168.12.82",
            "杨帆：192.168.11.60",
            "郭义明：192.168.11.44",
            "刘子帆：192.168.11.91",
            "测试外网：epnova-test.kwaige.cn",
            "预发布：120.24.240.28:9010",
//            "正式环境：api.epnovai.com"
            "正式环境：epnova-aw-api.epnovai.com"
        )
    }
    val ips by lazy {
        arrayOf(
            "http://192.168.12.82:8088",
            "http://192.168.11.60:8088",
            "http://192.168.11.44:8088",
            "http://192.168.11.91:8088",
            "https://epnova-test.kwaige.cn",
            "http://120.24.240.28:7010",
//            "https://api.epnovai.com"
            "https://epnova-aw-api.epnovai.com"
        )
    }

    private val debugFlavor by lazy {
        arrayOf(
            "preview",
            "develop"
        )
    }

    @JvmStatic
    fun isDebug(): Boolean {
        if (BuildConfig.DEBUG) return true
        val flavorEnv = BuildConfig.FLAVOR_env
        for (i in debugFlavor.indices) {
            if (debugFlavor[i].contains(flavorEnv)) {
                return true
            }
        }
        return false
    }
}