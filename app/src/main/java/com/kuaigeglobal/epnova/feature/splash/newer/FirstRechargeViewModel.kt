package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.network.apis.request.CreateOrderBody
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.recharge.CoinRepository
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FirstRechargeViewModel : AppRepositoryViewModel<CoinRepository>() {

    val data by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, FirstRechargeDialogData::class.java) }
    val scene by lazy { mBundle.getInt(Constants.Extra.FROM_PAGE, FirstRechargeDialog.SCENE_OTHER) }

    val hourStr = MutableLiveData("00")
    val minuteStr = MutableLiveData("00")
    val secondStr = MutableLiveData("00")

    /** 倒计时归零信号，由 Dialog 观察后自动关闭 */
    val countdownFinished = MutableLiveData(false)

    /** 支付成功回调，由外部（Dialog）注入 */
    var onPaySuccess: (() -> Unit)? = null

    private var countDownJob: Job? = null

    fun isFromHome(): Boolean {
        return scene == FirstRechargeDialog.SCENE_HOME
    }

    override fun onCreate() {
        super.onCreate()
        // 获取到期时间戳
        val invalidTime = data?.coionsMarketingTime ?: 0L
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
        countDownJob?.cancel()
        super.onCleared()
    }

    fun googlePay() {
        if (AppNavigator.checkLoginIntercepted()) return
        createOrder(data?.coionsConfigCode) {
            BillingManager.launchPurchase(
                topActivity,
                data?.googleProductId ?: "",
                it ?: "",
                onSuccess = {
                    refreshBalance()
//                    showToast(R.string.payment_successful)
                    onPaySuccess?.invoke()
                },
                onFail = { errorCode ->
                    if (errorCode == 3) {
//                        showToast(R.string.payment_cancelled)
                    } else {
//                        showToast(R.string.payment_failed_retry)
                    }
                }
            )
        }
    }

    fun createOrder(
        code: String?,
        onSuccess: ((String?) -> Unit)? = null
    ) {
        launchWithLoading {
            response(
                mRepo.api.createOrder(
                    CreateOrderBody(
                        goodsCode = code,
                        goodsType = 1,
                        mobileType = 1
                    )
                )
            ) {
                onSuccess?.invoke(data?.orderNo)
            }
        }
    }

    private fun refreshBalance() {
        Global.refreshBalance()
    }

}