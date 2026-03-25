package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 重绘分镜（重新生成分镜的图片）
 * @param shotSegmentId 分镜id
 * @param shotImageDesc 分镜画面描述
 */
data class SegmentImageDescOptimizeBody(
    val shotSegmentId: String?,
    val shotImageDesc: String?,
) : UserTokenBody()