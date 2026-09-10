package com.kuaigeglobal.epnova.core.network.apis.request

data class GoogleV2PreOrderBody(
    /**
     * 商品配置编号：算力传算力商品编号，金币传金币商品编号；不是商店 productId。金币套餐类型由该配置确定。
     */
    val goodsCode: String?,

    /**
     * 商品类型，只接受一次性算力、金币，单商品单份购买；金币当前尚未开放。与旧金币接口的 goodsType（常规金币/新人礼包）含义不同，金币在此统一传 2。
     *
     * 1 算力
     * 2 金币
     *
     */
    val goodsType: Int,

    val appsFlyerId: String?
)
