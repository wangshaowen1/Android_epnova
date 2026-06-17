package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "ids": [
 *         "泛型Long"
 *     ],
 *     "id": "id",
 *     "operateType": "操作类型（4-合成后的重绘删除）合成完成后重绘删除时传4"
 * }
 */
data class IdsBody(
    val ids: List<String>?,
    val id: String?,
    val operateType: Int? = null
)


data class AssetUnlinkBody(
    val workId: String? = null,
    val storyId: String? = null,
    val assetId: String? = null,
)

/**
 * 当前集资产图片操作请求体
 */
data class AssetStoryImageBody(
    //作品 ID
    val workId: String? = null,
    //剧集 ID
    val storyId: String? = null,
    //资产 ID
    val assetId: String? = null,
    //资产图片 ID
    val assetImageId: String? = null,
)
