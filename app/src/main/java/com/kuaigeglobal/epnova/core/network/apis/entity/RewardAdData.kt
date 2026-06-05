package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 激励视频广告前置准备响应。
 */
data class RewardAdPrepareData(
    /** 本次广告的唯一标识，即 adFlowId */
    val nonce: String?,
    /** AdMob 激励广告单元 ID，空时客户端使用本地默认广告位 */
    val adUnitId: String?
)

/**
 * 激励视频广告结果查询响应。
 */
data class RewardAdCompleteData(
    /** 广告场景：1=签到激励广告，2=福利任务激励广告，3=剧集解锁激励广告 */
    val adScene: Int?,
    /** 广告流水处理状态：0=待完成，1=已完成，2=已失效 */
    val adFlowStatus: Int?,
    /** 本次成功解锁的剧集 ID 列表，仅剧集解锁场景返回 */
    val unlockStoryIds: List<String>?,
    /** 今日该作品广告解锁剩余次数，仅剧集解锁场景返回 */
    val adUnlockRemainCount: Int?
)
