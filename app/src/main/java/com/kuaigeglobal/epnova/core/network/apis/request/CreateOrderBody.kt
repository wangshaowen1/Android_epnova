package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "goodsCode": "商品编号目前是算力配置表的编号",
 *     "goodsType": "商品类型1-算力 2-课程",
 *     "mobileType": "手机类型：1 - Android，2 - iOS",
 *     "payMerchantConfigId": "支付商配置表ID非必填，没传系统会自动匹配"
 * }
 */
data class CreateOrderBody(
    val goodsCode: String? = null,
    val goodsType: Int? = null,
    val mobileType: Int? = null
)