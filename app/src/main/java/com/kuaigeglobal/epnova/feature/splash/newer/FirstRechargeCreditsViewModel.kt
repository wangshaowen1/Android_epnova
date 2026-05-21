package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeCreditsDialogData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.recharge.CoinRepository
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class FirstRechargeCreditsViewModel : AppRepositoryViewModel<CoinRepository>() {

    val data by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, FirstRechargeCreditsDialogData::class.java) }
    val scene by lazy { mBundle.getInt(Constants.Extra.FROM_PAGE, FirstRechargeCreditsDialog.SCENE_OTHER) }

    val hourStr = MutableLiveData("00")
    val minuteStr = MutableLiveData("00")
    val secondStr = MutableLiveData("00")

    val currentPriceText = ObservableField(data?.basicPrice.orEmpty())
    val payPriceText = ObservableField(data?.pricePointText.ifNotBlank() ?: data?.basicPrice.orEmpty())

    val titleText: String
        get() = data?.giftTag.ifNotBlank() ?: R.string.newbie_new_user_exclusive_gift.getResString()

    val currentCreditsText: String
        get() = data?.basicPoint.ifNotBlank() ?: "0"

    val exclusiveCreditsText: String
        get() = data?.giveTotalPoint.ifNotBlank() ?: "0"

    val exclusiveBonusText: String
        get() = data?.extraGiftText.ifNotBlank() ?: R.string.newbie_credits_bonus_detail.getResString(
            data?.basicPoint.ifNotBlank() ?: "0",
            data?.givePoint.ifNotBlank() ?: "0"
        )

    /** 倒计时归零后通知 Dialog 关闭。 */
    val countdownFinished = MutableLiveData(false)

    private val _purchaseEvent = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)
    val purchaseEvent: SharedFlow<PurchaseResult> = _purchaseEvent.asSharedFlow()

    private var countDownJob: Job? = null
    private var purchaseJob: Job? = null

    init {
        BillingManager.markActivePage(true)
    }

    override fun onCreate() {
        super.onCreate()
        val remainingTime = getRemainingTime(data?.popupInvalidTime)
        if (remainingTime > 0) {
            startCountDown(remainingTime)
        }
    }

    fun isFromHome(): Boolean {
        return scene == FirstRechargeCreditsDialog.SCENE_HOME
    }

    fun updateGooglePrice(priceText: String?) {
        val text = priceText.ifNotBlank() ?: return
        currentPriceText.set(text)
        payPriceText.set(text)
    }

    fun googlePay() {
        if (AppNavigator.checkLoginIntercepted()) return
        val pointConfig = data?.pointConfigVO ?: return

        purchaseJob?.cancel()
        purchaseJob = viewModelScope.launch {
            BillingManager.purchaseResultFlow.take(1).collect { result ->
                if (result is PurchaseResult.Success) {
                    refreshBalance()
                }
                _purchaseEvent.emit(result)
            }
        }

        launchWithLoading(
            block = {
                response(mRepo.createGoogleOrder(pointConfig.pointConfigCode)) {
                    BillingManager.launchPurchase(topActivity, pointConfig.googleProductId ?: "", data?.orderNo ?: "")
                }
            },
            onError = {
                purchaseJob?.cancel()
            }
        )
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
            hourStr.value = "00"
            minuteStr.value = "00"
            secondStr.value = "00"
            countdownFinished.value = true
        }
    }

    private fun getRemainingTime(time: Long?): Long {
        if (time == null || time <= 0) return 0L
        val now = System.currentTimeMillis()
        return if (time > now) time - now else time
    }

    private fun refreshBalance() {
        Global.refreshBalance()
    }

    override fun onCleared() {
        BillingManager.markActivePage(false)
        countDownJob?.cancel()
        purchaseJob?.cancel()
        super.onCleared()
    }

    private fun String?.ifNotBlank(): String? {
        return this?.takeIf { it.isNotBlank() }
    }
}
