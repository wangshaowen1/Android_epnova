package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.annotation.ColorRes
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackPageInfo

/** 将反馈历史接口条目转换为列表展示文本和颜色，供 item XML 绑定。 */
class ItemFeedbackHistoryVm(feedback: FeedbackPageInfo) :
    AppItemViewModel<FeedbackPageInfo>(feedback) {

    val descriptionText: String
        get() = data.get()?.problemDescSummary.orEmpty()

    /** 类型名称从应用配置查询，未找到时展示类型编号。 */
    val typeText: String
        get() {
            val problemType = data.get()?.problemType
            return Config.get().appConfig.userFeedBackTypeList
                ?.firstOrNull { it.key == problemType }
                ?.value
                ?: problemType?.toString().orEmpty()
        }

    /** 数字时间按毫秒格式化为列表日期，非数字时间保留接口原文。 */
    val submittedOnText: String
        get() {
            val rawValue = data.get()?.createTime?.trim().orEmpty()
            return rawValue.toLongOrNull()
                ?.let { DateUtil.format(it, DateUtil.FORMAT_EN_DAY) }
                ?: rawValue
        }

    /** 列表按回复版本大于 0 判断已回复，状态文案和颜色使用相同判断。 */
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
