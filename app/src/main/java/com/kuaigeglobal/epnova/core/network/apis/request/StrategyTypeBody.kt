package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * @param strategyType 策略类型-扣费环节（1-资产图、2-分镜图、3-动态漫分镜生视频、4-小说解析、5-解说漫分镜生视频）单位：个
 */
data class StrategyTypeBody(
    /**
     * 策略类型-扣费环节（1-资产图、2-分镜图、3-动态漫分镜生视频、4-小说解析、5-解说漫分镜生视频）单位：个
     */
    val strategyType: Int?,
    val workId: String? = null,
    val storyId: String? = null,
) : UserTokenBody()