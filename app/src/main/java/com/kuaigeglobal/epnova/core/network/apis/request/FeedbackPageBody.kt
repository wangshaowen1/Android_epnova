package com.kuaigeglobal.epnova.core.network.apis.request

/** 反馈历史分页请求，withNoticeSnapshot 默认传 0。 */
class FeedbackPageBody(
    pageNum: Int,
    pageSize: Int,
    val withNoticeSnapshot: Int = 0
) : PagedBody(pageNum, pageSize)
