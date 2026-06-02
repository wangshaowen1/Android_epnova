package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 一次性奖励任务列表响应。
 *
 * 当前服务端只返回“打开消息通知”任务；如果后续扩展更多一次性任务，
 * 前端需要基于 taskCode 增加对应的点击行为分发。
 */
data class OnceTaskListData(
    val taskList: List<OnceTaskItem>?
)

data class OnceTaskItem(
    /** 任务编码，当前用于标识打开消息通知任务 */
    val taskCode: String?,
    /** 任务名称 */
    val taskName: String?,
    /** 任务描述 */
    val taskDesc: String?,
    /** 任务图标 URL */
    val iconUrl: String?,
    /** 奖励类型：1=金币 */
    val rewardType: Int?,
    /** 奖励数量 */
    val rewardNum: Long?,
    /** 奖励数量文案 */
    val rewardLabel: String?,
    /** 是否展示：0=不展示，1=展示 */
    val showFlag: Int?
)
