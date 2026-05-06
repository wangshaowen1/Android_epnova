package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 解锁弹窗-章节解锁信息响应
 */
data class StoryUnlockInfoData(
    val workId: String?,
    val storyId: String?,
    /** 剧集序号，从 1 起 */
    val episodeNo: Int?,
    /** 付费类型：1=金币 */
    val costType: Int?,
    /** 解锁所需金币数 */
    val costNums: Int?,
    /** 当前用户金币余额 */
    val userCoinsBalance: Long?,
    /** 是否支持广告解锁：0=不支持，1=支持 */
    val canUnlockByAd: Int?,
    /** 今日该作品广告解锁剩余次数 */
    val adUnlockRemainCount: Int?,
    /** 看一条广告可解锁的集数 */
    val adUnlockStoryNums: Int?,
    /** 距下一天（用户时区零点）的剩余秒数，用于展示「明天重置」倒计时 */
    val nextDayRemainSeconds: Long?
)

data class StoryUnlockStatusData(
    /** 解锁弹窗-章节解锁信息 */
    val episodeNo: Int?,
    /** 锁定状态：0=免费集 1=已解锁 2=未解锁付费集*/
    val lockStatus: Int?
)
