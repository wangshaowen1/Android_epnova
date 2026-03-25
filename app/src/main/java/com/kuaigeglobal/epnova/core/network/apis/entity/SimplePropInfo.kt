package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * {
 *     "propId": "道具主键 id",
 *     "appearanceId": "当前资产形象ID（参考图id）",
 *     "propName": "道具名称",
 *     "propPrompt": "道具图片描述提示词",
 *     "imageUrl": "道具图片URL",
 *     "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]",
 *     "taskId": "任务ID (雪花算法)"
 * }
 */
data class SimplePropInfo(
    /**
     * 道具主键 id
     */
    val propId: String? = null,
    /**
     * 当前资产形象ID（参考图id）
     */
    val appearanceId: String? = null,
    /**
     * 道具名称
     */
    val propName: String? = null,
    /**
     * 道具图片描述提示词
     */
    val propPrompt: String? = null,
    /**
     * 道具图片URL
     */
    var imageUrl: String? = null,
    /**
     * [枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]
     */
    var genStatus: Int? = null,
    /**
     * 任务ID (雪花算法)
     */
    var taskId: String? = null,
    /**
     * 是否有历史道具形象（同一 propId 是否存在其他版本）
     */
    val hasHistory: Boolean? = null
) : SimpleAsset {
    override val status: Int?
        get() = genStatus
    override val assetDesc: String?
        get() = propPrompt
    override val assetName: String?
        get() = propName
    override val assetImage: String?
        get() = imageUrl
    override val showReference: Boolean
        get() = hasHistory == true || !imageUrl.isNullOrEmpty()
}