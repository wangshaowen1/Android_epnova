package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 签到上报响应
 */
data class RewardCheckInReportData(
    /** 本次签到的周期天序号（1~7） */
    val day: Int?,
    /** 本次签到获得的金币数量 */
    val rewardNum: Int?,
    /** 当前连续签到天数（用于刷新标题 "Checked in for N day(s)"） */
    val checkinDays: Int?
)
