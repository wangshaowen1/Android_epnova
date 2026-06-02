package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * {
 *     "officialConsultationId": "资讯雪花ID",
 *     "consultTitle": "资讯标题",
 *     "coverImageUrl": "封面图片URL",
 *     "jumpUri": "跳转URI"
 * }
 */
data class OfficialConsultationInfo(
    val officialConsultationId: String? = null,
    val consultTitle: String? = null,
    val coverImageUrl: String? = null,
    val jumpUri: String? = null
)
