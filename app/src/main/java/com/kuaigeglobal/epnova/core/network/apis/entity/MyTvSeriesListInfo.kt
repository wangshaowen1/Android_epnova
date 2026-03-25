package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:分页查询我的剧集列表
 * @Data: 2025/11/5/周三  17:30
 * @Version: 1.0
 * *****************************************
 */
data class MyTvSeriesListInfo(
    //短剧剧集表id
    val dramasSeriesId: String? = null,
    //短剧剧集名称
    val seriesName: String? = null,
    //剧集封面的URL
    val seriesCoverUrl: String? = null,
    val seriesIntro: String? = null,//短剧剧集简介
    val styleName: String? = null,//风格名称
    //剧集当前总集数
    val totalWorkNum: String? = null,
    //播放次数
    val playNum: Int? = null,
    //追剧数量
    val watchingNum: Int? = null,
    //[枚举]上架状态 [0=未上架 1=已上架]
    val launchStatus: Int? = null,
    //上架时间
    val launchTime: Long? = null,
    //创建时间
    val createTime: Long? = null,
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MyTvSeriesListInfo

        if (launchStatus != other.launchStatus) return false
        if (dramasSeriesId != other.dramasSeriesId) return false
        if (seriesName != other.seriesName) return false
        if (seriesCoverUrl != other.seriesCoverUrl) return false
        if (totalWorkNum != other.totalWorkNum) return false
        if (playNum != other.playNum) return false
        if (watchingNum != other.watchingNum) return false
        if (launchTime != other.launchTime) return false
        if (createTime != other.createTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = launchStatus ?: 0
        result = 31 * result + (dramasSeriesId?.hashCode() ?: 0)
        result = 31 * result + (seriesName?.hashCode() ?: 0)
        result = 31 * result + (seriesCoverUrl?.hashCode() ?: 0)
        result = 31 * result + (totalWorkNum?.hashCode() ?: 0)
        result = 31 * result + (playNum?.hashCode() ?: 0)
        result = 31 * result + (watchingNum?.hashCode() ?: 0)
        result = 31 * result + (launchTime?.hashCode() ?: 0)
        result = 31 * result + (createTime?.hashCode() ?: 0)
        return result
    }
}