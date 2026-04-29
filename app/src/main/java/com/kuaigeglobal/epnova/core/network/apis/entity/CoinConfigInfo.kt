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
    val coionsType: Int?,//金币套餐类型 1-常规购买金币套餐、2-新人礼包金币套餐
    val pointConfigId: String?,
    val googleProductId: String?,//Google商品ID
    val coionsConfigCode: String?,
    val coionsName: String?,
    var coionsPrice: String?,//实际支付价格（单位：元）
    val coionsNum: Long?,
    val giveCoionsNum: Long?,//赠送金币数量
    val giveValidDays: Int?,//赠送金币有效期（单位：天）
    /**
     * 是否默认选中（0-非默认、1-默认）
     */
    val selectedStatus: Int?,//是否默认选中（0-非默认、1-默认），与库表对应；列表内至多一条为 1。
    val coionsMarketingTime: Long?,//金币营销倒计时截止时间.空则代表不需要受限制精确到毫秒
    val coionsMarketingText: String?,//金币营销倒计时文本
    val coionsMarketingTag: String?,//金币营销角标文案（如：赠30%）
) : Parcelable {

    fun isDiscountValid(): Boolean {
        return coionsMarketingTime != null && coionsMarketingTime > System.currentTimeMillis()
    }

    fun getCountDownText(): String {
        if (coionsMarketingTime == null) {
            return ""
        }
        val time = coionsMarketingTime - System.currentTimeMillis()
        if (time <= 0) {
            return ""
        }
        val hour = time / 1000 / 60 / 60
        val minute = time / 1000 / 60 % 60
        val second = time / 1000 % 60

        // 将 tag 作为第一个参数传入，这样它包含什么特殊字符都不会报错
        return String.format("%s %02d:%02d:%02d", coionsMarketingTag ?: "", hour, minute, second)
    }
}