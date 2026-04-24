package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.feature.recharge.PowerRepository
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.ext.getParcelableValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FirstRechargeViewModel : AppRepositoryViewModel<PowerRepository>() {

    val data by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, FirstRechargeDialogData::class.java) }
    val scene by lazy { mBundle.getInt(Constants.Extra.FROM_PAGE, FirstRechargeDialog.SCENE_OTHER) }

    val hourStr = MutableLiveData("00")
    val minuteStr = MutableLiveData("00")
    val secondStr = MutableLiveData("00")

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

    private fun startCountDown(expireTime: Long) {
        countDownJob?.cancel()
        countDownJob = viewModelScope.launch {
            while (true) {
//                val currentTime = System.currentTimeMillis()
//                val diffMs = expireTime - currentTime
                val diffMs = expireTime

                if (diffMs <= 0) {
                    // 倒计时结束
                    hourStr.value = "00"
                    minuteStr.value = "00"
                    secondStr.value = "00"

                    countDownJob?.cancel()
                    break
                }

                // 计算时、分、秒
                val totalSeconds = diffMs / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                // 格式化为两位数 (例如 9 -> "09")
                hourStr.value = String.format("%02d", hours)
                minuteStr.value = String.format("%02d", minutes)
                secondStr.value = String.format("%02d", seconds)

                delay(1000)
            }
        }
    }

    override fun onCleared() {
        countDownJob?.cancel()
        super.onCleared()
    }

    fun wxPay() {
        if (AppNavigator.checkLoginIntercepted()) return
//        val item = data?.pointConfigVO ?: return
//        if (isFromHome()) {
//            EventManager.onAppTrack(AppEvent.SF_CLICK_HOME_NEWCOMER_GIFTPACK_PAGE)
//        } else {
//            EventManager.onAppTrack(AppEvent.SF_CLICK_CREATE_NEWCOMER_GIFTPACK_PAGE)
//        }
//        pay(
//            PaymentClient.PaymentMethod.WxPay,
//            { paymentMethod, requestBody, responseSender ->
//                launchWithLoading {
//                    response(mRepo.createPowerOrder(paymentMethod, item.pointConfigCode, null)) {
//                        responseSender?.sendData(this.data?.toPaymentParams(paymentMethod))
//                    }
//                }
//            },
//            item,
//            { paymentData ->
//                if (isFromHome()) {
//                    EventManager.onAppTrack(AppEvent.SF_CLICK_HOME_NEWCOMER_GIFTPACK_PAYSUCCESS)
//                } else {
//                    EventManager.onAppTrack(AppEvent.SF_CLICK_CREATE_NEWCOMER_GIFTPACK_PAYSUCCESS)
//                }
//                refreshBalance()
//                dismiss()
//            },
//            { error, paymentData -> })
    }

    private fun refreshBalance() {
        Global.refreshBalance()
    }

}