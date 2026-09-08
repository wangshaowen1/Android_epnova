package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.annotation.ColorRes
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel

/** 暂无通知接口，先保留页面所需的展示字段。 */
data class FeedbackHistoryItem(
    val title: String,
    val content: String,
    val workTitle: String,
    val timeText: String,
    val state: String
)

class ItemFeedbackHistoryVm(feedbackHistoryItem: FeedbackHistoryItem) : AppItemViewModel<FeedbackHistoryItem>(feedbackHistoryItem){




    @ColorRes
    fun getStateTextColor(): Int {
        return  R.color.fixed_white_1
    }

}
