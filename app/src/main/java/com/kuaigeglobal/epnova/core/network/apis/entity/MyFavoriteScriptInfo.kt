package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:分页查询我喜欢的剧本列表d
 * @Data: 2025/11/5/周三  18:01
 * @Version: 1.0
 * *****************************************
 */
data class MyFavoriteScriptInfo(
    //短剧id，
    val dramasId: String? = null,
    //短剧封面URL
    val dramasCoverUrl: String? = null,
    //喜欢数量
    val likeNum: Int? = null,
    //短剧剧集信息
    val dramasSeries: DramasSeries? = null,
) {
    data class DramasSeries(
        //短剧剧集表id
        val dramasSeriesId: Long? = null,
        //短剧剧集名称
        val seriesName: String? = null,
        //短剧作品所在剧集中的集数编号
        val workNo: Int? = null
    ){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DramasSeries

            if (dramasSeriesId != other.dramasSeriesId) return false
            if (workNo != other.workNo) return false
            if (seriesName != other.seriesName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = dramasSeriesId?.hashCode() ?: 0
            result = 31 * result + (workNo ?: 0)
            result = 31 * result + (seriesName?.hashCode() ?: 0)
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MyFavoriteScriptInfo

        if (likeNum != other.likeNum) return false
        if (dramasId != other.dramasId) return false
        if (dramasCoverUrl != other.dramasCoverUrl) return false
        if (dramasSeries != other.dramasSeries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = likeNum ?: 0
        result = 31 * result + (dramasId?.hashCode() ?: 0)
        result = 31 * result + (dramasCoverUrl?.hashCode() ?: 0)
        result = 31 * result + (dramasSeries?.hashCode() ?: 0)
        return result
    }
}