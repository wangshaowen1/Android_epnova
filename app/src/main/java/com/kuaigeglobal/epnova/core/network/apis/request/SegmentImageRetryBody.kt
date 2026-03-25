package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 生成剧本图片(失败重试)
 * @param dramasId 短剧id，接受Long和String类型
 * @param shotSegmentId 短剧分镜片段id
 */
data class SegmentImageRetryBody(
    val dramasId: String?,
    val shotSegmentId: String?,
) : UserTokenBody()