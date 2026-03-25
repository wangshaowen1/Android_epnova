package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:分页查询我的剧集列表
 * @Data: 2025/11/5/周三  17:27
 * @Version: 1.0
 * *****************************************
 */
data class MyTvSeriesBody(
    //[枚举]上架状态 [0=未上架 1=已上架]
    val launchStatus: Int,
    //当前页
    val pageNum: Int,
    //每页的数量
    val pageSize: Int,
): UserTokenBody ()