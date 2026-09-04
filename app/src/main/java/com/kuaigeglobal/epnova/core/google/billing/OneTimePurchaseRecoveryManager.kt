package com.kuaigeglobal.epnova.core.google.billing

import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.VerifyPurchasedBody
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 一次性商品的应用级恢复编排器。
 *
 * 页面销毁不会取消已取得凭证的验单；启动、前台化和充值页入口可以重复触发，但同一进程只会
 * 串行执行一组商店查询/验单。任何网络异常或业务失败都保留凭证，只有服务端明确成功才删除。
 * consume 由服务端负责，客户端恢复链路不调用 consumeAsync 或 acknowledgePurchase。
 */
object OneTimePurchaseRecoveryManager {

    private const val TAG = "OneTimePurchaseRecovery"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val operationMutex = Mutex()
    private val recovering = AtomicBoolean(false)
    private val completionCallbacks = mutableListOf<() -> Unit>()

    private val _settled = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 后台补偿成功信号；页面只需重新读取服务端余额，不携带订单号或 purchaseToken。 */
    val settled: SharedFlow<Unit> = _settled.asSharedFlow()

    fun isRecovering(): Boolean = recovering.get()

    /**
     * 扫描当前账号本地未完成购买。已有 token 直接重试验单；只有 PREPARED 记录才查询 Google。
     */
    fun recoverIfNeeded(onComplete: (() -> Unit)? = null) {
        scope.launch {
            onComplete?.let(completionCallbacks::add)
            if (!recovering.compareAndSet(false, true)) return@launch

            try {
                if (App.instance.isApiSecurityInit) {
                    operationMutex.withLock {
                        recoverCurrentUserPurchases()
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                // 恢复器自身异常也不能改变本地凭证；留待下一个生命周期入口重试。
                Logger.e(TAG, "Recovery failed: ${exception.javaClass.simpleName}")
            } finally {
                recovering.set(false)
                val callbacks = completionCallbacks.toList()
                completionCallbacks.clear()
                callbacks.forEach { callback -> runCatching { callback() } }
            }
        }
    }

    /**
     * 正常购买回调取得并落盘 token 后，在应用级作用域完成验单，避免页面关闭中断请求。
     */
    internal fun verifyCurrentPurchase(
        record: OneTimePurchaseRecord,
        onResult: (success: Boolean) -> Unit,
    ) {
        scope.launch {
            val success = try {
                operationMutex.withLock {
                    // 即使并发恢复刚刚处理过同一 token，也允许幂等重提，避免把存储读取失败
                    // 错判成验单成功；服务端承诺重复提交已发货 token 仍返回成功。
                    verifyRecord(record)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }

            if (success) {
                Global.refreshCoinsAndPoints()
                _settled.tryEmit(Unit)
            }
            onResult(success)
        }
    }

    private suspend fun recoverCurrentUserPurchases() {
        val initialRecords = OneTimePurchaseStore.getForCurrentUser()
        if (initialRecords.isEmpty()) return

        val attemptedTokens = mutableSetOf<String>()
        var settledAny = false
        initialRecords.forEach { record ->
            val purchaseToken = record.purchaseToken ?: return@forEach
            attemptedTokens += purchaseToken
            settledAny = verifyRecord(record) || settledAny
        }

        val hasPreparedRecord = OneTimePurchaseStore.getForCurrentUser()
            .any { record -> record.purchaseToken == null }
        if (hasPreparedRecord) {
            BillingManager.markActivePage(true)
            try {
                BillingManager.queryOneTimePurchasesForRecovery()?.let { purchases ->
                    OneTimePurchaseStore.capturePurchased(purchases)
                }
            } finally {
                BillingManager.markActivePage(false)
            }
        }

        OneTimePurchaseStore.getForCurrentUser().forEach { record ->
            val purchaseToken = record.purchaseToken ?: return@forEach
            if (!attemptedTokens.add(purchaseToken)) return@forEach
            settledAny = verifyRecord(record) || settledAny
        }

        if (settledAny) {
            Global.refreshCoinsAndPoints()
            _settled.tryEmit(Unit)
        }
    }

    private suspend fun verifyRecord(record: OneTimePurchaseRecord): Boolean {
        val purchaseToken = record.purchaseToken ?: return false
        if (!OneTimePurchaseStore.isForCurrentUser(record)) return false
        // 在主线程完成账号归属检查后立刻冻结请求体中的鉴权 token，避免 IO 切线程期间登录态
        // 切换后把原账号订单提交到新账号。
        val body = VerifyPurchasedBody(
            verifyData = purchaseToken,
            orderNo = record.orderNo,
            googleProductId = record.productId,
            appsFlyerId = Config.get().appsFlyerId,
        )
        val apiResponse = try {
            withContext(Dispatchers.IO) {
                when (record.businessType) {
                    OneTimePurchaseBusinessType.COINS -> {
                        ApiFactory.apiService.verifyGoogleCoinPurchase(body)
                    }

                    OneTimePurchaseBusinessType.CREDITS -> {
                        ApiFactory.apiService.verifyGoogleCreditsPurchase(body)
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // 网络结果未知时不改本地记录，等待下一个恢复入口幂等重试。
            return false
        }

        // 包含业务非 200、鉴权失败在内的所有错误都不是客户端可删除凭证的终态。
        if (!apiResponse.isSuccess) return false

        // 删除失败时保留记录，后续重复验单仍会由服务端幂等返回成功。
        OneTimePurchaseStore.removeAfterServerSuccess(record)
        return true
    }
}
