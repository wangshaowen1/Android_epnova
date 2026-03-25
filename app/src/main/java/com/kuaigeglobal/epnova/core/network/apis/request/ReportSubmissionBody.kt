package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:提交举报
 * @Data: 2026/2/2/周一  16:07
 * @Version: 1.0
 * *****************************************
 */
data class ReportSubmissionBody (
    //被举报的作品用户 ID
    val userId: String?=null,
    //被举报的作品 ID
    val homeId: String?=null,
    //举报描述（必填，10-500字）
    val description: String?=null,
    //举报人联系方式（微信、qq、电话、邮箱）
    val contactInformation: String?=null,
)