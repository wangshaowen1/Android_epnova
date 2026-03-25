package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable

@kotlinx.parcelize.Parcelize
data class AgeGroupData(
    val ageList: List<AgeGroupParamInfo>? = null
) : Parcelable {
    fun getDefaultAge(): AgeGroupParamInfo? {
        return ageList?.firstOrNull { it.selectedState == 1 }
    }
}

/**
 * {
 *             "ageConfigId": "年龄配置id",
 *             "ageConfigName": "年龄配置名称",
 *             "selectedState": "是否选中:0-未选中,1-选中"
 *         }
 */
@kotlinx.parcelize.Parcelize
data class AgeGroupParamInfo(
    val ageConfigId: String? = null,
    val ageConfigName: String? = null,
    val selectedState: Int? = null
) : Parcelable