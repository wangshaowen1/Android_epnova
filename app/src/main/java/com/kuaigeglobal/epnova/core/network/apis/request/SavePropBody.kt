package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "propId": "道具主键 id",
 *     "appearanceId": "当前资产形象ID 查询单个时入参",
 *     "workId": "作品id",
 *     "storyId": "剧集id",
 *     "propName": "道具名称",
 *     "propPrompt": "道具图片描述提示词"
 * }
 */
data class SavePropBody(
    /**
     * 道具主键 id
     */
    val propId: String?,
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
     * 道具名称
     */
    val propName: String?,
    /**
     * 道具图片描述提示词
     */
    val propPrompt: String?,
)