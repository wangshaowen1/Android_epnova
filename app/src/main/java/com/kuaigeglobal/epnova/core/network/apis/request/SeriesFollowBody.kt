package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 追剧或取消追剧
 */
data class SeriesFollowBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val dramasSeriesId: String?,
    /**
     * 追剧状态 0-取消追剧 1-追剧
     */
    val followStatus: Int
) : UserTokenBody()