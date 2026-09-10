package com.kuaigeglobal.epnova.core.network.apis.entity

data class GoogleV2PreOrderInfo(
    /**
     * 平台内部订单号，确认购买及后续重试时原样传回。
     */
    val orderNo: String?,
    /**
     * 本次购买的商店商品 ID。
     */
    val productId: String?,
    /**
     * 平台生成的稳定账户 UUID，Apple 购买时原样传入 appAccountToken；Google 预下单为空。
     */
    val appAccountToken: String?,
    /**
     * 与 Apple 共用的稳定账户 UUID，Google 购买时原样传入 obfuscatedAccountId，无需哈希；Apple 预下单为空。
     */
    val obfuscatedAccountId: String?
)
