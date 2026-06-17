package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "workId": "作品 ID",
 *     "storyId": "剧集 id",
 *     "assetId": "资产 ID"
 * }
 */
data class ReferenceImageBody(
    //作品 ID
    val workId: String? = null,
    //剧集 id
    val storyId: String? = null,
    //资产 ID
    val assetId: String? = null
) : UserTokenBody()
