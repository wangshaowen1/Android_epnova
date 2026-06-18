package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 生成单个资产图片请求体
 */
data class AssetGenerateBody(
    // 作品 ID
    val workId: String?,
    // 剧集 ID
    val storyId: String?,
    // 作品级资产 ID
    val assetId: String?,
    // 本次生成使用的资产描述
    val prompt: String?,
    // 参考生图图片 ID
    val referenceAssetImageId: String? = null,
    // 参考图片入镜状态 [0=不入镜 1=入镜]
    val referenceImageInStatus: Int? = null
) : UserTokenBody()
