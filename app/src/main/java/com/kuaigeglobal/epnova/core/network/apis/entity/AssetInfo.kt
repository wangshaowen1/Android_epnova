package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 当前集资产项
 */
data class AssetInfo(
    // 当前集资产使用记录 ID
    val usageId: String? = null,
    // 作品级资产 ID
    val assetId: String? = null,
    // 资产类型 [1=角色 2=场景 3=道具]
    val assetType: Int? = null,
    // 资产名称
    val assetName: String? = null,
    // 资产描述/生图提示词
    val assetPrompt: String? = null,
    // 角色性别
    val gender: Int? = null,
    // 角色年龄段
    val ageGroup: Int? = null,
    // 是否主角 [0=否 1=是]
    val protagonistFlag: Int? = null,
    // 角色配音 ID
    val voiceId: String? = null,
    // 当前集资产展示排序
    val sortNo: Int? = null,
    // 当前集选中的资产图片 ID
    val assetImageId: String? = null,
    // 当前集选中的资产图片 URL
    val imageUrl: String? = null,
    // 当前选中图片生成状态 [0=待生成 1=生成中 2=成功 3=失败 4=算力不足]
    val genStatus: Int? = null,
    // 当前选中图片对应的模型任务 ID
    val taskId: String? = null,
    // 当前集是否已选图 [0=未选 1=已选]
    val selectedStatus: Int? = null,
    // 当前集是否最终出演 [0=未出演 1=已出演]
    val actedStatus: Int? = null,
    // 复用图片来源剧集 ID
    val sourceStoryId: String? = null,
    // 复用图片来源使用记录 ID
    val sourceUsageId: String? = null,
    // 是否存在历史图片
    val hasHistory: Boolean? = null
)
