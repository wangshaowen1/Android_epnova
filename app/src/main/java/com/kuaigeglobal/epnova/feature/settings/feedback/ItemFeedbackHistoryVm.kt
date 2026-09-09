package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.annotation.ColorRes
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackDetail

class ItemFeedbackHistoryVm(feedbackDetail: FeedbackDetail) :
    AppItemViewModel<FeedbackDetail>(feedbackDetail) {

    @ColorRes
    fun stateTextColor(): Int {
        return when (data.get()?.status) {
            FeedbackViewModel.STATUS_IN_PROGRESS -> R.color.color_ea9800
            FeedbackViewModel.STATUS_COMPLETED -> R.color.color_00cf18
            else -> R.color.color_3c66ff
        }
    }
}
