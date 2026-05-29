package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 剧集解锁结果响应。
 */
data class StoryUnlockResultData(
    /** 解锁状态 */
    val status: Int?,
    /** 本次成功解锁的剧集 ID 列表 */
    val unlockStoryIds: List<String>?,
    /** 解锁后剩余金币余额 */
    val remainCoinsBalance: Long?,
    /** 今日广告解锁剩余次数 */
    val adUnlockRemainCount: Int?
)
