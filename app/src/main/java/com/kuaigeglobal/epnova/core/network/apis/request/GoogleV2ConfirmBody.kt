package com.kuaigeglobal.epnova.core.network.apis.request

data class GoogleV2ConfirmBody(

    /**
     * 平台内部订单号，必须来自 Google V2 预下单；商品类型由订单确定，无需另传 goodsType。
     */
    val orderNo: String,

    /**
     * 商店商品 ID，必须与预下单及官方交易一致。
     */
    val productId: String,

    /**
     * Google 购买令牌，重试时原样传递同一令牌，不能使用商店订单号替代。
     */
    val purchaseToken: String,

    val appsFlyerId: String?
)
