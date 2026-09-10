package com.kuaigeglobal.epnova.core.network.apis.request

/** 查询问题反馈详情的请求体；用户身份由公共网络层写入 userToken 请求头。 */
data class FeedbackDetailBody(
    /** 反馈雪花业务 ID，使用无前导零的正十进制字符串传输。 */
    val feedbackId: String,
)
