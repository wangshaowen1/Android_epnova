package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 提交字幕配音
 * @param shotSegmentId 短剧分镜片段表id
 */
data class ShotSegmentIdBody(
    val shotSegmentId: String?,
) : UserTokenBody()