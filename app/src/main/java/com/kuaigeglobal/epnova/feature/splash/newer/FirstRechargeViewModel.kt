package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.network.apis.request.CreateOrderBody
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.core.response
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

    fun googlePay() {
        if (AppNavigator.checkLoginIntercepted()) return
        createOrder(data?.coionsConfigCode) {
            BillingManager.launchPurchase(
                topActivity,
                data?.googleProductId ?: "",
                it ?: "",
                onSuccess = {
                    refreshBalance()
                },
                onFail = { errorCode ->
                    if (errorCode != 3) {

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