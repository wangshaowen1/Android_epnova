package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.google.billing.OneTimePurchaseBusinessType
import com.kuaigeglobal.epnova.core.google.billing.OneTimePurchaseRecoveryManager
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeCreditsDialogData
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

class FirstRechargeCreditsViewModel : AppRepositoryViewModel<CoinRepository>() {

    val data by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, FirstRechargeCreditsDialogData::class.java) }
    val scene by lazy { mBundle.getInt(Constants.Extra.FROM_PAGE, FirstRechargeCreditsDialog.SCENE_OTHER) }

    val hourStr = MutableLiveData("00")
    val minuteStr = MutableLiveData("00")
    val secondStr = MutableLiveData("00")

    private val pointConfig get() = data?.pointConfigVO

    val currentPriceText = ObservableField("")
    val payPriceText = ObservableField("")
    val currentCreditsText = ObservableField("0")
    val exclusiveCreditsText = ObservableField("0")
    val exclusiveBonusText = ObservableField("")

    val payProductId: String?
        get() = pointConfig?.googleProductId.ifNotBlank()

    /** 倒计时归零后通知 Dialog 关闭。 */
    val countdownFinished = MutableLiveData(false)

    private val _purchaseEvent = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)
    val purchaseEvent: SharedFlow<PurchaseResult> = _purchaseEvent.asSharedFlow()

    private var countDownJob: Job? = null
    private var purchaseJob: Job? = null

    init {
        BillingManager.markActivePage(true)
        OneTimePurchaseRecoveryManager.recoverIfNeeded()
    }

    override fun onCreate() {
        super.onCreate()
        refreshDisplayData()
        val remainingTime = data?.popupInvalidTime?.let { getRemainingTime(it) }
            ?: (pointConfig?.pointMarketingTime ?: 0L)
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

    private fun refreshDisplayData() {
        val priceText = getPriceText()
        currentPriceText.set(priceText)
        payPriceText.set(priceText)
        currentCreditsText.set(getCurrentPointText())
        exclusiveCreditsText.set(getTotalPointText())
        exclusiveBonusText.set(getBonusText())
    }

    fun googlePay() {
        // pay_now_click：点击弹窗底部购买按钮时上报。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.FIRST_PURCHASE_CREDITS,
            FirebaseEvent.Action.PAY_NOW_CLICK
        )
        if (AppNavigator.checkLoginIntercepted()) return
        val pointConfig = data?.pointConfigVO ?: return

        purchaseJob?.cancel()
        purchaseJob = viewModelScope.launch {
            BillingManager.purchaseResultFlow.take(1).collect { result ->
                if (result != PurchaseResult.Pending) {
                    // purchase_result：Label1=支付状态。
                    EventManager.onFirebaseTrack(
                        FirebaseEvent.Category.FIRST_PURCHASE_CREDITS,
                        FirebaseEvent.Action.PURCHASE_RESULT,
                        if (result is PurchaseResult.Success) {
                            FirebaseEvent.Value.RESULT_SUCCESS
                        } else {
                            FirebaseEvent.Value.RESULT_FAIL
                        }
                    )
                }
                _purchaseEvent.emit(result)
            }
        }

        launchWithLoading(
            block = {
                response(mRepo.createGoogleCreditsPreOrder(pointConfig.pointConfigCode)) {
                    val orderData = this.data
                    BillingManager.launchPurchase(
                        activity = topActivity,
                        productId = orderData?.productId.orEmpty()
                            .ifBlank { pointConfig.googleProductId.orEmpty() },
                        orderId = orderData?.orderNo.orEmpty(),
                        businessType = OneTimePurchaseBusinessType.CREDITS,
                        obfuscatedAccountId = orderData?.obfuscatedAccountId.orEmpty()
                    )
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
        return if (time > now) time - now else 0L
    }

    private fun getPriceText(): String {
        return pointConfig?.pointPrice ?: ""
    }

    private fun getBonusText(): String {
        val pointText = pointConfig?.pointNum?.toString()
        val marketingText = pointConfig?.pointMarketingText
        val tagText = pointConfig?.pointMarketingTag
        return "${pointText}${marketingText} $tagText"
    }

    private fun getCurrentPointText(): String {
        return pointConfig?.pointNum?.toString() ?: "0"
    }

    private fun getTotalPointText(): String {
        val num = pointConfig?.pointNum ?: 0
        val give = pointConfig?.givePointNum ?: 0
        return (num + give).toString()
    }

    override fun onDestroy() {
        BillingManager.markActivePage(false)
        countDownJob?.cancel()
        purchaseJob?.cancel()
        super.onDestroy()
    }

    private fun String?.ifNotBlank(): String? {
        return this?.takeIf { it.isNotBlank() }
    }
}
