package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 签到任务列表响应
 */
data class RewardCheckInListData(
    /** 当前连续签到天数（用于展示 "Checked in for X day"） */
    val checkinDays: Int?,
    /** 今日是否已签到：0-否，1-是 */
    val todayCheckedIn: Int?,
    /** 今日签到后是否已观看广告：0-否，1-是 */
    val todayAdWatched: Int?,
    /** 下次刷新时间戳（毫秒） */
    val nextRefreshTime: Long?,
    val adRewardCoins: Int?,//签到广告奖励金币数
    /** 7天签到任务列表（按 day 升序） */
    val taskList: List<CheckInTaskItem>?
)

data class CheckInTaskItem(
    /** 周期内天序号（1~7） */
    val day: Int?,
    /** 该天奖励金币数量 */
    val rewardNum: Int?,
    /** 任务图标 URL */
    val iconUrl: String?,
    /** 当日签到状态：0-未完成，1-已完成，2-今日待签到 */
    val status: Int?
)
