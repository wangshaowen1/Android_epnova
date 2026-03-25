package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 提交字幕配音
 * @param shotSegmentId 短剧分镜片段表id
 * @param shotSegmentImageId 短剧分镜镜片段图片id
 */
data class SegmentImageReplaceBody(
    val shotSegmentId: String?,
    val shotSegmentImageId: String?,
) : UserTokenBody()