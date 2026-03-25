package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 重绘分镜（重新生成分镜的图片）
 * @param shotSegmentId 短剧分镜片段表id
 * @param shotImageDesc 分镜画面描述
 */
data class SegmentImageRegenerateBody(
    val shotSegmentId: String?,
    val shotImageDesc: String?,
) : UserTokenBody()