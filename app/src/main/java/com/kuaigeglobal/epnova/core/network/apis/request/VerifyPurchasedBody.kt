package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "googleProductId": "谷歌内购商品id",
 *     "verifyData": "支付凭证",
 *     "orderNo": "内部订单号"
 * }
 */
data class VerifyPurchasedBody(
    val verifyData: String? = null,
    val orderNo: String? = null,
    //内购商品编码
    val googleProductId: String? = null,
) : UserTokenBody()