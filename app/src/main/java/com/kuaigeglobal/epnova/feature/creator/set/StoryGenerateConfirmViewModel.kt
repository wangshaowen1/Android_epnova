package com.kuaigeglobal.epnova.feature.creator.set

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.ext.handlePointInsufficientError
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.CreationScene
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast


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
            val handled = handlePointInsufficientError(
                creationScene = CreationScene.STORY_OUTLINE,
                errorCode = data.code,
                allowFirstRechargeForLegacyGuide = false,
            )
            if (handled) return@launchWithLoading
            if (data.isSuccess) {
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
