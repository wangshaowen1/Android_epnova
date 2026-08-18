package com.kuaigeglobal.epnova.core.google.billing

import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

/**
 * 后端套餐在 Google Play 中最终匹配到的订阅 Offer。
 *
 * Google 会按当前账号资格过滤 [ProductDetails.subscriptionOfferDetails]。当后端配置了首周
 * Offer 时，商店返回该 Offer 才表示当前账号具备优惠资格；若未返回，只允许回退到同一个
 * Base Plan 下 `offerId == null` 的标准价格，绝不能随意选择该 Base Plan 下的其他营销 Offer。
 */
data class ResolvedMemberSubscriptionOffer(
    /** Google 查询得到的 SUBS 商品。 */
    val productDetails: ProductDetails,
    /** 严格匹配后用于展示和购买的 Offer。 */
    val offerDetails: ProductDetails.SubscriptionOfferDetails,
    /** 后端套餐配置的首选 Offer ID；标准套餐为空。 */
    val requestedOfferId: String?,
    /** 是否命中后端配置的首选 Offer；为 false 时表示使用标准 Base Plan 价格。 */
    val usesRequestedOffer: Boolean,
) {
    val productId: String
        get() = productDetails.productId

    val basePlanId: String
        get() = offerDetails.basePlanId

    val offerId: String?
        get() = offerDetails.offerId

    val offerToken: String
        get() = offerDetails.offerToken
}

/**
 * 已完成商店商品解析、可直接交给 BillingManager 拉起会员订阅的参数。
 *
 * 使用普通 class 而不是 data class，避免今后误打印对象时把升级参数内的旧 purchaseToken
 * 展开到日志中。该对象只在内存中短暂使用，不承担进程重启恢复。
 */
class MemberSubscriptionLaunchParams internal constructor(
    val resolvedOffer: ResolvedMemberSubscriptionOffer,
    val obfuscatedAccountId: String,
    val subscriptionUpdateParams: BillingFlowParams.SubscriptionUpdateParams?,
)

/**
 * 商店侧会员启动参数准备结果。
 *
 * 这里的“准备”只包含 Google Product/Offer 查询和 SWITCH 旧购买匹配，与后端
 * `purchase/prepare` 是两个不同阶段；调用方必须先取得后端准备结果，再调用商店侧准备。
 */
sealed class MemberSubscriptionStorePreparationResult {

    /** 商品、Offer 以及可选升级参数均已就绪，可以拉起 Play Billing。 */
    class Ready(val launchParams: MemberSubscriptionLaunchParams) :
        MemberSubscriptionStorePreparationResult()

    /**
     * 当前订阅仍为 PENDING。
     *
     * 此时不能继续升级、confirm、sync 或释放后端 prepare，只能保留上下文等待状态更新。
     */
    class Pending(val purchase: Purchase) : MemberSubscriptionStorePreparationResult()

    /**
     * 查询到了目标订阅，但商店状态仍无法证明 PURCHASED 或 PENDING。
     * 调用方应保留后端准备上下文并稍后重查，不能把该状态当成明确失败。
     */
    class Uncertain(val purchases: List<Purchase>) :
        MemberSubscriptionStorePreparationResult()

    sealed class Failed : MemberSubscriptionStorePreparationResult() {

        /** product/base plan/账号标识为空，或 SWITCH 缺少当前商品标识。 */
        object InvalidArguments : Failed()

        /** 查询目标 ProductDetails 失败；responseCode 为 Google 或本地映射的 Billing code。 */
        data class ProductQueryFailed(val responseCode: Int) : Failed()

        /** Google 查询成功，但没有返回后端指定的 Subscription Product。 */
        object ProductNotFound : Failed()

        /** Product 存在，但没有后端指定的 Base Plan。 */
        object BasePlanNotFound : Failed()

        /** 首选 Offer 不可用且同 Base Plan 下也没有标准 Offer，不能安全拉起支付。 */
        object OfferNotFound : Failed()

        /** 后端返回了当前客户端未支持的 Google ReplacementMode。 */
        object UnsupportedReplacementMode : Failed()

        /** 查询当前 SUBS 失败，无法判断 SWITCH 应使用哪一个旧 purchaseToken。 */
        data class SubscriptionQueryFailed(val responseCode: Int) : Failed()

        /** 没有找到与当前商品及账号绑定标识同时匹配的 PURCHASED 订阅。 */
        object CurrentPurchaseNotFound : Failed()

        /** 同一商品和账号匹配到多条 PURCHASED，无法安全选择要替换的旧订阅。 */
        object AmbiguousCurrentPurchase : Failed()
    }
}
