package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * 会员购买准备结果。
 */
data class MemberSubscriptionPrepareData(
    /** 购买确认或主动释放准备状态时原样回传的不透明令牌。 */
    val purchasePrepareToken: String? = null,
    /** 平台内部订单号，用于商店侧支付失败事件上报。 */
    val orderNo: String? = null,
    /** Apple Product ID 或 Google Subscription Product ID。 */
    val productId: String? = null,
    /** Google Base Plan ID；Apple 返回空。 */
    val basePlanId: String? = null,
    /** Apple 账户绑定标识；Android 返回空。 */
    val appAccountToken: String? = null,
    /** Google 账户混淆标识；iOS 返回空。 */
    val obfuscatedAccountId: String? = null,
    /** Google 套餐切换模式；新购及 Apple 返回空。 */
    val googleReplacementMode: String? = null,
)

/**
 * 会员购买准备锁释放结果。
 */
data class MemberSubscriptionPrepareReleaseData(
    /** 是否匹配令牌并成功释放：0-否，1-是。 */
    val released: Int? = null,
)
