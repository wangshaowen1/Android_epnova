package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:分页查询我的作品列表
 * @Data: 2026/2/4/周三  9:21
 * @Version: 1.0
 * *****************************************
 */
data class MyWorkBody(
    //作品ID
    val workId: String?,
    val pageNum: Int?,
    val pageSize: Int?,
) : UserTokenBody()