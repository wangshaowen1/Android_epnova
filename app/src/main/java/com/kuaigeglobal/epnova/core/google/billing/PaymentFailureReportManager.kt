package com.kuaigeglobal.epnova.core.google.billing

import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.PaymentFailureReasonEnum
import com.kuaigeglobal.epnova.core.network.apis.request.PaymentFailureReportBody
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 支付失败事件的应用级、非阻塞上报器。
 *
 * 调用方只负责提交一次失败事实；本对象先落盘，再在应用级协程中静默发送。网络异常、服务端
 * 500、限流和鉴权过期都保留同一个 clientEventId，下一次前台化或新事件到达时继续重试。
 */
object PaymentFailureReportManager {

    private const val TAG = "PaymentFailureReportManager"
    private const val MAX_ATTEMPTS_PER_TRIGGER = 3
    /** 服务端限制为每用户 10 次/分钟，队列请求间隔按 6 秒留出余量。 */
    private const val RETRY_DELAY_MS = 6_000L
    private const val REQUEST_INTERVAL_MS = 6_000L

    private val drainMutex = Mutex()
    private val draining = AtomicBoolean(false)

    /**
     * 记录一笔商店侧失败。返回 false 只表示当前账号未初始化或本地队列写入失败，绝不影响
     * 原支付流程的失败收敛。
     */
    fun report(
        orderNo: String?,
        reasonCode: PaymentFailureReasonEnum,
        platformErrorCode: String? = null,
    ): Boolean {
        val ownerUserId = currentOwnerUserId() ?: return false
        val normalizedOrderNo = orderNo
            ?.takeIf { it.isNotBlank() && it.length <= 32 }
            ?: return false
        val normalizedPlatformErrorCode = platformErrorCode
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it.length <= 128 && it.matches(Regex("[A-Za-z0-9_.-]+")) }

        val record = PaymentFailureReportRecord(
            ownerUserId = ownerUserId,
            orderNo = normalizedOrderNo,
            clientEventId = UUID.randomUUID().toString().lowercase(Locale.US),
            failReasonCode = reasonCode.name,
            platformErrorCode = normalizedPlatformErrorCode,
            createdAt = System.currentTimeMillis(),
        )
        val saved = PaymentFailureReportStore.enqueue(record)
        if (saved) drainPending()
        return saved
    }

    /** 在登录完成、应用前台化或网络恢复后调用，发送当前账号尚未完成的事件。 */
    fun drainPending() {
        ioScope.launch {
            if (!draining.compareAndSet(false, true)) return@launch
            try {
                drainMutex.withLock {
                    repeat(MAX_ATTEMPTS_PER_TRIGGER) { attempt ->
                        if (!App.instance.isApiSecurityInit) return@withLock
                        val pending = PaymentFailureReportStore.getForCurrentUser()
                        if (pending.isEmpty()) return@withLock

                        var shouldRetry = false
                        for ((index, record) in pending.withIndex()) {
                            when (send(record)) {
                                SendResult.SENT,
                                SendResult.DROP -> {
                                    PaymentFailureReportStore.remove(record)
                                }

                                SendResult.RETRY -> {
                                    shouldRetry = true
                                    break
                                }

                                SendResult.PAUSE -> {
                                    // 401 需要等待登录态恢复；本次不继续撞接口。
                                    return@withLock
                                }
                            }
                            if (index < pending.lastIndex) delay(REQUEST_INTERVAL_MS)
                        }

                        if (!shouldRetry || attempt == MAX_ATTEMPTS_PER_TRIGGER - 1) {
                            return@withLock
                        }
                        delay(RETRY_DELAY_MS)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                // 上报器异常不能反向影响支付链路；事件仍保留在本地队列。
                Logger.e(TAG, "drain failed: ${exception.javaClass.simpleName}")
            } finally {
                draining.set(false)
            }
        }
    }

    private suspend fun send(record: PaymentFailureReportRecord): SendResult {
        // 账号可能在队列快照后切换；绝不能用新账号的鉴权头发送旧账号订单。
        if (currentOwnerUserId() != record.ownerUserId) return SendResult.PAUSE
        val response = try {
            ApiFactory.apiService.reportPaymentFailure(
                PaymentFailureReportBody(
                    orderNo = record.orderNo,
                    clientEventId = record.clientEventId,
                    failReasonCode = record.failReasonCode,
                    platformErrorCode = record.platformErrorCode,
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Logger.e(TAG, "request failed: ${exception.javaClass.simpleName}")
            return SendResult.RETRY
        }

        return when {
            response.isSuccess -> SendResult.SENT
            response.code == CODE_AUTH_EXPIRED -> SendResult.PAUSE
            response.code == CODE_INVALID_REQUEST || response.code == CODE_ORDER_NOT_FOUND -> {
                // 客户端请求已被服务端判定为不可修复；继续重试只会制造噪声。
                SendResult.DROP
            }
            else -> SendResult.RETRY
        }
    }

    private fun currentOwnerUserId(): String? {
        return User.get().userId?.takeIf { it.isNotBlank() }
            ?: User.get().userUid?.takeIf { it.isNotBlank() }
    }

    private enum class SendResult {
        SENT,
        RETRY,
        DROP,
        PAUSE,
    }

    private const val CODE_AUTH_EXPIRED = 401
    private const val CODE_INVALID_REQUEST = 3001
    private const val CODE_ORDER_NOT_FOUND = 300138
}
