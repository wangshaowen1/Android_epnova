package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * 算力不足时的付费引导信息。
 */
data class PointInsufficientGuideData(
    /** 前端付费引导类型：0、1、2、3。 */
    val guideType: Int? = null,
)
