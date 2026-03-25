package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:闪验登录验证
 * @Data: 2026/2/10/周二  17:40
 * @Version: 1.0
 * *****************************************
 */
data class FlashTestPreFetchNumberInfo(
    //"innerCode":1022,
    val innerCode: Int? = null,
    //"innerDesc":"success"
    val innerDesc: String? = null,
    //"message":"预取号成功",
    val message: String? = null,
    //"number":"173****7223",
    val number: String? = null,
    //"telecom":"CTCC", CMCC（中国移动）；CUCC（中国联通）；CTCC（中国电信）；CMHK(中国移动香港) ；Unknown_Operator（无 SIM 卡或非三网运营商）
    val telecom: String? = null,
    //"protocolName":"天翼账号服务与隐私协议",
    val protocolName: String? = null,
    //"protocolUrl":"https:\/\/e.189.cn\/sdk\/agreement\/detail.do?hidetop=true"}
    val protocolUrl: String? = null
) {

    fun serviceName(): String? {
        return when (telecom) {
            "CMCC" -> "中国移动"
            "CUCC" -> "中国联通"
            "CTCC" -> "中国电信"
            "CMHK" -> "中国移动香港"
            else -> null
        }
    }


    fun authenticationService(): String? {
        return when (telecom) {
            "CMCC" -> "中国移动统一认证服务条款"
            "CUCC" -> "中国联通统一认证服务条款"
            "CTCC" -> "中国电信统一认证服务条款"
            "CMHK" -> "中国移动香港统一认证服务条款"
            else -> null
        }
    }
}