package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *         "needShow": "是否需要展示弹窗（0-不需要展示，1-需要展示）",
 *         "popupTitle": "弹窗标题",
 *         "popupImageUrl": "弹窗图片URL",
 *         "rewardLabel": "奖励数量（示例：50 ）",
 *         "finishFlag": "是否已完成（0-否，1-是）"
 *     }
 */
@Parcelize
data class LoginTaskData(
    val finishFlag: Int? = null,
    val needShow: Int? = null,
    val popupImageUrl: String? = null,
    val popupTitle: String? = null,
    val rewardLabel: String? = null
) : Parcelable