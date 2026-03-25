package com.kuaigeglobal.epnova.core.network.apis.request

data class GenerateSubtitleBody(
    /**
     * 分镜ID
     */
    val storyboardId: String?,
    /**
     * 视频URL
     */
    val videoUrl: String?,
    /**
     * 用户ID
     */
    val userId: String?,
    /**
     * 作品ID
     */
    val workId: String?,
    /**
     * 剧集ID
     */
    val storyId: String?,
) : UserTokenBody()