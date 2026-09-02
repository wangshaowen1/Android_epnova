package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 会员购买准备请求。
 */
data class MemberSubscriptionPrepareBody(
    /** PURCHASE-新购，SWITCH-预约下周期切换。 */
    val actionType: String,
    /** 套餐列表返回的不可变套餐版本编码。 */
    val planVersionCode: String,
    /** af归因id */
    val appsFlyerId: String?
)

/**
 * 会员购买准备锁释放请求。
 */
data class MemberSubscriptionPrepareReleaseBody(
    /** 购买准备接口返回的不透明令牌。 */
    val purchasePrepareToken: String,
    /** af归因id */
    val appsFlyerId: String?
)

/**
 * Google 会员订阅确认或恢复同步请求。
 */
data class GoogleMemberSubscriptionBody(
    /** Play Billing 返回的敏感购买令牌，禁止写入普通日志。 */
    val purchaseToken: String,
    /** Play Billing 返回的 Subscription Product ID。 */
    val productId: String,
    /** 购买准备令牌；恢复同步时允许为空。 */
    val purchasePrepareToken: String? = null,
    /** Google Base Plan ID；恢复同步时允许为空。 */
    val basePlanId: String? = null,
    /** af归因id */
    val appsFlyerId: String?
)
