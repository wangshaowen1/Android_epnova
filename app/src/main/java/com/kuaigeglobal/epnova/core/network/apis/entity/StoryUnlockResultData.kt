package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 解锁结果响应（金币解锁 & 广告解锁结果轮询共用）
 *
 * 广告轮询时：status 有效（0=等待 SSV，1=成功，2=失败），remainCoinsBalance 为 null
 * 金币解锁时：status 始终为 null，adUnlockRemainCount 为 null
 */
data class StoryUnlockResultData(
    /** 解锁状态（仅广告轮询）：0=等待，1=成功，2=失败；金币解锁时为 null */
    val status: Int?,
    /** 本次成功解锁的剧集 ID 列表；广告轮询 status=0 时为 null */
    val unlockStoryIds: List<String>?,
    /** 解锁后剩余金币余额；广告解锁时为 null */
    val remainCoinsBalance: Long?,
    /** 今日广告解锁剩余次数；金币解锁时为 null */
    val adUnlockRemainCount: Int?
)

data class ResultAdPollData(
    /** 奖励发放状态0=还在处理  1=下发成功  2=下发失败 */
    val rewardStatus: Int?,
)
