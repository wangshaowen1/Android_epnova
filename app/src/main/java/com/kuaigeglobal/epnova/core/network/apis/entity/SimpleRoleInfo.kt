package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * {
 *     "roleId": "角色主键id",
 *     "appearanceId": "当前资产形象ID（参考图id）",
 *     "roleName": "角色名称",
 *     "rolePrompt": "角色图片描述提示词",
 *     "imageUrl": "角色图片URL",
 *     "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]",
 *     "taskId": "任务ID (雪花算法)"
 * }
 */
data class SimpleRoleInfo(
    /**
     * 角色主键id
     */
    val roleId: String? = null,
    /**
     * 当前资产形象ID（参考图id）
     */
    val appearanceId: String? = null,
    /**
     * 角色名称
     */
    val roleName: String? = null,
    /**
     * 角色图片描述提示词
     */
    val rolePrompt: String? = null,
    /**
     * 角色图片URL
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
     * 是否有历史角色形象（同一 roleId 是否存在其他版本）
     */
    val hasHistory: Boolean? = null
) : SimpleAsset {
    override val status: Int?
        get() = genStatus
    override val assetDesc: String?
        get() = rolePrompt
    override val assetName: String?
        get() = roleName
    override val assetImage: String?
        get() = imageUrl
    override val showReference: Boolean
        get() = hasHistory == true || !imageUrl.isNullOrEmpty()
}