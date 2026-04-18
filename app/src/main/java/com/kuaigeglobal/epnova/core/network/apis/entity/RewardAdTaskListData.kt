package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 观看广告任务列表响应
 */
data class RewardAdTaskListData(
    /** 下次任务刷新时间戳（毫秒），前端用于展示倒计时 */
    val nextRefreshTime: Long?,
    /** 广告任务列表（共5条，按 seq 升序） */
    val taskList: List<AdTaskItem>?
)

data class AdTaskItem(
    /** 任务序号（1~5），按此顺序依次解锁 */
    val seq: Int?,
    /** 任务记录ID（广告前置上报时作为 bizId 传入） */
    val taskRecordId: String?,
    /** 奖励金币数量 */
    val rewardNum: Int?,
    /** 任务图标 URL */
    val iconUrl: String?,
    /**
     * 任务状态：
     * 0 - 待完成（Watch 可点击）
     * 1 - 已完成/Claimed（置灰）
     * 2 - 未解锁（置灰，未完成前序任务）
     */
    val taskStatus: Int?
)
