package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "sceneId": "场景主键id",
 *     "appearanceId": "当前资产形象ID 查询单个时入参",
 *     "workId": "作品id",
 *     "storyId": "剧集id",
 *     "creationSource": "[枚举]创建来源 [1=AI生成 2=用户创建]",
 *     "sceneName": "场景名称",
 *     "scenePrompt": "场景图片描述提示词"
 * }
 */
data class SaveSceneBody(
    /**
     * 场景主键id
     */
    val sceneId: String?,
    /**
     * 当前资产形象ID 查询单个时入参
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
     * 场景名称
     */
    val sceneName: String?,
    /**
     * 场景图片描述提示词
     */
    val scenePrompt: String?,
)