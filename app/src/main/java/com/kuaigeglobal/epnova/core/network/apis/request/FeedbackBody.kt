package com.kuaigeglobal.epnova.core.network.apis.request


data class FeedbackBody(
    //问题类型（1=产品建议，2=功能故障，3=其他问题）
    val problemType: Int?,
    //问题描述(最多500字)
    val problemDesc: String?,
    //图片url(最多4张，用英文逗号分割图片Url)
    var imagesUrl: String?,
    //联系方式(邮箱)
    val contactEmail: String?
) : UserTokenBody()