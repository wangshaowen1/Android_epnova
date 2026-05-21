package com.kuaigeglobal.epnova.core.network.apis.entity

import com.google.gson.annotations.SerializedName

/**
 * 解锁弹窗-广告前置准备响应
 */
data class AdPreportData(
    /** 传给广告 SDK setCustomData() 的 JWT 字符串，服务端签名防篡改 */
    val customData: String?,
    /** 本次广告的唯一标识（即 JWT 的 jti = adFlowId），用于后续轮询 */
    val nonce: String?,
    /** Unity Ads 竞价广告位加载所需的竞价广告内容 */
    @SerializedName(value = "adMarkup", alternate = ["ad_markup"])
    val adMarkup: String? = null,
    /** Unity Ads 竞价广告位加载所需的广告对象 ID */
    @SerializedName(value = "objectId", alternate = ["object_id"])
    val objectId: String? = null
)
