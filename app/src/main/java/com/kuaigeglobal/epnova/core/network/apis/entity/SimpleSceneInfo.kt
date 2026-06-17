package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "sceneId": "场景主键id",
 *     "appearanceId": "当前资产形象ID（参考图id）",
 *     "sceneName": "场景名称",
 *     "scenePrompt": "场景图片描述提示词",
 *     "imageUrl": "场景图片URL",
 *     "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]",
 *     "taskId": "任务ID (雪花算法)"
 * }
 */
@Parcelize
data class SimpleSceneInfo(
    /**
     * 场景主键id
     */
    val sceneId: String? = null,
    /**
     * 当前资产形象ID（参考图id）
     */
    val appearanceId: String? = null,
    /**
     * 当前集选中的资产图片ID
     */
    override val assetImageId: String? = null,
    /**
     * 场景名称
     */
    val sceneName: String? = null,
    /**
     * 场景图片描述提示词
     */
    val scenePrompt: String? = null,
    /**
     * 场景图片URL
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
     * 是否有历史场景形象（同一 sceneId 是否存在其他版本）
     */
    val hasHistory: Boolean? = null
) : SimpleAsset, Parcelable {
    override val assetId: String?
        get() = sceneId
    override val assetAppearanceId: String?
        get() = appearanceId
    override val status: Int?
        get() = genStatus
    override val assetDesc: String?
        get() = scenePrompt
    override val assetName: String?
        get() = sceneName
    override val assetImage: String?
        get() = imageUrl
    override val showReference: Boolean
        get() = hasHistory == true || !imageUrl.isNullOrEmpty()
    override val assetTaskId: String?
        get() = taskId

    override fun setAssetImage(imageUrl: String?) {
        this.imageUrl = imageUrl
    }
}
