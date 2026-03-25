package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 修改剧集价格
 */
data class SeriesSetPriceBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val dramasSeriesId: String?,
    /**
     * 每集解锁价格，单位：厘
     */
    val unlockPrice: Long?,
    /**
     * 从第几集开始收费,最小为4
     */
    val startChargeWorkNo: Int?,
    /**
     * 免费状态 0-收费 1-免费
     */
    val freeStatus: Int?
) : UserTokenBody()