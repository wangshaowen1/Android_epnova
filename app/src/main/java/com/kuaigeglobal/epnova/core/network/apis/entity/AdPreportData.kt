package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 解锁弹窗-广告前置准备响应
 */
data class AdPreportData(
    /** 传给广告 SDK setCustomData() 的 JWT 字符串，服务端签名防篡改 */
    val customData: String?,
    /** 本次广告的唯一标识（即 JWT 的 jti = adFlowId），用于后续轮询 */
    val nonce: String?
)
