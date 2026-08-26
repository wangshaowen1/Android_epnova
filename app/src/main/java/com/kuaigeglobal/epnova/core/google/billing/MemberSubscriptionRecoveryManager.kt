package com.kuaigeglobal.epnova.core.google.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.GoogleMemberSubscriptionBody
import com.kuaigeglobal.epnova.core.response
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 会员订阅的进程级恢复编排器。
 *
 * 页面内的正常支付仍由 MemberIndexViewModel 持有完整 prepare 上下文并执行 confirm；本对象
 * 只处理“进程已重启、页面上下文已经丢失”的补偿场景：
 * 1. 当前 EpNova 账号存在 [MemberSubscriptionRecoveryStore] 标记时才查询 Google SUBS；
 * 2. 只接受同时匹配 productId 与 obfuscatedAccountId 的官方购买；
 * 3. PURCHASED 使用后端幂等 sync 收敛，PENDING/未知结果继续保留恢复标记；
 * 4. 不持久化 purchaseToken，也不会凭空构造已经丢失的 prepare token 或 basePlanId。
 *
 * 使用 Application 生命周期的协程域，是为了避免首页 Activity 重建时中断已经取得的商店事实
 * 或 sync 请求。一次进程内只允许一个恢复任务，查询失败最多重试一次，之后等待会员页前台化
 * 或下次进程启动，避免后台形成无界轮询。
 */
object MemberSubscriptionRecoveryManager {

    private const val QUERY_RETRY_DELAY_MS = 1_500L
    private const val QUERY_MAX_ATTEMPTS = 2

    /** 官方凭证交叉校验失败及未知错误都不是客户端可判定终态，必须保留标记。 */
    private const val MEMBER_ERROR_PURCHASE_CROSS_CHECK_FAILED = 300185

    /**
     * 文档定义的稳定业务终态。出现这些结果时不再用 sync 覆盖后端判断，只结束本地恢复并
     * 让页面重新读取 plan/list；300185 不在其中，它需要保留标记等待再次查询商店。
     */
    private val MEMBER_TERMINAL_BUSINESS_ERRORS = setOf(
        300181,
        300182,
        300184,
        300186,
        300189,
        300191,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recovering = AtomicBoolean(false)

    /**
     * 只通知“恢复流程已经得到可结束的结果”。会员页收到通知后重新读取后端页面状态，不把
     * sync 响应直接当作本地会员状态。无订阅者时事件允许丢弃，因为后续新建页面本就会加载。
     */
    private val _settled = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val settled: SharedFlow<Unit> = _settled.asSharedFlow()

    /** 当前任务是否已经占有一份 Billing 使用计数，必须在每个退出分支中成对释放。 */
    private var billingUsageHeld = false

    fun isRecovering(): Boolean = recovering.get()

    /**
     * 尝试恢复上次未收敛的会员订阅。没有当前账号标记时立即返回，不会查询普通用户的 SUBS。
     */
    fun recoverIfNeeded() {
        if (!recovering.compareAndSet(false, true)) return

        scope.launch {
            val signal = MemberSubscriptionRecoveryStore.getForCurrentUser()
            if (signal == null) {
                recovering.set(false)
                return@launch
            }

            try {
                // 进程恢复没有会员页面替我们维持 Billing 连接，因此任务期间单独持有一次计数。
                BillingManager.markActivePage(true)
                billingUsageHeld = true
                queryMemberSubscriptions(signal, attempt = 1)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // 启动阶段的本地异常不能被解释为“没有交易”；保留标记等待下一次恢复机会。
                finishAttemptKeepingMarker()
            }
        }
    }

    private fun queryMemberSubscriptions(
        signal: MemberSubscriptionRecoveryStore.Signal,
        attempt: Int,
    ) {
        if (!isCurrentSignal(signal)) {
            finishAttemptKeepingMarker()
            return
        }

        // INAPP 购买或另一个会员商品占用全局 Billing 路由时不能覆盖上下文；先做一次有界重试。
        if (!BillingManager.restoreMemberBillingContext(signal.productId)) {
            retryQueryOrFinish(signal, attempt)
            return
        }

        BillingManager.queryMemberSubscriptions queryResult@{ billingResult, purchases ->
            if (!recovering.get() || !isCurrentSignal(signal)) {
                finishAttemptKeepingMarker()
                return@queryResult
            }

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                // 连接失败或商店查询异常都无法证明交易不存在，不清标记、不 release。
                retryQueryOrFinish(signal, attempt)
                return@queryResult
            }

            // 查询事实已经替代可能丢失的 Billing 回调，可释放 SDK 的瞬时购买等待位；商品路由
            // 仍保留到 sync/无交易终态真正完成，防止并发创建另一笔 prepare。
            BillingManager.markMemberSubscriptionQueryResolved(signal.productId)
            handleQueryResult(signal, purchases, attempt)
        }
    }

    private fun retryQueryOrFinish(
        signal: MemberSubscriptionRecoveryStore.Signal,
        attempt: Int,
    ) {
        if (attempt >= QUERY_MAX_ATTEMPTS) {
            finishAttemptKeepingMarker()
            return
        }

        scheduleQuery(signal, attempt + 1)
    }

