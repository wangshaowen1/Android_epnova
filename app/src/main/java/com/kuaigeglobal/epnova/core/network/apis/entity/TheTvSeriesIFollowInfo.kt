package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:分页查询我追的剧集列表
 * @Data: 2025/11/5/周三  17:51
 * @Version: 1.0
 * *****************************************
 */
data class TheTvSeriesIFollowInfo(
    //短剧剧集表id
    val dramasSeriesId: String?=null,
    //短剧剧集名称
    val seriesName: String?=null,
    //剧集封面的URL
    val seriesCoverUrl: String?=null,
    //剧集当前总集数
    val totalWorkNum: String?=null,
    //播放次数
    val playNum: Int?=null,
    //当前播放的短剧id
    val currentDramasId: String?=null
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TheTvSeriesIFollowInfo

        if (playNum != other.playNum) return false
        if (dramasSeriesId != other.dramasSeriesId) return false
        if (seriesName != other.seriesName) return false
        if (seriesCoverUrl != other.seriesCoverUrl) return false
        if (totalWorkNum != other.totalWorkNum) return false
        if (currentDramasId != other.currentDramasId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playNum ?: 0
        result = 31 * result + (dramasSeriesId?.hashCode() ?: 0)
        result = 31 * result + (seriesName?.hashCode() ?: 0)
        result = 31 * result + (seriesCoverUrl?.hashCode() ?: 0)
        result = 31 * result + (totalWorkNum?.hashCode() ?: 0)
        result = 31 * result + (currentDramasId?.hashCode() ?: 0)
        return result
    }
}