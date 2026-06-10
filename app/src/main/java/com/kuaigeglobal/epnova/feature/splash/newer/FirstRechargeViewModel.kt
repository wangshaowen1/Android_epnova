package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.recharge.CoinRepository
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.ext.getParcelableValue
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

    override fun onCleared() {
        BillingManager.markActivePage(false)
        countDownJob?.cancel()
        purchaseJob?.cancel()
        super.onCleared()
    }

    fun googlePay() {
        if (AppNavigator.checkLoginIntercepted()) return
        val rechargeData = data ?: return
        EventManager.onFirebaseTrack(FirebaseEvent.COINS_FIRST_PURCHASE_GIFT_POPUP_CLICK)

        // 先订阅结果，再发起支付，确保不遗漏回调
        purchaseJob?.cancel()
        purchaseJob = viewModelScope.launch {
            BillingManager.purchaseResultFlow.take(1).collect { result ->
                if (result is PurchaseResult.Success) {
                    EventManager.onFirebaseTrack(FirebaseEvent.COINS_FIRST_PURCHASE_GIFT_POPUP_SUCCESS)
                    refreshCoinsAndPoints()
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
            onError = {
                purchaseJob?.cancel()
            }
        )
    }

    private fun refreshCoinsAndPoints() {
        Global.refreshCoinsAndPoints()
    }

}
