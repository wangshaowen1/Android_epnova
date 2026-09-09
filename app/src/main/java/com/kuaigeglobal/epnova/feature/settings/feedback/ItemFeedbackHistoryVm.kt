package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.annotation.ColorRes
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel

data class FeedbackHistoryItem(
    val id: String,
    val type: String,
    val submittedOn: String,
    val description: String,
    val status: String,
    val submittedAtMillis: Long,
    val imageUrls: List<String> = emptyList()
)

data class FeedbackDetail(
    val id: String,
    val type: String,
    val submittedOn: String,
    val description: String,
    val imageUrls: List<String>,
    val status: String,
    val reply: String? = null,
    val replyOn: String? = null,
    val replyImageUrls: List<String> = emptyList()
)

class ItemFeedbackHistoryVm(feedbackHistoryItem: FeedbackHistoryItem) :
    AppItemViewModel<FeedbackHistoryItem>(feedbackHistoryItem) {

    @ColorRes
    fun stateTextColor(): Int {
        return when (data.get()?.status) {
            FeedbackViewModel.STATUS_IN_PROGRESS -> R.color.color_ea9800
            FeedbackViewModel.STATUS_COMPLETED -> R.color.color_00cf18
            else -> R.color.color_3c66ff
        }
    }
}
