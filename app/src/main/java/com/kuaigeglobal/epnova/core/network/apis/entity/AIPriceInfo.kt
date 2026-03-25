package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * {
 *     "strategyCode": "策略编号",
 *     "strategyType": "策略类型-扣费环节（1-资产图、2-分镜图、3-分镜视频）单位：个（如果是分镜则是代表秒）",
 *     "pointNum": "算力点数"
 * }
 */
data class AIPriceInfo(
    /**
     * 策略编号
     */
    val strategyCode: String?,
    /**
     * 策略类型-扣费环节（1-资产图、2-分镜图、3-分镜视频）单位：个（如果是分镜则是代表秒）
     */
    val strategyType: Int?,
    /**
     * 算力点数
     */
    val pointNum: Int?
)