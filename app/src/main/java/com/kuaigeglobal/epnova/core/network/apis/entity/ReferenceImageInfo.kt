package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 资产参考生图图片
 */
data class ReferenceImageInfo(
    //资产图片 ID
    val assetImageId: String? = null,
    //作品级资产 ID
    val assetId: String? = null,
    //图片 URL
    val imageUrl: String? = null,
    //参考资产图片 ID
    val referenceAssetImageId: String? = null,
    //参考图片 URL
    val referenceImageUrl: String? = null,
    //参考图片入镜状态(0-不入镜 1-入镜)
    val referenceImageInStatus: Int? = null
) {
    val appearanceId: String?
        get() = assetImageId
}
