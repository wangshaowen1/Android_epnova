package com.kuaigeglobal.epnova.feature.message

import com.kuaigeglobal.epnova.base.item.AppItemViewModel

/** 暂无通知接口，先保留页面所需的展示字段。 */
data class NotificationItem(
    val title: String,
    val content: String,
    val workTitle: String,
    val timeText: String
)

class ItemNotificationVm(notice: NotificationItem) : AppItemViewModel<NotificationItem>(notice)
