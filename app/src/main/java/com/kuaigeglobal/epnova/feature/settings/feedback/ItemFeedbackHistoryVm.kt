package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.annotation.ColorRes
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackPageInfo

class ItemFeedbackHistoryVm(feedback: FeedbackPageInfo) :
    AppItemViewModel<FeedbackPageInfo>(feedback) {

    val descriptionText: String
        get() = data.get()?.problemDescSummary.orEmpty()

    val typeText: String
        get() {
            val problemType = data.get()?.problemType
            return Config.get().appConfig.userFeedBackTypeList
                ?.firstOrNull { it.key == problemType }
                ?.value
                ?: problemType?.toString().orEmpty()
        }

    val submittedOnText: String
        get() {
            val rawValue = data.get()?.createTime?.trim().orEmpty()
            return rawValue.toLongOrNull()
                ?.let { DateUtil.format(it, DateUtil.FORMAT_EN_DAY) }
                ?: rawValue
        }

    /** 接口未定义 feedbackStatus 的文案映射，按回复版本判断是否已回复。 */
    val statusText: String
        get() = if (hasReply()) {
            FeedbackViewModel.STATUS_REPLIED
        } else {
            FeedbackViewModel.STATUS_IN_PROGRESS
        }

    @ColorRes
    fun stateTextColor(): Int {
        return if (hasReply()) R.color.color_3c66ff else R.color.color_ea9800
    }

    private fun hasReply(): Boolean = (data.get()?.replyVersion ?: 0) > 0
}
