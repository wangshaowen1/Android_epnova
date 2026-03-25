package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * @param tabId id
 * @param tabType Tab类型：1-推荐Tab，2-分类Tab
 */
class HitRecommendPagedBody(
    val tabId: String?,
    val tabType: Int?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)