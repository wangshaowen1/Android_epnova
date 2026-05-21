package com.kuaigeglobal.epnova.core.network.apis.response

import android.os.Parcelable
import com.kuaigeglobal.epnova.core.network.apis.entity.PaymentInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.PointConfigInfo
import kotlinx.parcelize.Parcelize

/**
 * @Introduce: 算力首充礼包弹窗数据
 */
@Parcelize
data class FirstRechargeCreditsDialogData(
    val extraGiftText: String? = null,
    val giftTag: String? = null,
    val imageUrl: String? = null,
    val needShow: Boolean? = null,
    val popupInvalidTime: Long? = null,
    val pricePointText: String? = null,
    val pointConfigVO: PointConfigInfo? = null,
    val payWayConfig: PaymentInfo? = null,
    val basicPoint: String? = null,
    val basicPrice: String? = null,
    val giveTotalPoint: String? = null,
    val givePoint: String? = null,
    val givePercent: String? = null,
) : Parcelable {
    fun isNeedShow(): Boolean {
        return needShow == true
    }
}
