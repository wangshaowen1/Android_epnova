package com.kuaigeglobal.epnova.core.network.apis.response

data class ShortLikeData(

    val likeStatus: Int?,//点赞状态，0未点赞，1已经点赞
    val likeNumStr: String?,//点赞数量
    val likeNum: Int?
)