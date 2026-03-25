package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 追剧或取消追剧
 */
data class ShortLikeBody(
    /**
     * 短剧id，接受Long和String类型
     */
    val dramasId: String?,

) : UserTokenBody()