package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PointConfigInfo(
    val pointConfigId: String?,
    val pointConfigCode: String?,
    val googleProductId: String?,
    val pointName: String?,
    var pointPrice: String?,
    val pointNum: Long?,
    val givePointNum: Long?,
    val selectedStatus: Int?,
    val pointMarketingTime: Long?,
    val pointMarketingTag: String?,
) : Parcelable {

    fun isDiscountValid(): Boolean {
        return pointMarketingTime != null && pointMarketingTime > System.currentTimeMillis()
    }

    fun getCountDownText(): String {
        if (pointMarketingTime == null) return ""
        val time = pointMarketingTime - System.currentTimeMillis()
        if (time <= 0) return ""
        val hour = time / 1000 / 60 / 60
        val minute = time / 1000 / 60 % 60
        val second = time / 1000 % 60
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }
}
