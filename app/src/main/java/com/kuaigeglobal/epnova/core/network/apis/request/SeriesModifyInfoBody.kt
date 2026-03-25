package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 修改剧集信息
 */
data class SeriesModifyInfoBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val dramasSeriesId: String?,
    /**
     * 短剧剧集封面
     */
    val seriesCover: String?,
    /**
     * 短剧剧集名称
     */
    val seriesName: String?,
    /**
     * 短剧剧集简介
     */
    val seriesIntro: String?
) : UserTokenBody()