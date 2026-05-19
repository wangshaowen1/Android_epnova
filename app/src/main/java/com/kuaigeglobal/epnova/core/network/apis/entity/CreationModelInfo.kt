package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import com.kuaigeglobal.epnova.common.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreationModelInfo(
    val configCode: String? = null,
    val configName: String? = null,
    val configValue: Int? = null,//算力价格
    val enabled: Int? = null,//是否启用(0-否,1-是)
    val isSelected: Int? = null,//当前用户是否已选中该模型(0-否,1-是)未登录时全部为0；登录后取 b_user_storyboard_preference，无记录时默认 storyboard_narration
    val remark: String? = null
) : Parcelable {

    /**
     * 是否是解说漫
     */
    fun isNarration(): Boolean {
        return configCode == Constants.Model.STORYBOARD_NARRATION
    }

    fun isValid(): Boolean {
        return enabled == 1
    }

    fun isDefault(): Boolean {
        return isSelected == 1
    }
}
