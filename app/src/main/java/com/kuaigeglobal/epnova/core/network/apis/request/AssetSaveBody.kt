package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 保存资产请求体
 */
data class AssetSaveBody(
    // 作品 ID
    val workId: String?,
    // 剧集 ID，创作流程中传入时会同步当前集选用记录
    val storyId: String?,
    // 作品级资产 ID，为空表示新增
    val assetId: String?,
    // 资产类型 [1=角色 2=场景 3=道具]
    val assetType: Int,
    // 资产名称
    val assetName: String?,
    // 资产描述
    val assetPrompt: String?
) : UserTokenBody()
