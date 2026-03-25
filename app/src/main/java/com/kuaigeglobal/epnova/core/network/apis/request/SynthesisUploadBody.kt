package com.kuaigeglobal.epnova.core.network.apis.request

data class SynthesisUploadBody(
    /**
     * 短剧id
     */
    val dramasId: String?,
    /**
     * 视频URL
     */
    val videoUrl: String?,
    /**
     * 时长(单位：秒)
     */
    val videoDuration: Int?,
) : UserTokenBody()