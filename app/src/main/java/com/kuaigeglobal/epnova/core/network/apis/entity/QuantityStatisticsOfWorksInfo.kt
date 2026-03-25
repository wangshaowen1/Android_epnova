package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:查询我的作品数量统计信息
 * @Data: 2025/11/5/周三  18:13
 * @Version: 1.0
 * *****************************************
 */
data class QuantityStatisticsOfWorksInfo(
    //剧集数量
    val dramasSeriesNum: Int = 0,
    //短剧数量
    val dramasScriptNum: Int = 0,
    //追剧数量
    val dramasSeriesWatchingNum: Int = 0,
    //喜欢的短剧数量
    val dramasScriptLikeNum: Int = 0,
)