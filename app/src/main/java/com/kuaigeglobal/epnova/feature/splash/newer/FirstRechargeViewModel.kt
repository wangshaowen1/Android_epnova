package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.recharge.CoinRepository
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class FirstRechargeViewModel : AppRepositoryViewModel<CoinRepository>() {

    val data by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, FirstRechargeDialogData::class.java) }
    val scene by lazy { mBundle.getInt(Constants.Extra.FROM_PAGE, FirstRechargeDialog.SCENE_OTHER) }

    val hourStr = MutableLiveData("00")
    val minuteStr = MutableLiveData("00")
    val secondStr = MutableLiveData("00")

    /** 倒计时归零信号，由 Dialog 观察后自动关闭 */
    val countdownFinished = MutableLiveData(false)

    private val _purchaseEvent = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)

    /** 支付结果事件流，View 层订阅此 Flow 响应 UI 变化 */
    val purchaseEvent: SharedFlow<PurchaseResult> = _purchaseEvent.asSharedFlow()

    private var countDownJob: Job? = null
    private var purchaseJob: Job? = null

    fun isFromHome(): Boolean {
        return scene == FirstRechargeDialog.SCENE_HOME
    }

    fun isFromPlayer(): Boolean {
        return scene == FirstRechargeDialog.SCENE_PLAYER
    }

    init {
        BillingManager.markActivePage(true)
    }

    override fun onCreate() {
        super.onCreate()
        // 获取到期时间戳
        val invalidTime = data?.coinsMarketingTime ?: 0L
        if (invalidTime > 0) {
            startCountDown(invalidTime)
        }
    }

    private fun startCountDown(remainMs: Long) {
        countDownJob?.cancel()
        countDownJob = viewModelScope.launch {
            var remaining = remainMs
            while (remaining > 0) {
                val totalSeconds = remaining / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                hourStr.value = String.format("%02d", hours)
                minuteStr.value = String.format("%02d", minutes)
                secondStr.value = String.format("%02d", seconds)
                delay(1000)
                remaining -= 1000
            }
            // 归零：UI 保持显示 00:00:00，通知 Dialog 关闭
            hourStr.value = "00"
            minuteStr.value = "00"
            secondStr.value = "00"
            countdownFinished.value = true
        }
    }

    override fun onDestroy() {
        BillingManager.markActivePage(false)
        countDownJob?.cancel()
        purchaseJob?.cancel()
        super.onDestroy()
    }

    fun googlePay() {
        // pay_now_click：点击弹窗底部购买按钮时上报。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.FIRST_PURCHASE_COIN,
            FirebaseEvent.Action.PAY_NOW_CLICK
        )
        if (isFromPlayer()) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.PAID_EPISODE,
                FirebaseEvent.Action.BUY_COIN_CLICK,
                data?.coinsNum?.toString(),
            )
        }
        if (AppNavigator.checkLoginIntercepted()) return
        val rechargeData = data ?: return

        // 先订阅结果，再发起支付，确保不遗漏回调
        purchaseJob?.cancel()
        purchaseJob = viewModelScope.launch {
            BillingManager.purchaseResultFlow.take(1).collect { result ->
                val resultValue = if (result is PurchaseResult.Success) {
                    FirebaseEvent.Value.RESULT_SUCCESS
                } else {
                    FirebaseEvent.Value.RESULT_FAIL
                }
                // purchase_result：Label1=支付状态。
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.FIRST_PURCHASE_COIN,
                    FirebaseEvent.Action.PURCHASE_RESULT,
                    resultValue,
                )
                if (isFromPlayer()) {
                    EventManager.onFirebaseTrack(
                        FirebaseEvent.Category.PAID_EPISODE,
                        FirebaseEvent.Action.BUY_COIN_RESULT,
                        resultValue,
                        data?.coinsNum?.toString(),
                    )
                }
                if (result is PurchaseResult.Success) {
                    refreshCoinsAndPoints()
                } else if (result is PurchaseResult.Failed) {
                    R.string.first_recharge_payment_failed_retry.getResString().showToast()
                }
                _purchaseEvent.emit(result)
            }
        }

        launchWithLoading(
            block = {
                response(mRepo.createGoogleOrder(rechargeData.coinsConfigCode)) {
                    BillingManager.launchPurchase(topActivity, rechargeData.googleProductId ?: "", data?.orderNo ?: "")
                }
            },
            onError = { e ->
                purchaseJob?.cancel()
                parseMessage(e)?.showToast()
            }
        )
    }

    private fun refreshCoinsAndPoints() {
        Global.refreshCoinsAndPoints()
    }

}
