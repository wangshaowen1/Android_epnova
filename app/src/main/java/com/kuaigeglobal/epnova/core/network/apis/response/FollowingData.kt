package com.kuaigeglobal.epnova.core.network.apis.response

data class FollowingData(

    val watchingStatus: Int?,//追剧状态 0-未追剧 1-已经追剧
    val watchingNumStr: String?,//追剧数量
    val watchingNum: Int?
)