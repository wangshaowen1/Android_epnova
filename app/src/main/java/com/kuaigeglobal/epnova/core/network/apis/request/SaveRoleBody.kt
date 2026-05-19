package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "roleId": "角色主键id（为空 = 新建角色）",
 *     "appearanceId": "当前资产形象ID（为空 = 新建形象）",
 *     "workId": "作品id （新建角色必填）",
 *     "storyId": "剧集id",
 *     "roleName": "角色名称",
 *     "rolePrompt": "角色形象提示词（形象表字段）"
 * }
 */
data class SaveRoleBody(
    /**
     * 角色主键id（为空 = 新建角色）
     */
    val roleId: String?,
    /**
     * 当前资产形象ID（为空 = 新建形象）
     */
    val appearanceId: String?,
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    /**
     * 角色名称
     */
    val roleName: String?,
    /**
     * 角色形象提示词（形象表字段）
     */
    val rolePrompt: String?,
    val source: Int? = 2,//创作步骤标识，用于区分调用来源（如：1=资产管理 2=创作流程 等）
    val parentAppearanceId: String? = null,//基于某个形象重新生成时传入，为空则视为原始生成
    val imageUrl: String? = null,//图片地址
)