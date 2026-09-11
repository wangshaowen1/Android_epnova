package com.kuaigeglobal.epnova.core.network.apis.request

/** 消息分页请求。 */
class MessagePageBody(
    pageNum: Int,
    pageSize: Int,
    val withReadSnapshot: Int = 1
) : PagedBody(pageNum, pageSize)
