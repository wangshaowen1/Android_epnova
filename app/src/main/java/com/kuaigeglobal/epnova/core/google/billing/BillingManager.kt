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

    /**
     * 预下单的订单号
     */
    var orderId: String? = null

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

    private fun createBillingClient(): BillingClient {
        billingClient?.let { return it }

        return BillingClient.newBuilder(App.instance)
            .setListener { billingResult, purchases ->
                isPurchasing.set(false)
                if (orderId == null) {
                    // 进程被杀重启场景：orderId 已丢失，但 Google 仍会推送 pending purchases
                    // 若 responseCode == OK 且有 purchases，直接验签并 Toast 提示；UI 回调依赖后续 queryPendingPurchases 补偿
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
                        handlePurchases(purchases) { _, _, _, allVerified ->
                            if (allVerified) {
                                App.instance.getString(R.string.system_payment_success).showToast()
                                // 进程重启场景：无 ViewModel 订阅，emit 会被自动丢弃，仅靠 Toast 提示
                                _purchaseResult.tryEmit(PurchaseResult.Success(purchases.firstOrNull()))
                            }
                        }
                    } else {
                        queryPendingPurchases()
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
                            "Google购买异常===>PurchasesUpdatedListener: $billingResult , purchase:${purchases?.getOrNull(0)} order=$currentOrderId userId=${User.get().id}"
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
                            "Google购买失败：PurchasesUpdatedListener: $billingResult , purchase:${purchases?.getOrNull(0)} order=$currentOrderId  userId=${User.get().id}"
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
                    isPurchasing.set(false) // 断线时购买流程已中断，允许重新发起
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
     * 发起购买。结果通过 [purchaseResultFlow] 下发，调用方需在此之前订阅。
     * @param productId google商品ID
     * @param orderId 自建预订单ID
     * @param productType 商品类型（1-一次性，2-订阅）
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
        connectIfNeeded { state ->
            // orderId != null 说明上一笔 Google 回调还未结清（可能断线重连后 isPurchasing 已被 reset），禁止覆盖
            if (isPurchasing.get() || this.orderId != null || state == 1 || state == -1) {
                return@connectIfNeeded
            }
            isPurchasing.set(true)
            launchFlow(
                activity,
                productId,
                orderId,
                if (productType == 2) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
            )
        }
    }

    /**
     * 购买获取商品详情
     */
    private fun launchFlow(activity: Activity, productId: String, orderId: String, productType: String) {
        val details = productCache[productId]
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
     * 购买调起谷歌界面
     */
    private fun launchBillingFlow(activity: Activity, details: ProductDetails, orderId: String) {
        val billingFlowParams = if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .setObfuscatedAccountId(orderId)
                .setObfuscatedProfileId(User.get().userId ?: "")
                .setIsOfferPersonalized(true) // 面向欧美市场的GDPR设置
                .build()
        } else {
            BillingFlowParams.newBuilder()
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
        }
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

    // 查询历史购买记录，newlyVerified 为本次新验签成功的购买列表，可用于后台补偿场景的提示
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
                    val inAppPurchases = billingClient?.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )?.purchasesList ?: listOf()
                    val subsPurchases = billingClient?.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )?.purchasesList ?: listOf()
                    inAppPurchases + subsPurchases
                }
                withContext(Dispatchers.Main) {
                    handlePurchases(purchases) { list, newlyVerified, verifyFailedList, allVerified ->
                        onConsumeOver?.invoke(list, newlyVerified, verifyFailedList, allVerified)
                    }
                }
            }
        }
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
                                AppsFlyerManager.logPurchase(
                                    productId = productId,
                                    orderId = orderNo,
                                    priceAmountMicros = priceParameters?.second,
                                    currency = priceParameters?.third
                                )
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
