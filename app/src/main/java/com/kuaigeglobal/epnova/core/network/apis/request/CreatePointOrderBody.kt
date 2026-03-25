package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "goodsCode": "商品编号目前是算力配置表的编号",
 *     "goodsType": "商品类型1-算力",
 *     "mobileType": "手机类型：1 - Android，2 - iOS",
 *     "payMerchantConfigId": "支付商配置表ID"
 * }
 */
data class CreatePointOrderBody(
    /**
     * 商品编号目前是算力配置表的编号
     */
    val goodsCode: String?,
    /**
     * 商品类型1-算力
     */
    val goodsType: Int?,
    /**
     * 支付商配置表ID
     */
    val payMerchantConfigId: String?,
    /**
     * 手机类型：1 - Android，2 - iOS
     */
    val mobileType: Int? = 1,
) : UserTokenBody()