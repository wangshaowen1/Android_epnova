package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import com.kuaigeglobal.epnova.core.Config
import kotlinx.parcelize.Parcelize

/**
 *  {
 *         "popupList": [
 *             {
 *                 "id": "弹窗ID",
 *                 "popupLocationCode": "弹窗位置编号（1-大厅主页、2-个人主页、3-我的作品、）",
 *                 "businessType": "业务类型（1-课程）",
 *                 "showFrequency": "展示频次（1-首次启动弹出、2-每天弹出、3-常驻展示）",
 *                 "maxShowCountPerDay": "每日最大展示次数（show_frequency=2时有效）",
 *                 "jumpType": "跳转类型（1-站内H5、2-站外H5）",
 *                 "jumpUri": "跳转URL",
 *                 "popupStatus": "弹窗状态（0-禁用、1-启用）"
 *             }
 *         ],
 *         "floatWindowList": [
 *             {
 *                 "id": "浮窗ID",
 *                 "floatLocationCode": "浮窗位置编号（1-大厅主页、2-个人主页、3-我的作品、）",
 *                 "businessType": "业务类型（1-课程）",
 *                 "showFrequency": "展示频次（1-首次启动展示、2-每天展示、3-常驻展示）",
 *                 "maxShowCountPerDay": "每日最大展示次数（show_frequency=2时有效）",
 *                 "jumpType": "跳转类型（1-站内H5、2-站外H5）",
 *                 "jumpUrl": "跳转URL",
 *                 "floatWindowStatus": "弹窗状态（0-禁用、1-启用）"
 *             }
 *         ]
 *     }
 */
data class ActivityConfigData(
    val floatWindowList: List<FloatWindowInfo>? = null,
    val popupList: List<PopupInfo>? = null
) {
    companion object {
        const val TAB_HOME = 1
        const val TAB_MINE = 2
        const val TAB_MY_WORKS = 3
    }

    fun getFloatWindowInfo(tab: Int): FloatWindowInfo? {
        return floatWindowList?.firstOrNull {
            it.floatLocationCode == tab
        }
    }

    fun getPopupInfo(tab: Int): PopupInfo? {
        return popupList?.firstOrNull {
            it.popupLocationCode == tab
        }
    }
}

data class FloatWindowInfo(
    val businessType: Int? = null,
    val floatLocationCode: Int? = null,
    val floatWindowStatus: Int? = null,
    val id: String? = null,
    val jumpType: Int? = null,
    val jumpUrl: String? = null,
    val imageUrl: String? = null,
    val maxShowCountPerDay: Int? = null,
    val showFrequency: Int? = null
) {
    fun isShow(): Boolean {
        if (floatWindowStatus != 1 || id.isNullOrEmpty()) return false

        val config = Config.get()
        val lastShowTime = config.activityPopupShowTime(id)
        val showCountToday = config.activityPopupShowCount(id) // 此时该方法已自带跨天归零逻辑

        return when (showFrequency) {
            1 -> lastShowTime == 0L
            2 -> showCountToday < (maxShowCountPerDay ?: 0) // 直接判断即可
            3 -> true
            else -> false
        }
    }
}

@Parcelize
data class PopupInfo(
    val businessType: Int? = null,
    val id: String? = null,
    val jumpType: Int? = null,
    val imageUrl: String? = null,
    val jumpUri: String? = null,
    val maxShowCountPerDay: Int? = null,
    val popupLocationCode: Int? = null,
    val popupStatus: Int? = null,
    val showFrequency: Int? = null
) : Parcelable {
    fun isShow(): Boolean {
        if (popupStatus != 1 || id.isNullOrEmpty()) return false

        val config = Config.get()
        val lastShowTime = config.activityPopupShowTime(id)
        val showCountToday = config.activityPopupShowCount(id) // 此时该方法已自带跨天归零逻辑

        return when (showFrequency) {
            1 -> lastShowTime == 0L
            2 -> showCountToday < (maxShowCountPerDay ?: 0) // 直接判断即可
            3 -> true
            else -> false
        }
    }
}