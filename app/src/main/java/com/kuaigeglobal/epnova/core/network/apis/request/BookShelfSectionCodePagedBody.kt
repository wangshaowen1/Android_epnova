package com.kuaigeglobal.epnova.core.network.apis.request

class BookShelfSectionCodePagedBody(
    val bookshelfSectionCode: String,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)
