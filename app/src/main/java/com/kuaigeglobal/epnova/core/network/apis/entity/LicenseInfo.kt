package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LicenseInfo(
    var commerceAccreditStatus: Int?,//商业授权状态 0=未授权 1=已授权
    var commerceAccreditTime: Long?
) : Parcelable {
    fun isAuthed(): Boolean {
        return  commerceAccreditStatus==1
    }
}