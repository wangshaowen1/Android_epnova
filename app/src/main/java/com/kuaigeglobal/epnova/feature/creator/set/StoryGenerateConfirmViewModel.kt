package com.kuaigeglobal.epnova.feature.creator.set

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.showRechargeDialog
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch


class StoryGenerateConfirmViewModel : AppRepositoryViewModel<AppRepository>() {
    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(true) }
    val body by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, UploadStoryBody::class.java) }
    fun confirmPowerConsume(callback: (WorkTaskData?) -> Unit) {
        if (body == null) return
        launchWithLoading {
            val data = mRepo.api.novelUploadSummary(body!!)
            if (data.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                viewModelScope.launch {
                    showRechargeDialog(true)
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
}