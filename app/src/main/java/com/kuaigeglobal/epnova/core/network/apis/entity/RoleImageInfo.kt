package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "roleId": "角色主键id",
 *     "appearanceId": "当前资产形象ID（参考图id）",
 *     "roleName": "角色名称",
 *     "rolePrompt": "角色图片描述提示词",
 *     "imageUrl": "角色图片URL",
 *     "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]",
 *     "taskId": "任务ID (雪花算法)"
 *                 "sourceAppearanceId": "来源形象ID，1=原始生成，非1=指向父形象 appearance_id",
 *                 "acted": "是否已出演（该形象存在 episode 关联记录）"
 * }
 */
@Parcelize
data class RoleImageInfo(
    val assetImageId: String? = null,
    val assetId: String? = null,
    val acted: Boolean? = null,
    val appearanceId: String? = null,
    val genStatus: Int? = null,
    val imageUrl: String? = null,
    val referenceAssetImageId: String? = null,
    val referenceImageUrl: String? = null,
    val referenceImageInStatus: Int? = null,
    val currentSelected: Boolean? = null,
    val rolePrompt: String? = null,
    val sourceAppearanceId: String? = null,
    val taskId: String? = null
) : Parcelable
