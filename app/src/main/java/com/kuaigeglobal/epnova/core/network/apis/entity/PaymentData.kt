package com.kuaigeglobal.epnova.core.network.apis.entity

data class PaymentData(
    /**支付-签名字符串*/
    val orderStr: String?,
    /**订单编号*/
    val orderNo: String?,
    /**(目前这里只有微信支付有) 支付方式 0-微信支付 1-宝付*/
    val payWay: Int,
    /**(目前这里只有微信支付有) 宝付 跳转小程序原始ID*/
    val originalId: String?
) {
    val isBaoFu get() = payWay == 1
}