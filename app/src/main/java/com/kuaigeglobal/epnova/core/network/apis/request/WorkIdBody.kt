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

data class CurrentWorkIdBody(
    /**
     * 当前刚播放结束的作品 ID，用于排除自身并作为推荐上下文
     */
    val currentWorkId: String?
) : UserTokenBody()