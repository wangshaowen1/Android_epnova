package com.kuaigeglobal.epnova.feature.creator.set

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch


class StoryGenerateConfirmViewModel : AppRepositoryViewModel<AppRepository>() {
    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(true) }
    val body by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, UploadStoryBody::class.java) }
    val pointPowerDto by lazy { Config.get().appConfig.workPointPowerPopUpDto }
    fun confirmPowerConsume(callback: (WorkTaskData?) -> Unit) {
        if (body == null) return
        launchWithLoading {
            val data = mRepo.api.novelUploadSummary(body!!)
            if (
                data.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH ||
                data.code == ApiResponse.RESPONSE_CODE_POWER_ERROR
            ) {
                viewModelScope.launch {
                    PowerRechargeDialog.show(showToast = true, rechargeTab = PowerRechargeDialog.TAB_CREDITS)
                }
            } else if (data.isSuccess) {
                callback.invoke(data.data)
            } else {
                data.msg?.showToast()
            }
        }
    }

    fun checkPolicy() {
        checkedPolicy.set(checkedPolicy.get().not())
    }

    fun powerService() {
        AppRouter.jump(pointPowerDto?.jumpUri)
    }
}
