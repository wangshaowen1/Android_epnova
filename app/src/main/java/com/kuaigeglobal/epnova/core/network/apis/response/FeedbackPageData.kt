package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.PagedList
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackDetail

/** 反馈历史分页数据及本次通知快照 token。 */
data class FeedbackPageData(
    val noticeSnapshotToken: String? = null
) : PagedList<FeedbackDetail>()
