package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Locale

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
    val isDefault: Int?,
    val pointMarketingTime: Long?,
    val pointMarketingTag: String?,
) : Parcelable {

    fun isDiscountValid(): Boolean {
        return (pointMarketingTime ?: 0L) > 0
    }

    fun getCountDownText(remainingTime: Long = pointMarketingTime ?: 0L): String {
        val time = remainingTime.coerceAtLeast(0L)
        if (time <= 0) return "00:00:00"
        val totalSeconds = time / 1000
        val hour = totalSeconds / 60 / 60
        val minute = totalSeconds / 60 % 60
        val second = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second)
    }
}
