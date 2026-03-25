package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "workId": "作品ID",
 *     "workTitle": "作品名称",
 *     "workDesc": "作品简介",
 *     "workCoverUrl": "作品封面图URL（AI生成或默认图）"
 * }
 */
data class WorkInfoUpdateBody(
    val workId: String? = null,
    val workCoverUrl: String? = null,
    val workTitle: String? = null,
    val workDesc: String? = null,
) : UserTokenBody()