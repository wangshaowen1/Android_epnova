package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "assetId": "资产 ID （角色、道具、场景 id）根据业务类型判断",
 *     "appearanceId": "当前资产形象ID（参考图id）",
 *     "bizType": "业务类型枚举41=角色文生图片 42=角色参考生图 51=道具文生图片 52=道具参考生图  61=场景文生图片 62=场景参考生图",
 *     "prompt": "提示词",
 *     "referenceImageId": "参考图 id",
 *     "referenceImageInStatus": "参考图片入镜状态(0-不入镜，1-入镜)",
 *     "referenceImageUrl": "参考图片 URL",
 *     "storyId": "剧集 id",
 *     "workId": "作品 ID"
 * }
 */
data class GenerateAssetBody(
    /**
     * 资产 ID （角色、道具、场景 id）根据业务类型判断
     */
    val assetId: String? = null,
    /**
     * 当前资产形象ID（参考图id）
     */
    val appearanceId: String? = null,
    /**
     * 业务类型枚举41=角色文生图片 42=角色参考生图 51=道具文生图片 52=道具参考生图  61=场景文生图片 62=场景参考生图
     */
    val bizType: Int? = null,
    /**
     * 提示词
     */
    val prompt: String? = null,
    /**
     * 参考图 id
     */
    val referenceImageId: String? = null,
    /**
     * 参考图片入镜状态(0-不入镜，1-入镜)
     */
    val referenceImageInStatus: Int? = null,
    /**
     * 参考图片 URL
     */
    val referenceImageUrl: String? = null,
    /**
     * 剧集 id
     */
    val storyId: String? = null,
    /**
     * 作品 ID
     */
    val workId: String? = null,
)