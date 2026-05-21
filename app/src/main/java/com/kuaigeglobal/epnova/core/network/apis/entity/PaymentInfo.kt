package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import com.kuaigeglobal.epnova.R
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "wayConfigCode": "支付方式配置编号（雪花算法ID）",
 *     "payWay": "支付方式（1-微信、2-支付宝、3-苹果内购）",
 *     "waySortNo": "展示排序值（从大到小，越大越靠前）",
 *     "payMerchantConfigId": "支付商配置表ID（苹果内购不需要设置）"
 * }
 */
@Parcelize
data class PaymentInfo(
    /**
     * 支付方式配置编号（雪花算法ID）
     */
    val wayConfigCode: String?,
    /**
     * 支付方式（1-微信、2-支付宝、3-苹果内购）
     */
    val payWay: Int?,
    /**
     * 展示排序值（从大到小，越大越靠前）
     */
    val waySortNo: Int?,
    /**
     * 支付商配置表ID（苹果内购不需要设置）
     */
    val payMerchantConfigId: String?,
    /**
     * 是否默认选中（0-非默认、1-默认）
     */
    val isDefault: Int?
) : Parcelable {
    val name
        get() = when (payWay) {
            1 -> "微信"
            2 -> "支付宝"
            else -> ""
        }
    val icon
        get() = when (payWay) {
            1 -> R.drawable.icon_payment_wx
            2 -> R.drawable.icon_payment_zfb
            else -> null
        }
    val paymentMethod
        get() = when (payWay) {
            else -> null
        }
}
