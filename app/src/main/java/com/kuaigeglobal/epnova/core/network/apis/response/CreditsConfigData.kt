package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.core.network.apis.ListData
import com.kuaigeglobal.epnova.core.network.apis.entity.PointConfigInfo

data class PointConfigData(
    val pointBalanceNum: Int?,
) : ListData<PointConfigInfo>()
