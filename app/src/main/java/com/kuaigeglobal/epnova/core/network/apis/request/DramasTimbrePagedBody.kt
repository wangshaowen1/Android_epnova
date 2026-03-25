package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 提交字幕配音
 * @param dramasId 剧本id
 * @param timbreId 字幕音色id
 */
class DramasTimbrePagedBody(
    val dramasId: String?,
    val timbreId: String?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)