    private fun scheduleQuery(
        signal: MemberSubscriptionRecoveryStore.Signal,
        nextAttempt: Int,
    ) {
        scope.launch {
            delay(QUERY_RETRY_DELAY_MS)
            if (recovering.get() && isCurrentSignal(signal)) {
                queryMemberSubscriptions(signal, nextAttempt)
            } else {
                finishAttemptKeepingMarker()
            }
        }
    }

    private fun handleQueryResult(
        signal: MemberSubscriptionRecoveryStore.Signal,
        purchases: List<Purchase>,
        attempt: Int,
    ) {
        val matchingPurchases = purchases
            .filter(signal::matches)
            .distinctBy { purchase -> purchase.purchaseToken }

        if (matchingPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            // PENDING 是明确的商店状态，但不是支付终态。保留恢复标记和会员商品路由，等待
            // Google 后续更新；下次页面前台化或进程启动会再次查询。
            finishAttemptKeepingMarker()
            return
        }

        val purchased = matchingPurchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (purchased.isNotEmpty()) {
            syncNextPurchase(signal, purchased, index = 0)
            return
        }

        if (matchingPurchases.isNotEmpty()) {
            // UNSPECIFIED_STATE 同样不能反推未成交，保留标记而不是放开重复购买。
            finishAttemptKeepingMarker()
            return
        }

        if (attempt < QUERY_MAX_ATTEMPTS) {
            // 标记在拉起 Google 页面前就会创建，进程也可能恰好在商店回调前被杀。第一次空
            // 结果给 Play 缓存一个短暂宽限期，再确认一次，避免过早清标记后漏掉刚成交交易。
            scheduleQuery(signal, attempt + 1)
            return
        }

        // 两次成功查询都没有当前账号、当前商品的有效交易，进程恢复又没有旧 prepare token
        // 可供 release。结束本地等待并刷新后端状态，prepare 锁由服务端超时机制自行释放。
        settleCurrentSignal(signal)
    }

    /**
     * Google 可能返回同商品、同账号的多笔当前购买，逐笔提交真实凭证，由后端永久幂等；
     * purchaseToken 只存在于当前调用栈，不写入 MMKV、普通日志或事件对象。
     */
    private fun syncNextPurchase(
        signal: MemberSubscriptionRecoveryStore.Signal,
        purchases: List<Purchase>,
        index: Int,
    ) {
        if (!recovering.get() || !isCurrentSignal(signal)) {
            finishAttemptKeepingMarker()
            return
        }
        if (index >= purchases.size) {
            settleCurrentSignal(signal)
            return
        }

        val purchase = purchases[index]
        scope.launch {
            val apiResponse = try {
                withContext(Dispatchers.IO) {
                    ApiFactory.apiService.syncGoogleMemberSubscription(
                        GoogleMemberSubscriptionBody(
                            purchaseToken = purchase.purchaseToken,
                            productId = signal.productId,
                            // 进程重启后 prepare 上下文已经丢失；sync 必须由后端根据 Google
                            // 官方凭证恢复，不能复用或猜测历史 prepare/base plan。
                            purchasePrepareToken = null,
                            basePlanId = null,
                            appsFlyerId = Config.get().appsFlyerId
                        )
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // 网络结果未知时保留标记；下一次恢复会重新查询 Google 再幂等 sync。
                finishAttemptKeepingMarker()
                return@launch
            }

            if (!recovering.get() || !isCurrentSignal(signal)) {
                finishAttemptKeepingMarker()
                return@launch
            }

            response(
                bean = apiResponse,
                result = {
                    syncNextPurchase(signal, purchases, index + 1)
                },
                error = {
                    val errorCode = this?.code
                    when {
                        errorCode?.let(MEMBER_TERMINAL_BUSINESS_ERRORS::contains) == true -> {
                            settleCurrentSignal(signal)
                        }

                        errorCode == MEMBER_ERROR_PURCHASE_CROSS_CHECK_FAILED -> {
                            finishAttemptKeepingMarker()
                        }

                        // 网络、鉴权及未登记业务 code 都不是安全终态，保留标记等待再次对账。
                        else -> finishAttemptKeepingMarker()
                    }
                },
            )
        }
    }

    /** 清理当前账号同一恢复信号并通知可见会员页重新读取后端状态。 */
    private fun settleCurrentSignal(signal: MemberSubscriptionRecoveryStore.Signal) {
        if (!isCurrentSignal(signal)) {
            finishAttemptKeepingMarker()
            return
        }

        MemberSubscriptionRecoveryStore.clearForCurrentUser()
        BillingManager.finishMemberBillingFlow()
        _settled.tryEmit(Unit)
        Global.refreshCoinsAndPoints()
        finishAttempt()
    }

    /** 保留标记及会员商品路由，只结束本次有界查询/sync 尝试。 */
    private fun finishAttemptKeepingMarker() {
        finishAttempt()
    }

    private fun finishAttempt() {
        if (billingUsageHeld) {
            billingUsageHeld = false
            BillingManager.markActivePage(false)
        }
        recovering.set(false)
    }

    private fun isCurrentSignal(signal: MemberSubscriptionRecoveryStore.Signal): Boolean {
        return MemberSubscriptionRecoveryStore.getForCurrentUser()?.hasSameTarget(signal) == true
    }
}
