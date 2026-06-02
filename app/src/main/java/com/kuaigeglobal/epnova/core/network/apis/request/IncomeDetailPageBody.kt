package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * @param month 收益年月，格式 yyyy-MM；为空时默认查询当前结算时区下的当前月份。
 */
class IncomeDetailPageBody(
    val month: String?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)
