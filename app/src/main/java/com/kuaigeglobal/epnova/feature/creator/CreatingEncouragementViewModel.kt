package com.kuaigeglobal.epnova.feature.creator

import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.PopupClaimInfo
import com.kuaigeglobal.epnova.core.network.apis.request.PopupDisplayBody
import com.nhsd.basic.ext.getParcelableValue

/** 
 * @createtime 2026年09月08日 14:52
 * @author TooWhiteT
 */

class CreatingEncouragementViewModel : AppRepositoryViewModel<AppRepository>() {

    val data by lazy {
        mBundle.getParcelableValue(Constants.Extra.DATA, PopupClaimInfo::class.java)
    }

    private var displayReportStarted = false

    fun reportDisplay(onComplete: () -> Unit) {
        if (displayReportStarted) return
        displayReportStarted = true

        val popupDeliveryId = data?.popupDeliveryId
        if (popupDeliveryId.isNullOrBlank()) {
            displayReportStarted = false
            onComplete()
            return
        }

        requestWithLoading(
            apiBlock = {
                mRepo.api.confirmPopupDisplay(PopupDisplayBody(popupDeliveryId))
            },
            showLoading = false,
            showToastError = false,
            onError = { onComplete() },
            onSuccess = { onComplete() }
        )
    }

    fun reportDisplay() {
        if (displayReportStarted) return
        displayReportStarted = true

        val popupDeliveryId = data?.popupDeliveryId
        if (popupDeliveryId.isNullOrBlank()) {
            displayReportStarted = false
            return
        }

        request(
            apiBlock = {
                mRepo.api.confirmPopupDisplay(PopupDisplayBody(popupDeliveryId))
            },
            showToast = true,
            onSuccess = {},
            onError = {}
        )
    }
}
