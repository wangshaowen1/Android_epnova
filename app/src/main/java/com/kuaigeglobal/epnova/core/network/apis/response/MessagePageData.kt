package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.PagedList
import com.kuaigeglobal.epnova.core.network.apis.entity.MessagePageInfo

/** 消息分页数据及本次已读快照 token。 */
data class MessagePageData(
    val readSnapshotToken: String? = null
) : PagedList<MessagePageInfo>()
