package com.kuaigeglobal.epnova.core.network.apis.request

class WorkIdPagedBody(
    /**
     * 作品Id，接受Long和String类型
     */
    val workId: String?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)