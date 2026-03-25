package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 下载短剧视频
 * @param dramasId 短剧id，接受Long和String类型
 * @param videoUrl 视频URL
 * @param videoDuration 时长(单位：秒)
 */
data class DownloadReportBody(
    val dramasId: String?,
    val videoUrl: String?,
    val videoDuration: Int
) : UserTokenBody()