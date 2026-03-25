package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:批量删除我的作品
 * @Data: 2026/2/4/周三  17:48
 * @Version: 1.0
 * *****************************************
 */
data class DeleteWorkBody(
    //批量的id
    val ids: List<String>?=null,
    //id
    val id: String?=null,
): UserTokenBody()