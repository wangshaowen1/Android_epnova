package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 资产图片生成结果
 */
data class AssetGenerateInfo(
    // 作品级资产 ID
    val assetId: String? = null,
    // 本次生成创建的资产图片 ID
    val assetImageId: String? = null,
    // 本次生成任务 ID
    val taskId: String? = null,
    // 生成状态 [1=生成中 2=成功 3=失败 4=算力不足]
    val genStatus: Int? = null
)
