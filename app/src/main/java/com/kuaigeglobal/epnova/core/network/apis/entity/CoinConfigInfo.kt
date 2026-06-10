package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "pointConfigId": "算力信息配置ID",
 *     "pointConfigCode": "算力信息配置编码下单时要提交的商品编号",
 *     "pointName": "算力名称",
 *     "pointPrice": "实际支付价格（单位：元）",
 *     "pointNum": "算力数量",
 *     "isDefault": "是否默认选中（0-非默认、1-默认）"
 * }
 */
@Parcelize
data class CoinConfigInfo(
    val coinsType: Int?,//金币套餐类型 1-常规购买金币套餐、2-新人礼包金币套餐
    val pointConfigId: String?,
    val googleProductId: String?,//Google商品ID
    val coinsConfigCode: String?,
    val coinsName: String?,
    var coinsPrice: String?,//实际支付价格（单位：元）
    val coinsNum: Long?,
    val giveCoinsNum: Long?,//赠送金币数量
    val giveValidDays: Int?,//赠送金币有效期（单位：天）
    /**
     * 是否默认选中（0-非默认、1-默认）
     */
    val selectedStatus: Int?,//是否默认选中（0-非默认、1-默认），与库表对应；列表内至多一条为 1。
    val coinsMarketingTime: Long?,//金币营销倒计时截止时间.空则代表不需要受限制精确到毫秒
    val coinsMarketingText: String?,//金币营销倒计时文本
    val coinsMarketingTag: String?,//金币营销角标文案（如：赠30%）
) : Parcelable {

    fun shouldShowGiftInfo(): Boolean {
        return coinsType == 2
    }

    fun isDiscountValid(): Boolean {
        return coinsMarketingTime != null && coinsMarketingTime > System.currentTimeMillis()
    }

    fun getCountDownText(): String {
        if (coinsMarketingTime == null) {
            return "00:00:00"
        }
        val time = coinsMarketingTime
        if (time <= 0) {
            return "00:00:00"
        }
        val hour = time / 1000 / 60 / 60
        val minute = time / 1000 / 60 % 60
        val second = time / 1000 % 60

        // 将 tag 作为第一个参数传入，这样它包含什么特殊字符都不会报错
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }
}
