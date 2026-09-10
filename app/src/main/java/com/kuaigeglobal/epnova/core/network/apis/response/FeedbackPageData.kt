package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.PagedList
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackPageInfo

/** 反馈历史分页数据及本次通知快照 token。 */
data class FeedbackPageData(
    /** 覆盖当前用户全部分页提醒版本的快照令牌；未请求快照时为 null。 */
    val noticeSnapshotToken: String? = null
) : PagedList<FeedbackPageInfo>()
