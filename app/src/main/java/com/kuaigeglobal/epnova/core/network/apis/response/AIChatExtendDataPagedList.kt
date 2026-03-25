package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.PagedList

data class AIChatExtendDataPagedList<T>(
    val extendData: AIChatExtendData?,
) : PagedList<T>()