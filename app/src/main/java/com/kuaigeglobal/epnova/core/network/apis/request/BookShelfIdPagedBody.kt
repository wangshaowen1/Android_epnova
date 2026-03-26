package com.kuaigeglobal.epnova.core.network.apis.request

class BookShelfIdPagedBody(
    val bookshelfId: String?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)