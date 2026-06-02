package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * 作品发行状态
 */
data class WorkPublishStatusData(
    /**
     * 作品 ID
     */
    val workId: String? = null,
    /**
     * 正在审核中的批次号，若存在。
     */
    val auditBatchId: Long? = null,
    /**
     * 是否需要提交法定姓名/邮箱：0=否，1=是。
     */
    val needLegalInfoFlag: Int? = null,
    /**
     * 当前可提交剧集区间起始集数，包含。
     */
    val startEpisode: Int? = null,
    /**
     * 当前可提交剧集区间结束集数，包含。
     */
    val endEpisode: Int? = null,
    /**
     * 是否可以继续提交申请：0=否，1=是。
     */
    val canApplyFlag: Int? = null,
    /**
     * canApplyFlag=0 时前端直接展示的业务提示文案。
     */
    val reasonMsg: String? = null,
    /**
     * 发行入口类型：0=不可发布，1=H5首次发行页，2=原生发行页
     */
    val publishEntryType: Int? = null,
    /**
     * 是否首次发行（0-否，1-是）
     */
    val firstPublishFlag: Int? = null
) {
    fun canApply(): Boolean {
        return canApplyFlag == CAN_APPLY
    }

    fun needLegalInfo(): Boolean {
        return needLegalInfoFlag == NEED_LEGAL_INFO
    }

    fun unavailable(): Boolean {
        return publishEntryType == ENTRY_UNAVAILABLE ||
                (publishEntryType == null && canApplyFlag == CANNOT_APPLY)
    }

    fun shouldOpenH5(): Boolean {
        return publishEntryType == ENTRY_H5_FIRST_PUBLISH ||
                firstPublishFlag == FIRST_PUBLISH ||
                needLegalInfoFlag == NEED_LEGAL_INFO
    }

    fun shouldNativePublish(): Boolean {
        return publishEntryType == ENTRY_NATIVE_PUBLISH ||
                (publishEntryType == null && canApplyFlag == CAN_APPLY && !shouldOpenH5())
    }

    fun hasValidEpisodeRange(): Boolean {
        val start = startEpisode ?: return false
        val end = endEpisode ?: return false
        return start > 0 && end >= start
    }

    fun episodeRangeText(): String {
        val start = startEpisode ?: return ""
        val end = endEpisode ?: return ""
        return if (start == end) start.toString() else "$start-$end"
    }

    companion object {
        const val CANNOT_APPLY = 0
        const val CAN_APPLY = 1
        const val NEED_LEGAL_INFO = 1
        const val ENTRY_UNAVAILABLE = 0
        const val ENTRY_H5_FIRST_PUBLISH = 1
        const val ENTRY_NATIVE_PUBLISH = 2
        const val FIRST_PUBLISH = 1
    }
}
