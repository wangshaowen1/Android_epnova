package com.kuaigeglobal.epnova.feature.message

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.network.apis.entity.MessagePageInfo

/** 消息实体的展示包装，不复制或转换接口字段，只补充 XML 所需的派生展示值。 */
class ItemNotificationVm(notice: MessagePageInfo?) : AppItemViewModel<MessagePageInfo>(notice) {

    /** 将接口返回的 Unix 毫秒时间戳格式化为消息卡片使用的英文日期。 */
    val timeText: String
        get() = data.get()?.sendTime?.let {
            DateUtil.format(it, DateUtil.FORMAT_EN_DAY)
        }.orEmpty()

    /** 关联作品标题为空时为 true，XML 据此隐藏作品标题区域。 */
    val isWorkTitle: Boolean = data.get()?.workTitle.isNullOrEmpty()
}
