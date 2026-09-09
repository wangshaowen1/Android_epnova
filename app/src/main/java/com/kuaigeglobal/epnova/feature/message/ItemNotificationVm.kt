package com.kuaigeglobal.epnova.feature.message

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.network.apis.entity.SystemNoticeInfo

/** 消息实体的展示包装，不复制或转换接口字段。 */
class ItemNotificationVm(notice: SystemNoticeInfo?) : AppItemViewModel<SystemNoticeInfo>(notice) {

    val timeText: String
        get() = data.get()?.createTime?.let {
            DateUtil.format(it, DateUtil.FORMAT_EN_DAY)
        }.orEmpty()
}
