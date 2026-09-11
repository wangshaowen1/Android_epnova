package com.kuaigeglobal.epnova.feature.message

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.network.apis.entity.MessagePageInfo

/** 消息实体的展示包装，不复制或转换接口字段。 */
class ItemNotificationVm(notice: MessagePageInfo?) : AppItemViewModel<MessagePageInfo>(notice) {

    val timeText: String
        get() = data.get()?.sendTime?.let {
            DateUtil.format(it, DateUtil.FORMAT_EN_DAY)
        }.orEmpty()

    val isWorkTitle: Boolean = data.get()?.workTitle.isNullOrEmpty()
}
