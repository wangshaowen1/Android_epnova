package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 查询作品发行状态请求体
 */
data class WorkPublishStatusBody(
    /**
     * 作品 ID，后端接受 Long 和 String 类型
     */
    val workId: String?
) : UserTokenBody()

/**
 * 提交作品剧集集中审核请求体
 */
data class WorkPublishSubmitBody(
    /**
     * 作品 ID，后端接受 Long 和 String 类型
     */
    val workId: String?,
    /**
     * 审核批次 ID。为空表示新建批次，不为空表示提交到指定批次。
     */
    val auditBatchId: Long?,
    /**
     * 提交审核起始集数，包含。
     */
    val startEpisode: Int?,
    /**
     * 提交审核结束集数，包含。
     */
    val endEpisode: Int?
) : UserTokenBody()
