package com.kuaigeglobal.epnova.core.network.apis.request

/** 灵感案例、热门案例使用次数上报。 */
data class ReportCreationUsageBody(
    val reportId: String? = null,
    val reportType: Int? = null
)
