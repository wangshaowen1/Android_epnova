package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * @param strategyType 策略类型-扣费环节（1-资产图、2-分镜图、3-分镜视频）单位：个（如果是分镜则是代表秒）
 */
data class StrategyTypeBody(
    /**
     * 策略类型-扣费环节（1-资产图、2-分镜图、3-分镜视频）单位：个（如果是分镜则是代表秒）
     */
    val strategyType: Int?
) : UserTokenBody()