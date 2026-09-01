package com.kuaigeglobal.epnova.core.google.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.AppsFlyerManager
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.VerifyPurchasedBody
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.showToastDelay
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object BillingManager {
    //VIP 订阅 (基础)： dt_sub_vip1_month   基础id  vip1-month-plan1
    //VIP 订阅 (标准)： dt_sub_vip2_month   基础id  vip2-month-plan1
    //VIP 订阅 (高级)： dt_sub_vip3_month   基础id  vip3-month-plan1
    //VIP一次性购买   dt_vip_yearly_299  年订阅 $299
    //              dt_vip_semi_year_229  半年订阅 $229
    //              dt_vip_monthly_69 月订阅 $69
    //发行券 (1张)： dt_song_release_1
    //发行券 (5张)： dt_song_release_2
    //发行券 (10张)： dt_song_release_3

    const val TAG = "BillingManager"
    private const val MEMBER_ACTION_PURCHASE = "PURCHASE"
    private const val MEMBER_ACTION_SWITCH = "SWITCH"
    private const val GOOGLE_REPLACEMENT_MODE_WITHOUT_PRORATION = "WITHOUT_PRORATION"
    private var billingClient: BillingClient? = null

    var isConnecting = AtomicBoolean(false)
    private val isPurchasing = AtomicBoolean(false)
    private val activePageCount = AtomicInteger(0) // 有多少个页面在使用支付功能
    private var pendingDisconnect = false // 购买进行中时触发了 endConnection，购买结束后补偿断开

    private val _purchaseResult = MutableSharedFlow<PurchaseResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** 支付结果流，ViewModel 订阅此 Flow 接收结果，不再使用全局回调 */
    val purchaseResultFlow: SharedFlow<PurchaseResult> = _purchaseResult.asSharedFlow()

    private val _memberBillingResult = MutableSharedFlow<MemberBillingResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * 会员订阅的 Play Billing 原始结果流。
     *
     * 此 Flow 与普通积分购买的 [purchaseResultFlow] 完全隔离。它只负责把 Google 的
     * PURCHASED、PENDING、取消或不确定结果交给会员业务层，绝不在 BillingManager 内调用
     * 旧的一次性商品验单接口。会员权益最终是否生效，必须以后续 confirm/sync 为准。
     * Flow 不重放历史事件，会员 ViewModel 必须先启动收集，再允许用户点击购买。
     */
    val memberBillingResultFlow: SharedFlow<MemberBillingResult> =
        _memberBillingResult.asSharedFlow()

    /**
     * 预下单的订单号
     */
    var orderId: String? = null

    /**
     * 当前正在处理的会员 Subscription Product ID。
     *
     * BillingClient 全局只有一个 PurchasesUpdatedListener，必须用该字段先判断回调属于会员
     * 订阅还是旧的一次性购买。它只是一段进程内的“回调路由上下文”，不是服务端订单状态，
     * 也不承担进程重启后的恢复；进程重启场景要通过 [queryMemberSubscriptions] 查询 SUBS。
     *
     * PURCHASED、PENDING 和结果不确定时不能立即清空，否则用户快速二次点击可能覆盖尚未
     * confirm/sync 的交易。只有明确未成交，或业务层已经完成后续收敛时才能清理。
     */
    private var activeMemberProductId: String? = null

    // 商品缓存
    private val productCache = mutableMapOf<String, ProductDetails>()

    init {
        billingClient = createBillingClient()
    }

    fun markActivePage(enter: Boolean) {
        if (enter) {
            activePageCount.incrementAndGet()
        } else {
            if (activePageCount.decrementAndGet() < 0) activePageCount.set(0)
            endConnection()
        }
    }

    fun isActive(): Boolean {
        return activePageCount.get() > 0
    }

    /**
     * 当前是否允许页面开始一次新的会员后端 prepare。
     *
     * 该检查用于避免页面重建后，在上一笔会员交易仍为 PURCHASED/PENDING/未知状态时再次创建
     * prepare。它只是开始请求前的快速快照，最终拉起支付仍由原子 [isPurchasing] 和回调上下文
     * 二次校验，不能替代 [launchMemberSubscription] 内的并发保护。
     */
    fun canStartMemberSubscriptionPreparation(): Boolean {
        return !isPurchasing.get() && orderId == null && activeMemberProductId == null
    }

    /**
     * 为进程重启、页面重建或 PENDING 恢复重新建立会员回调路由。
     *
     * 该方法不拉起支付、不查询商店，只把后续可能到达的 PurchasesUpdatedListener 回调继续
     * 路由到 [memberBillingResultFlow]。若普通积分购买或另一个会员商品正在处理则拒绝绑定，
     * 调用方应保留恢复标记并在下一次前台机会重试，不能覆盖现有支付上下文。
     */
    fun restoreMemberBillingContext(productId: String): Boolean {
        if (productId.isBlank() || orderId != null) return false
        val currentMemberProductId = activeMemberProductId
        if (currentMemberProductId != null && currentMemberProductId != productId) return false
        if (currentMemberProductId == null && isPurchasing.get()) {
            // 普通 INAPP 可能已占用原子购买位、但尚在查询商品而还没写入 orderId。此时也不能
            // 抢先绑定会员路由，否则后续一次性商品回调会被错误分发到会员恢复链路。
            return false
        }
        activeMemberProductId = productId
        return true
    }

    /**
     * 商店 SUBS 查询成功后，用查询事实收敛 launchBillingFlow 的瞬时等待位。
     *
     * 查询结果已经能区分 PURCHASED、PENDING 或无当前购买，此时即使 SDK 没有补发原始回调，
     * 也不应让 [isPurchasing] 永久保持 true。这里只释放瞬时位，仍保留商品路由上下文，直到
     * 上层完成 sync/release/页面刷新后显式调用 [finishMemberBillingFlow]。
     */
    fun markMemberSubscriptionQueryResolved(productId: String) {
        if (activeMemberProductId == productId) {
            isPurchasing.set(false)
        }
    }

    private fun createBillingClient(): BillingClient {
        billingClient?.let { return it }

        return BillingClient.newBuilder(App.instance)
            .setListener { billingResult, purchases ->
                isPurchasing.set(false)

                // 会员订阅与普通一次性购买共用 BillingClient 的唯一回调入口，因此会员上下文
                // 必须优先路由。若先判断 orderId，SUBS 会落入旧 verifyPurchased 验单链路。
                activeMemberProductId?.let { memberProductId ->
                    handleMemberPurchasesUpdated(
                        billingResult = billingResult,
                        purchases = purchases,
                        expectedProductId = memberProductId
                    )
                    return@setListener
                }

                if (orderId == null) {
                    // 进程重启后 orderId 和 activeMemberProductId 都可能丢失，而 Google 回调本身
                    // 不提供可靠的业务链路标识。这里不能直接把 purchases 交给旧验单，否则其中
                    // 的 SUBS 会被当成普通积分商品处理。改为显式只查询 INAPP 后再走旧补偿链路；
                    // SUBS 由会员页面/恢复编排通过 queryMemberSubscriptions 单独处理。
                    queryPendingPurchases { _, newlyVerified, _, allVerified ->
                        if (allVerified && newlyVerified.isNotEmpty()) {
                            App.instance.getString(R.string.system_payment_success).showToast()
                            // 进程重启且没有 ViewModel 订阅时，emit 允许被丢弃，Toast 继续保留旧提示。
                            _purchaseResult.tryEmit(
                                PurchaseResult.Success(newlyVerified.firstOrNull())
                            )
                        }
                    }
                    return@setListener
                }
                val currentOrderId = orderId
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {//购买成功
                        Logger.d(TAG, "Purchases updated: $purchases")
                        handlePurchases(purchases) { _, _, _, allVerified ->
                            orderId = null
                            if (allVerified) {
                                showToast(R.string.system_payment_success)
                                _purchaseResult.tryEmit(PurchaseResult.Success(purchases?.firstOrNull()))
                            } else {
                                _purchaseResult.tryEmit(PurchaseResult.Failed.BenefitFailed)
                            }
                            tryDisconnectAfterPurchase()
                        }
                    }

                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED, BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, BillingClient.BillingResponseCode.ERROR -> {//已经拥有，超时，未知错误，均有可能支付成功
                        val reason =
                            "Google购买异常===>PurchasesUpdatedListener: $billingResult , purchase:${purchases?.getOrNull(0)} order=$currentOrderId userId=${User.get().userId}"
                        FirebaseCrashlytics.getInstance().recordException(Exception(reason))
                        // 第一次立即查
                        queryPendingPurchases { list, _, verifyFailedList, _ ->
                            val purchase = list?.find { it.accountIdentifiers?.obfuscatedAccountId == currentOrderId }
                            if (purchase != null) {
                                orderId = null
                                val isCurrentVerifyFailed = verifyFailedList.any {
                                    it.accountIdentifiers?.obfuscatedAccountId == currentOrderId
                                }
                                if (isCurrentVerifyFailed) {
                                    _purchaseResult.tryEmit(PurchaseResult.Failed.BenefitFailed)
                                } else {
                                    R.string.system_payment_success.getResString().showToastDelay(500L)
                                    _purchaseResult.tryEmit(PurchaseResult.Success(purchase))
                                }
                            } else {
                                R.string.system_payment_failed.getResString().showToastDelay(500L)
                                _purchaseResult.tryEmit(PurchaseResult.Failed.PaymentFailed)
                                reportOrderStatus(currentOrderId, 2, reason)
                            }
                            tryDisconnectAfterPurchase()
                        }
                    }

                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        Logger.d(
                            TAG,
                            "Purchases failed: $billingResult ,purchase:${purchases?.getOrNull(0)?.toString()}"
                        )
                        reportOrderStatus(currentOrderId, 3)
                        R.string.system_payment_cancel.getResString().showToastDelay(500L)
                        _purchaseResult.tryEmit(PurchaseResult.Cancelled)
                        tryDisconnectAfterPurchase()
                    }

                    else -> {
                        val reason =
                            "Google购买失败：PurchasesUpdatedListener: $billingResult , purchase:${purchases?.getOrNull(0)} order=$currentOrderId  userId=${User.get().userId}"
                        FirebaseCrashlytics.getInstance().recordException(
                            Exception(reason)
                        )
                        reportOrderStatus(currentOrderId, 2, reason)
                        showToast(R.string.system_payment_failed)
                        _purchaseResult.tryEmit(PurchaseResult.Failed.PaymentFailed)
                        tryDisconnectAfterPurchase()
                    }
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().apply {
                enableOneTimeProducts()
                enablePrepaidPlans()
            }.build())
            .enableAutoServiceReconnection()
            .build()
    }

    /**
     * 处理会员订阅的 Google 回调，只下发商店事实，不调用任何服务端会员接口。
     *
     * 本步骤刻意把 confirm/sync/release 留给后续业务编排层：BillingManager 无法知道当前
     * prepare token、目标 base plan 以及页面应刷新哪些数据，因此不应在全局 SDK 管理器里
     * 擅自判定会员权益成功或释放后端准备锁。
     */
    private fun handleMemberPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
        expectedProductId: String,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // 回调可能包含账号下的多条购买，只接收本次拉起时记录的 Subscription Product。
                // 不能简单取 firstOrNull，否则多订阅/历史购买并存时可能把错误 token 交给 confirm。
                val currentPurchase = purchases.orEmpty().firstOrNull { purchase ->
                    expectedProductId in purchase.products
                }
                if (currentPurchase == null) {
                    // OK 但没有本次商品，不足以证明交易没有创建，继续保留上下文并查询 SUBS。
                    _memberBillingResult.tryEmit(
                        MemberBillingResult.Uncertain(billingResult.responseCode)
                    )
                } else {
                    when (currentPurchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            _memberBillingResult.tryEmit(
                                MemberBillingResult.Purchased(currentPurchase)
                            )
                        }

                        Purchase.PurchaseState.PENDING -> {
                            _memberBillingResult.tryEmit(
                                MemberBillingResult.Pending(currentPurchase)
                            )
                        }

                        else -> {
                            // UNSPECIFIED 同样不足以证明未成交，由调用方查询 SUBS 后决定 sync
                            // 或仅刷新后端状态；此处不能释放 prepare 或开放重复购买。
                            _memberBillingResult.tryEmit(
                                MemberBillingResult.Uncertain(billingResult.responseCode)
                            )
                        }
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // USER_CANCELED 是当前唯一可以直接认定“用户明确未成交”的商店回调。
                // 回调路由保留到业务层完成 prepare/release；若其间极少数设备又补发交易更新，
                // 仍应继续交给会员链路，而不是落入普通 INAPP 的进程恢复分支。
                _memberBillingResult.tryEmit(MemberBillingResult.Cancelled)
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.ERROR -> {
                // 以上响应均存在商店已生成交易但客户端没有拿到完整结果的可能。先查询 SUBS，
                // 不能释放 prepare，也不能清上下文后允许用户立刻重复购买。
                _memberBillingResult.tryEmit(
                    MemberBillingResult.Uncertain(billingResult.responseCode)
                )
            }

            else -> {
                // 其余响应（如商品不可用、开发者配置错误）可判定本次没有正常进入交易流程。
                // 仍由业务层先释放 prepare，再显式结束会员路由上下文。
                _memberBillingResult.tryEmit(
                    MemberBillingResult.Failed.Billing(billingResult.responseCode)
                )
            }
        }
        tryDisconnectAfterPurchase()
    }


    /**
     * state -1 连接失败  1 连接ing  2 连接成功
     * google支付服务发起连接的方法
     */
    private fun connectIfNeeded(onConnected: (state: Int) -> Unit) {
        if (billingClient?.isReady == true) {
            onConnected(2)
        } else {
            if (isConnecting.get()) {
                onConnected(1)
                Logger.d(TAG, "Billing service connection is already in progress")
                return
            }
            isConnecting.set(true)
            if (billingClient == null) {
                billingClient = createBillingClient()
            }
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    isConnecting.set(false)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Logger.d(TAG, "Billing service connected")
                        onConnected(2)
                    } else {
                        onConnected(-1)
                        Logger.d(
                            TAG,
                            "Billing service connection failed with response code : ${result.responseCode} and debug message : ${result.debugMessage}"
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnecting.set(false)
                    // SDK 拉起动作已经中断，所以释放瞬时互斥位；会员上下文不会在此清理，
                    // activeMemberProductId 仍会阻止重复购买，直到查询 SUBS 并完成业务收敛。
                    isPurchasing.set(false)
                    // 网络不稳定会走这里，可重连
                    Logger.d(TAG, "Billing service disconnected")
                }
            })
        }
    }

    /**
     * 批量获取 Google 商品价格
     * @param productIds INAPP 类型的商品 ID 列表
     * @param onSuccess key=productId，value=Triple(格式化价格, 微分价格, 货币代码)
     */
    fun loadPowerPrices(
        productIds: List<String>,
        onSuccess: ((HashMap<String, Triple<String, Long, String>>) -> Unit)? = null
    ) {
        val pairs = productIds.map { Pair(it, BillingClient.ProductType.INAPP) }
        preloadProductDetails(pairs) { list ->
            val map = HashMap<String, Triple<String, Long, String>>()
            list?.forEach { details ->
                getPriceParameters(details)?.apply {
                    map[details.productId] = this
                }
            }
            onSuccess?.invoke(map)
        }
    }

    /**
     * 获取google上价格
     */
    fun loadNewerGiftPrice(
        productId: String?,
        onSuccess: ((HashMap<String, Triple<String, Long, String>>) -> Unit)? = null
    ) {
        val pair = Pair(productId, BillingClient.ProductType.INAPP)
        preloadProductDetails(listOf(pair)) { list ->
            val map = HashMap<String, Triple<String, Long, String>>()
            list?.find { productId == it.productId }?.let { details ->
                getPriceParameters(details)?.apply {
                    map[details.productId] = this
                }
            }
            onSuccess?.invoke(map)
        }
    }

    /**
     * 预加载商品价格
     */
    fun preloadProductDetails(
        products: List<Pair<String?, String>>?,
        onSuccess: ((MutableList<ProductDetails>?) -> Unit)? = null
    ) {
        // products: [("productId1", "inapp"), ("productId2", "subs")]
        if (products.isNullOrEmpty()) {
            onSuccess?.invoke(null)
            return
        }
        Logger.d(TAG, "获取价格 入参  products=${products}")
        connectIfNeeded { state ->
            if (state != 2) {
                Logger.d(TAG, "获取价格失败，连接状态: $state")
                onSuccess?.invoke(null)
                return@connectIfNeeded
            }
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    products.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it.first ?: "")
                            .setProductType(it.second)
                            .build()
                    }
                ).build()
            Logger.d(
                TAG,
                "queryProductDetailsAsync调用 billingClient=${billingClient} , queryParams=${queryParams}"
            )
            billingClient?.queryProductDetailsAsync(queryParams) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    list.productDetailsList.forEach { details ->
                        productCache[details.productId] = details
                        Logger.d(TAG, "获取商品ID成功: details = $details")
                    }
                    onSuccess?.invoke(list.productDetailsList)
                    // 未能获取详情的商品列表
                    list.unfetchedProductList.forEach { unfetchedProduct ->
                        // 记录或提示错误
                        Logger.d(TAG, "未获取商品ID: ${unfetchedProduct.productId}, 原因: ${unfetchedProduct.statusCode}")
                    }
                } else {
                    onSuccess?.invoke(null)
                    Logger.d(TAG, "获取价格失败 result=${result} , list=${list}")
                }
            }
        }
    }

    /**
     * 在指定 Subscription Product 中严格解析后端套餐对应的 Base Plan / Offer。
     *
     * 匹配规则：
     * 1. Base Plan ID 必须完全一致，不允许回退到同商品的其他 Base Plan；
     * 2. 后端未配置 Offer 时，只允许 `offerId == null` 的标准 Base Plan Offer；
     * 3. 后端配置了首选 Offer 且 Google 返回该 Offer 时使用它；
     * 4. Google 因账号不具备优惠资格而未返回首选 Offer 时，只回退到同 Base Plan 的标准
     *    Offer，绝不选择其他营销 Offer。
     *
     * 该方法是纯解析，不发起商店查询，可同时用于套餐价格展示和购买前的最终校验，保证两处
     * 不会各自实现一套不一致的 fallback 规则。
     */
    fun resolveMemberSubscriptionOffer(
        productDetails: ProductDetails,
        basePlanId: String?,
        requestedOfferId: String?,
    ): ResolvedMemberSubscriptionOffer? {
        val targetBasePlanId = basePlanId?.takeIf { it.isNotBlank() } ?: return null
        if (productDetails.productType != BillingClient.ProductType.SUBS) return null

        val normalizedOfferId = requestedOfferId?.takeIf { it.isNotBlank() }
        val basePlanOffers = productDetails.subscriptionOfferDetails.orEmpty()
            .filter { offer -> offer.basePlanId == targetBasePlanId }
        if (basePlanOffers.isEmpty()) return null

        val requestedOffer = normalizedOfferId?.let { offerId ->
            basePlanOffers.firstOrNull { offer -> offer.offerId == offerId }
        }
        val standardOffer = basePlanOffers.firstOrNull { offer ->
            offer.offerId.isNullOrBlank()
        }
        val selectedOffer = when {
            normalizedOfferId == null -> standardOffer
            requestedOffer != null -> requestedOffer
            else -> standardOffer
        } ?: return null

        return ResolvedMemberSubscriptionOffer(
            productDetails = productDetails,
            offerDetails = selectedOffer,
            requestedOfferId = normalizedOfferId,
            usesRequestedOffer = normalizedOfferId != null && requestedOffer != null,
        )
    }

    /**
     * 根据后端 purchase/prepare 和当前套餐上下文，准备可直接拉起 Google 会员订阅的参数。
     *
     * 该方法不调用后端 purchase/prepare，也不拉起支付页。调用顺序应为：
     * `后端 prepare -> 本方法 -> Ready 后 launchMemberSubscription`。
     *
     * PURCHASE 只解析目标 Product/Base Plan/Offer；SWITCH 除此之外还会查询当前 SUBS，严格
     * 匹配当前商品与 [obfuscatedAccountId] 后取旧 purchaseToken，并按后端返回的
     * WITHOUT_PRORATION 构造 SubscriptionUpdateParams。任何不确定状态都不会猜测 token。
     *
     * @param actionType 套餐动作，仅支持 PURCHASE 或 SWITCH。
     * @param productId 后端 prepare 返回的目标 Subscription Product ID。
     * @param basePlanId 后端 prepare 返回的目标 Base Plan ID。
     * @param requestedOfferId 套餐列表中的首选 Offer ID；无独立 Offer 时为空。
     * @param obfuscatedAccountId 后端 prepare 返回的账号绑定标识。
     * @param googleReplacementMode 新购为空；SWITCH 当前仅支持 WITHOUT_PRORATION。
     * @param currentProductId SWITCH 时套餐列表中 action=CURRENT 的 Subscription Product ID。
     */
    fun loadMemberSubscriptionLaunchParams(
        actionType: String,
        productId: String,
        basePlanId: String,
        requestedOfferId: String?,
        obfuscatedAccountId: String,
        googleReplacementMode: String?,
        currentProductId: String? = null,
        onResult: (MemberSubscriptionStorePreparationResult) -> Unit,
    ) {
        val normalizedReplacementMode = googleReplacementMode?.takeIf { it.isNotBlank() }
        val isPurchase = actionType == MEMBER_ACTION_PURCHASE
        val isSwitch = actionType == MEMBER_ACTION_SWITCH
        val hasValidActionArguments = when {
            isPurchase -> normalizedReplacementMode == null
            isSwitch -> !currentProductId.isNullOrBlank() && normalizedReplacementMode != null
            else -> false
        }
        if (
            productId.isBlank() ||
            basePlanId.isBlank() ||
            obfuscatedAccountId.isBlank() ||
            !hasValidActionArguments
        ) {
            onResult(MemberSubscriptionStorePreparationResult.Failed.InvalidArguments)
            return
        }
        if (isSwitch && normalizedReplacementMode != GOOGLE_REPLACEMENT_MODE_WITHOUT_PRORATION) {
            // 替换模式是后端与 Play Console 订阅策略的一部分，客户端不能静默换成其他模式。
            onResult(MemberSubscriptionStorePreparationResult.Failed.UnsupportedReplacementMode)
            return
        }

        queryMemberProductDetails(productId) { billingResult, productDetails ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(
                    MemberSubscriptionStorePreparationResult.Failed.ProductQueryFailed(
                        billingResult.responseCode
                    )
                )
                return@queryMemberProductDetails
            }
            if (productDetails == null) {
                onResult(MemberSubscriptionStorePreparationResult.Failed.ProductNotFound)
                return@queryMemberProductDetails
            }

            // 先区分 Base Plan 缺失与 Offer 不可用，后续页面可以给出更准确的配置错误提示。
            val hasBasePlan = productDetails.subscriptionOfferDetails.orEmpty()
                .any { offer -> offer.basePlanId == basePlanId }
            if (!hasBasePlan) {
                onResult(MemberSubscriptionStorePreparationResult.Failed.BasePlanNotFound)
                return@queryMemberProductDetails
            }
            val resolvedOffer = resolveMemberSubscriptionOffer(
                productDetails = productDetails,
                basePlanId = basePlanId,
                requestedOfferId = requestedOfferId,
            )
            if (resolvedOffer == null) {
                onResult(MemberSubscriptionStorePreparationResult.Failed.OfferNotFound)
                return@queryMemberProductDetails
            }

            if (isPurchase) {
                onResult(
                    MemberSubscriptionStorePreparationResult.Ready(
                        MemberSubscriptionLaunchParams(
                            resolvedOffer = resolvedOffer,
                            obfuscatedAccountId = obfuscatedAccountId,
                            subscriptionUpdateParams = null,
                        )
                    )
                )
                return@queryMemberProductDetails
            }

            loadMemberSubscriptionSwitchParams(
                resolvedOffer = resolvedOffer,
                currentProductId = requireNotNull(currentProductId),
                obfuscatedAccountId = obfuscatedAccountId,
                onResult = onResult,
            )
        }
    }

    /**
     * 查询单个会员 Subscription Product。这里不复用“任意首项”逻辑，查询成功后仍按 productId
     * 和 SUBS 类型二次过滤，避免 Play Console 配置或缓存异常时拿错商品。
     */
    private fun queryMemberProductDetails(
        productId: String,
        onResult: (billingResult: BillingResult, productDetails: ProductDetails?) -> Unit,
    ) {
        connectIfNeeded { state ->
            if (state != 2) {
                onResult(
                    buildLocalBillingResult(
                        responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                        debugMessage = "Billing service is not connected"
                    ),
                    null
                )
                return@connectIfNeeded
            }

            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()
            val client = billingClient
            if (client == null) {
                onResult(
                    buildLocalBillingResult(
                        responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                        debugMessage = "Billing client is unavailable"
                    ),
                    null
                )
                return@connectIfNeeded
            }

            try {
                client.queryProductDetailsAsync(queryParams) { billingResult, queryResult ->
                    val productDetails = queryResult.productDetailsList.firstOrNull { details ->
                        details.productId == productId &&
                            details.productType == BillingClient.ProductType.SUBS
                    }
                    if (
                        billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        productDetails != null
                    ) {
                        productCache[productDetails.productId] = productDetails
                    }
                    onResult(billingResult, productDetails)
                }
            } catch (exception: Exception) {
                // 同步调用异常表示本次商品查询没有获得商店事实，不能继续构造购买参数。
                Logger.e(TAG, "queryMemberProductDetails: ${exception.message}")
                onResult(
                    buildLocalBillingResult(
                        responseCode = BillingClient.BillingResponseCode.ERROR,
                        debugMessage = "Failed to query member product details"
                    ),
                    null
                )
            }
        }
    }

    /**
     * 为 SWITCH 查找唯一、账号匹配且已 PURCHASED 的当前订阅，并构造 Google 替换参数。
     */
    private fun loadMemberSubscriptionSwitchParams(
        resolvedOffer: ResolvedMemberSubscriptionOffer,
        currentProductId: String,
        obfuscatedAccountId: String,
        onResult: (MemberSubscriptionStorePreparationResult) -> Unit,
    ) {
        queryMemberSubscriptions { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(
                    MemberSubscriptionStorePreparationResult.Failed.SubscriptionQueryFailed(
                        billingResult.responseCode
                    )
                )
                return@queryMemberSubscriptions
            }

            // 同一 Google 账号可能存在其他订阅。只有商品和后端账号绑定标识均匹配，才允许
            // 把 purchaseToken 用作本次会员切换的 oldPurchaseToken。
            val matchingPurchases = purchases.filter { purchase ->
                currentProductId in purchase.products &&
                    purchase.accountIdentifiers?.obfuscatedAccountId == obfuscatedAccountId
            }
            matchingPurchases.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PENDING
            }?.let { pendingPurchase ->
                onResult(MemberSubscriptionStorePreparationResult.Pending(pendingPurchase))
                return@queryMemberSubscriptions
            }

            val uncertainPurchases = matchingPurchases.filter {
                it.purchaseState != Purchase.PurchaseState.PURCHASED
            }
            if (uncertainPurchases.isNotEmpty()) {
                onResult(
                    MemberSubscriptionStorePreparationResult.Uncertain(uncertainPurchases)
                )
                return@queryMemberSubscriptions
            }

            val purchased = matchingPurchases.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (purchased.isEmpty()) {
                onResult(
                    MemberSubscriptionStorePreparationResult.Failed.CurrentPurchaseNotFound
                )
                return@queryMemberSubscriptions
            }
            if (purchased.size > 1) {
                // 不能用 firstOrNull 猜测旧 token；选错会替换账号下另一条有效订阅。
                onResult(
                    MemberSubscriptionStorePreparationResult.Failed.AmbiguousCurrentPurchase
                )
                return@queryMemberSubscriptions
            }

            val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(purchased.single().purchaseToken)
                .setSubscriptionReplacementMode(
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION
                )
                .build()
            onResult(
                MemberSubscriptionStorePreparationResult.Ready(
                    MemberSubscriptionLaunchParams(
                        resolvedOffer = resolvedOffer,
                        obfuscatedAccountId = obfuscatedAccountId,
                        subscriptionUpdateParams = updateParams,
                    )
                )
            )
        }
    }

    /**
     * 获取不带单位的价格，但这里需要除1000
     */
    private fun getPriceAmountMicros(details: ProductDetails?): Long {
        details ?: return 0
        return if (details.productType == BillingClient.ProductType.SUBS) {
            details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros ?: 0
        } else {
            details.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0
        }
    }

    /**
     * 获取不带单位的价格，跟所有的
     */
    private fun getPriceParameters(details: ProductDetails?): Triple<String, Long, String>? {
        details ?: return null
        if (details.productType == BillingClient.ProductType.SUBS) {
            details.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.apply {
                    return Triple(formattedPrice, priceAmountMicros, priceCurrencyCode)
                }
            return null
        }
        details.oneTimePurchaseOfferDetails?.apply {
            return Triple(formattedPrice, priceAmountMicros, priceCurrencyCode)
        }
        return null
    }

    /**
     * 发起普通积分的一次性商品购买。结果通过 [purchaseResultFlow] 下发，调用方需提前订阅。
     *
     * [productType] 暂时保留是为了兼容已有调用签名，但该旧入口只允许值 1。会员订阅必须调用
     * [launchMemberSubscription]，不能再借用旧 orderId、验单和消费链路。
     *
     * @param productId Google INAPP 商品 ID。
     * @param orderId 普通积分购买的自建预订单 ID。
     * @param productType 兼容参数，仅允许 1（一次性商品）；2 会被明确拒绝。
     */
    fun launchPurchase(
        activity: Activity?,
        productId: String,
        orderId: String,
        productType: Int = 1,
    ) {
        if (activity == null) {
            Logger.d(TAG, "activity is null")
            return
        }
        if (productType != 1) {
            // 从入口处关闭旧 SUBS 分支，保证会员购买不会写入 orderId 或进入 verifyPurchased。
            Logger.d(TAG, "launchPurchase only supports INAPP products")
            _purchaseResult.tryEmit(PurchaseResult.Failed.LaunchFailed)
            return
        }
        connectIfNeeded { state ->
            // Google 只有一个购买回调：旧 orderId 未结清、会员上下文未收敛或 SDK 正在拉起时，
            // 都禁止启动另一笔一次性购买，避免回调被错误路由到会员或旧验单链路。
            if (
                state != 2 ||
                this.orderId != null ||
                activeMemberProductId != null ||
                !isPurchasing.compareAndSet(false, true)
            ) {
                return@connectIfNeeded
            }
            launchFlow(
                activity,
                productId,
                orderId,
                BillingClient.ProductType.INAPP
            )
        }
    }

    /**
     * 使用 [loadMemberSubscriptionLaunchParams] 已校验完成的参数拉起会员订阅。
     *
     * 该重载让业务层无需再次拆解 ProductDetails、offerToken 和 SWITCH 更新参数，避免准备阶段
     * 已经严格匹配后，调用阶段又误传另一组 Offer 或旧 purchaseToken。
     */
    fun launchMemberSubscription(
        activity: Activity?,
        launchParams: MemberSubscriptionLaunchParams,
    ) {
        launchMemberSubscription(
            activity = activity,
            productDetails = launchParams.resolvedOffer.productDetails,
            offerToken = launchParams.resolvedOffer.offerToken,
            obfuscatedAccountId = launchParams.obfuscatedAccountId,
            subscriptionUpdateParams = launchParams.subscriptionUpdateParams,
        )
    }

    /**
     * 拉起会员订阅的 Google Play 支付页。
     *
     * 这是会员订阅专用的底层入口，与 [launchPurchase] 的一次性商品订单链路完全隔离：
     * - 不写入旧链路的 [orderId]，避免回调进入 verifyPurchased；
     * - 不在这里查询/猜测 offer，调用方必须先按 productId + basePlanId + offerId 严格匹配；
     * - 只下发 [memberBillingResultFlow]，PURCHASED 后仍需业务层调用会员 confirm；
     * - SWITCH 时由调用方传入包含旧 purchaseToken 和替换模式的 [subscriptionUpdateParams]。
     *
     * 调用前必须已经订阅 [memberBillingResultFlow]，避免同步参数错误或启动失败事件无人接收。
     *
     * @param productDetails 已按后端 productId 查询到的 SUBS 商品详情。
     * @param offerToken 已按目标 base plan/offer 严格匹配得到的 Google offerToken。
     * @param obfuscatedAccountId purchase/prepare 返回的 Google 账号混淆标识，必须原样传入。
     * @param subscriptionUpdateParams 新购传 null；升级/切换时传旧订阅 token 与替换模式。
     */
    fun launchMemberSubscription(
        activity: Activity?,
        productDetails: ProductDetails,
        offerToken: String,
        obfuscatedAccountId: String,
        subscriptionUpdateParams: BillingFlowParams.SubscriptionUpdateParams? = null,
    ) {
        // 在连接 BillingClient 前完成纯参数校验。校验失败说明商店流程尚未开始，可以明确失败，
        // 不需要查询 SUBS，也不能创建会员回调上下文。offerToken 还必须确实属于传入商品，
        // 防止业务层匹配错误后把另一个 base plan/offer 的 token 与当前 ProductDetails 混用。
        val containsOfferToken = productDetails.subscriptionOfferDetails.orEmpty()
            .any { offerDetails -> offerDetails.offerToken == offerToken }
        if (
            activity == null ||
            productDetails.productType != BillingClient.ProductType.SUBS ||
            offerToken.isBlank() ||
            !containsOfferToken ||
            obfuscatedAccountId.isBlank()
        ) {
            _memberBillingResult.tryEmit(MemberBillingResult.Failed.InvalidArguments)
            return
        }

        connectIfNeeded { state ->
            // BillingClient 只有一个购买回调。旧 orderId、会员上下文、SDK 正在拉起中的任一状态
            // 存在时都不能并发启动另一笔交易，否则无法可靠判断回调应交给哪条服务端链路。
            if (orderId != null || activeMemberProductId != null) {
                _memberBillingResult.tryEmit(MemberBillingResult.Failed.Busy)
                return@connectIfNeeded
            }
            if (state != 2) {
                // connectIfNeeded 的 state 是项目内部连接状态，不是 Google responseCode。
                // 对外统一映射为 SERVICE_DISCONNECTED，表示本次尚未进入商店支付页。
                _memberBillingResult.tryEmit(
                    MemberBillingResult.Failed.Billing(
                        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
                    )
                )
                return@connectIfNeeded
            }
            if (!isPurchasing.compareAndSet(false, true)) {
                _memberBillingResult.tryEmit(MemberBillingResult.Failed.Busy)
                return@connectIfNeeded
            }

            activeMemberProductId = productDetails.productId

            try {
                val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
                val flowBuilder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .setObfuscatedAccountId(obfuscatedAccountId)
                subscriptionUpdateParams?.let { params ->
                    flowBuilder.setSubscriptionUpdateParams(params)
                }

                val launchResult = billingClient?.launchBillingFlow(
                    activity,
                    flowBuilder.build()
                )
                if (launchResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                    isPurchasing.set(false)
                    val responseCode = launchResult?.responseCode
                        ?: BillingClient.BillingResponseCode.ERROR
                    when (responseCode) {
                        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                        BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
                        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                        BillingClient.BillingResponseCode.NETWORK_ERROR,
                        BillingClient.BillingResponseCode.ERROR -> {
                            // launchBillingFlow 的同步返回也可能是“商店已有交易，但客户端没有拿到
                            // 完整结果”。这些响应与 PurchasesUpdatedListener 中的同名响应保持一致：
                            // 保留 activeMemberProductId 和 prepare，由上层先查询当前 SUBS，绝不能
                            // 在 ITEM_ALREADY_OWNED、超时或网络结果未知时直接释放后端准备锁。
                            _memberBillingResult.tryEmit(
                                MemberBillingResult.Uncertain(responseCode)
                            )
                        }

                        BillingClient.BillingResponseCode.USER_CANCELED -> {
                            // SDK 明确返回用户取消，能够确认本次拉起未成交；交给业务层 release。
                            // 路由上下文保留到 release 完成，防止迟到的交易更新被误分到 INAPP。
                            _memberBillingResult.tryEmit(MemberBillingResult.Cancelled)
                        }

                        else -> {
                            // 商品不可用、开发者配置错误等响应可以确定没有正常进入交易流程，
                            // 允许业务层释放 prepare，完成后再结束本次回调路由上下文。
                            _memberBillingResult.tryEmit(
                                MemberBillingResult.Failed.Billing(responseCode)
                            )
                        }
                    }
                    tryDisconnectAfterPurchase()
                }
            } catch (exception: Exception) {
                // SDK 调用抛异常时无法从客户端证明商店一定没有接受交易。按“结果未知”保留
                // 回调路由上下文，后续查询 SUBS 再决定 sync 或结束；不能因为本地异常直接
                // release。保留现有项目日志策略，但不在这里改变任何旧日志行为。
                isPurchasing.set(false)
                Logger.e(TAG, "launchMemberSubscription: ${exception.message}")
                _memberBillingResult.tryEmit(
                    MemberBillingResult.Uncertain(BillingClient.BillingResponseCode.ERROR)
                )
                tryDisconnectAfterPurchase()
            }
        }
    }

    /**
     * 购买获取商品详情
     */
    private fun launchFlow(activity: Activity, productId: String, orderId: String, productType: String) {
        // 缓存同时服务价格展示，可能包含 SUBS。旧购买入口只能复用类型完全匹配的 INAPP，
        // 防止同 ID 的错误缓存让一次性购买意外走到订阅商品。
        val details = productCache[productId]?.takeIf { it.productType == productType }
        if (details == null) {
            // 没缓存就查一次
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(productType)
                            .build()
                    )
                )
                .build()

            billingClient?.queryProductDetailsAsync(queryParams) { result, list ->
                Logger.i(TAG, "查询商品信息:${list}")
                Logger.i(TAG, "result:${result}")
                if (result.responseCode == BillingClient.BillingResponseCode.OK && list.productDetailsList.isNotEmpty()) {
                    productCache[productId] = list.productDetailsList[0]
                    launchBillingFlow(activity, list.productDetailsList[0], orderId)
                } else {
                    isPurchasing.set(false)
                    _purchaseResult.tryEmit(PurchaseResult.Failed.LaunchFailed)
                    tryDisconnectAfterPurchase()
                    Logger.d(TAG, "查询商品信息失败")
                }
            }
        } else {
            launchBillingFlow(activity, details, orderId)
        }
    }

    /**
     * 调起普通积分 INAPP 的 Google 支付页。
     *
     * 会员订阅已经迁移到 [launchMemberSubscription]。这里保留类型防御，即使未来调用方传错
     * ProductDetails，也不会重新启用旧的 SUBS + verifyPurchased 链路。
     */
    private fun launchBillingFlow(activity: Activity, details: ProductDetails, orderId: String) {
        if (details.productType != BillingClient.ProductType.INAPP) {
            isPurchasing.set(false)
            _purchaseResult.tryEmit(PurchaseResult.Failed.LaunchFailed)
            tryDisconnectAfterPurchase()
            return
        }
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .setObfuscatedAccountId(orderId)
            .setObfuscatedProfileId(User.get().userId ?: "")
            .setIsOfferPersonalized(true) // 面向欧美市场的GDPR设置
            .build()
        try {
            this.orderId = orderId
            val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
            if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
                isPurchasing.set(false)
                val reason = "Google购买失败：launchBillingFlow: $result ,productId:${details.productId}"
                FirebaseCrashlytics.getInstance().recordException(
                    Exception(reason)
                )
                // 购买流程启动失败，立即上报失败
                reportOrderStatus(orderId, 2, reason)
                _purchaseResult.tryEmit(PurchaseResult.Failed.LaunchFailed)
                tryDisconnectAfterPurchase()
            }
        } catch (e: Exception) {
            isPurchasing.set(false)
            Logger.e(TAG, "launchBillingFlow:${e.message}")
            val reason = "Google购买失败：launchBillingFlow 异常: cause:${e.cause},message:${e.message},productId:${details.productId}"
            FirebaseCrashlytics.getInstance().recordException(
                Exception(reason)
            )
            // 购买流程启动失败，立即上报失败
            reportOrderStatus(orderId, 2, reason)
            _purchaseResult.tryEmit(PurchaseResult.Failed.LaunchFailed)
            tryDisconnectAfterPurchase()
        }
    }

    /**
     * 查询并补偿普通积分的一次性商品历史购买。
     *
     * 该方法是旧 INAPP 链路的一部分，查询结果会进入 [handlePurchases] 并调用旧验单接口，
     * 因此必须严格只查询 [BillingClient.ProductType.INAPP]。会员 SUBS 只能通过
     * [queryMemberSubscriptions] 查询，禁止在这里合并两种商品类型。
     *
     * @param onConsumeOver newlyVerified 为本次新验签成功的 INAPP 列表，可用于后台补偿提示。
     */
    fun queryPendingPurchases(
        onConsumeOver: ((
            all: List<Purchase>?,
            newlyVerified: List<Purchase>,
            verifyFailedList: List<Purchase>,
            allVerified: Boolean
        ) -> Unit)? = null
    ) {
        connectIfNeeded { state ->
            if (state != 2) {
                Logger.d(TAG, "查询历史购买失败，连接状态: $state")
                onConsumeOver?.invoke(null, emptyList(), emptyList(), false)
                return@connectIfNeeded
            }
            ioScope.launchNoCrash {
                val purchases = withContext(Dispatchers.IO) {
                    try {
                        billingClient?.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        )?.purchasesList ?: listOf()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (exception: Exception) {
                        // 首页会在该回调之后串行启动会员 SUBS 恢复，因此查询异常也必须回调，
                        // 不能因为旧 INAPP 补偿失败而永久跳过会员进程恢复或泄漏页面计数。
                        Logger.e(TAG, "queryPendingPurchases: ${exception.message}")
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    if (purchases == null) {
                        onConsumeOver?.invoke(null, emptyList(), emptyList(), false)
                    } else {
                        handlePurchases(purchases) { list, newlyVerified, verifyFailedList, allVerified ->
                            onConsumeOver?.invoke(list, newlyVerified, verifyFailedList, allVerified)
                        }
                    }
                }
            }
        }
    }

    /**
     * 只查询当前 Google 账号下的会员订阅购买记录。
     *
     * 该方法是会员恢复/结果确认的“只读商店查询”能力：
     * - 只查询 SUBS，不查询 INAPP；
     * - 不调用旧 verifyPurchased，不 consume，也不 acknowledge；
     * - 不自动调用会员 confirm/sync，因为调用方还需要结合 prepare 上下文判断正确接口；
     * - 返回 PENDING 时调用方必须继续保留上下文，不能释放 prepare 或重复购买。
     *
     * 查询结果原样交给后续会员业务编排。正常本次购买成功优先 confirm；只有结果未知、
     * 已拥有、重启恢复等场景，在查到 PURCHASED 后才使用 sync。
     */
    fun queryMemberSubscriptions(
        onResult: (billingResult: BillingResult, purchases: List<Purchase>) -> Unit,
    ) {
        connectIfNeeded { state ->
            if (state != 2) {
                onResult(
                    buildLocalBillingResult(
                        responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                        debugMessage = "Billing service is not connected"
                    ),
                    emptyList()
                )
                return@connectIfNeeded
            }

            ioScope.launchNoCrash {
                val queryResult = withContext(Dispatchers.IO) {
                    try {
                        billingClient?.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    } catch (cancellation: CancellationException) {
                        // 协程取消属于上层生命周期控制，必须继续向上传递，不能伪装成商店查询失败。
                        throw cancellation
                    } catch (exception: Exception) {
                        // 查询异常只表示客户端当前无法获知商店事实，不能据此认定交易失败。
                        Logger.e(TAG, "queryMemberSubscriptions: ${exception.message}")
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    if (queryResult == null) {
                        onResult(
                            buildLocalBillingResult(
                                responseCode = BillingClient.BillingResponseCode.ERROR,
                                debugMessage = "Failed to query member subscriptions"
                            ),
                            emptyList()
                        )
                    } else {
                        onResult(queryResult.billingResult, queryResult.purchasesList)
                    }
                }
            }
        }
    }

    /**
     * 构造仅供本地查询失败回调使用的 BillingResult，便于调用方沿用 Google responseCode 分支。
     * 该对象不是 Google 服务端真实返回值，debugMessage 也只用于定位本地连接/调用阶段。
     */
    private fun buildLocalBillingResult(responseCode: Int, debugMessage: String): BillingResult {
        return BillingResult.newBuilder()
            .setResponseCode(responseCode)
            .setDebugMessage(debugMessage)
            .build()
    }

    /**
     * 处理未消耗的订单
     * @param onConsumeOver 回调参数：all purchases、新验签成功的列表（空表示无新验签或全部失败）、是否全部验签成功
     */
    private fun handlePurchases(
        purchases: List<Purchase>?,
        onConsumeOver: ((
            all: List<Purchase>?,
            newlyVerified: List<Purchase>,
            verifyFailedList: List<Purchase>,
            allVerified: Boolean
        ) -> Unit)? = null
    ) {
        if (purchases.isNullOrEmpty()) {
            Logger.d(TAG, "No purchases found")
            onConsumeOver?.invoke(null, emptyList(), emptyList(), true)
            return
        }
        ioScope.launchNoCrash {
            var allVerified = true
            val newlyVerified = mutableListOf<Purchase>()
            val verifyFailedList = mutableListOf<Purchase>()
            purchases.forEach {
                if (it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!it.isAcknowledged) {
                        Logger.d(TAG, "验券调用：${it.purchaseState}======>${it}")
                        try {
                            val orderNo = it.accountIdentifiers?.obfuscatedAccountId
                            val productId = it.products.firstOrNull()
                            val response = ApiFactory.apiService.verifyPurchased(
                                VerifyPurchasedBody(
                                    it.purchaseToken,
                                    orderNo,
                                    productId
                                )
                            )
                            if (response.isSuccess) {
                                val priceParameters = productId?.let { id -> getPriceParameters(productCache[id]) }
                                // 1.3.8 统一后台上报af支付事件
//                                AppsFlyerManager.logPurchase(
//                                    productId = productId,
//                                    orderId = orderNo,
//                                    priceAmountMicros = priceParameters?.second,
//                                    currency = priceParameters?.third
//                                )
                            }
                            newlyVerified.add(it)
                        } catch (e: Exception) {
                            allVerified = false
                            verifyFailedList.add(it)
                            Logger.e(TAG, "验券失败: ${e.message}")
                            FirebaseCrashlytics.getInstance().recordException(e)
                        }
                    } else {
                        Logger.d(TAG, "已经验过券了：${it.purchaseState}======>${it}")
                    }
                } else {
                    Logger.d(TAG, "异常验券调用：${it.purchaseState}======>${it}")
                }
            }
            withContext(Dispatchers.Main) {
                onConsumeOver?.invoke(purchases, newlyVerified, verifyFailedList, allVerified)
            }
        }
    }

    /**
     * 订单异常状态上报
     * @param status 2-支付失败 3-取消订单"
     */
    fun reportOrderStatus(orderId: String?, status: Int, reason: String? = null) {
        this.orderId = null
        //订单状态 2-支付失败 3-取消订单"
        Logger.d(TAG, "订单 $orderId 状态上报: ${(if (status == 2) "支付失败" else "取消订单")}")
        ioScope.launchNoCrash {
            withContext(Dispatchers.IO) {
//                ApiFactory.apiService.reportOrderStatus(ReportOrderStatusBody(orderId, status, reason))
            }
        }
    }

    /**
     * 标记当前会员 Billing 流程已经由上层业务完成收敛。
     *
     * 允许调用的时机：
     * - PURCHASED 对应的会员 confirm/sync 已成功；
     * - USER_CANCELED 或明确启动失败后，prepare/release 已处理完；
     * - 不确定结果经 SUBS 查询确认没有有效交易，并已刷新服务端页面状态。
     *
     * PENDING、confirm 网络结果未知、ITEM_ALREADY_OWNED 尚未查询 SUBS 时禁止调用，否则会
     * 提前放开并发保护，使同一账号可能重复拉起购买。该方法只清理 Billing 回调路由上下文，
     * 不代表会员状态本身发生变化。
     */
    fun finishMemberBillingFlow() {
        if (isPurchasing.get()) {
            // Google 支付页仍在等待回调时清理路由标识，会让随后到达的 SUBS 回调误入 INAPP
            // 的进程恢复分支。除 KDoc 约束外再做运行时防御，提前调用时保持原上下文不变。
            Logger.d(TAG, "Cannot finish member billing while Google flow is active")
            return
        }
        clearMemberBillingContext()
        tryDisconnectAfterPurchase()
    }

    /**
     * 清理进程内会员回调路由标识。私有化实际写操作，确保所有清理点都遵循同一语义。
     */
    private fun clearMemberBillingContext() {
        activeMemberProductId = null
    }

    fun endConnection() {
        if (!isActive()) {
            if (isPurchasing.get()) {
                pendingDisconnect = true // 购买进行中，等购买结束后补偿断开
            } else {
                doDisconnect()
            }
        }
    }

    private fun doDisconnect() {
        pendingDisconnect = false
        billingClient?.endConnection()
        billingClient = null
        productCache.clear()
    }

    /**
     * 购买结束后检查是否需要补偿断开连接（用户在购买中途离开了所有页面时触发）
     */
    private fun tryDisconnectAfterPurchase() {
        if (pendingDisconnect && !isActive()) {
            doDisconnect()
        }
    }
}
