package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 解锁弹窗-广告前置准备响应
 */
data class StoryUnlockAdPrepareData(
    /** 传给 AdMob SDK setCustomData() 的 JWT 字符串 */
    val customData: String?,
    /** 本次广告唯一标识，用于轮询 /ad/result 接口 */
    val nonce: String?
)
