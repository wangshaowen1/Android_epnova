package com.kuaigeglobal.epnova.core.model

/**
 * H5 调用APP支付协议，由app调用接口获取支付参数并支付
 * APP 通过payWay 区分支付方式
 */
data class H5PayInfo(
    /**
     *  json字符串
     */
    val payInfo: String? = null,

    /**
     * wx||zfb
     */
    val payWay: String? = null,
    /**
     * 金额 单位厘
     */
    val amount: Long? = null
)