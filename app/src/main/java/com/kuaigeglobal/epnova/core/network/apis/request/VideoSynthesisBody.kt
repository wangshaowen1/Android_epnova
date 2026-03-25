package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "workId": "作品ID",
 *     "storyId": "剧集ID",
 *     "videoUrl": "合成后的视频 URL",
 *     "duration": "视频时长（秒），可选 20260311传秒"
 * }
 */
data class VideoSynthesisBody(
    val workId: String?,
    val storyId: String?,
    val videoUrl: String?,
    val duration: Long?
) : UserTokenBody()
