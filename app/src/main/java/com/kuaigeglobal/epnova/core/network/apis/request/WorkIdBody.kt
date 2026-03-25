package com.kuaigeglobal.epnova.core.network.apis.request

data class WorkIdBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val workId: String?
) : UserTokenBody()

data class WorkBody(
    /**
     * 作品Id，接受Long和String类型
     */
    val workId: String?,
    /**
     * 剧集Id，接受Long和String类型
     */
    val storyId: String?
) : UserTokenBody()

data class ContinueGenerateBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val dramasSeriesId: String?,
    val inputContent: String?//输入内容
) : UserTokenBody()