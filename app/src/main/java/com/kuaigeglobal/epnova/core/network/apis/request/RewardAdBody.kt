package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 剧集解锁激励视频广告完成确认请求体。
 */
data class StoryUnlockAdCompleteBody(
    /** 作品 ID */
    val workId: String?,
    /** 剧集 ID */
    val storyId: String?,
    /** 广告流水 ID，由对应 prepare 接口返回 */
    val nonce: String?
) : UserTokenBody()

/**
 * 广告任务激励视频广告前置准备请求体。
 */
data class RewardAdTaskPrepareBody(
    /** 广告任务列表返回的 taskRecordId */
    val taskRecordId: String?
) : UserTokenBody()

/**
 * 签到/广告任务激励视频广告完成确认请求体。
 */
data class RewardAdCompleteBody(
    /** 广告流水 ID，由对应 prepare 接口返回 */
    val nonce: String?
) : UserTokenBody()
