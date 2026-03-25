package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * @param detailType 明细类型（0-全部、1-收入明细、2-消耗明细）
 */
class DetailTypeBody(
    /**
     * 明细类型（0-全部、1-收入明细、2-消耗明细）
     */
    val detailType: Int?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)
