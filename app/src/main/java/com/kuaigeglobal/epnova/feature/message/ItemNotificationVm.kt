package com.kuaigeglobal.epnova.feature.message

import com.kuaigeglobal.epnova.base.item.AppItemViewModel

/** 暂无通知接口，先保留页面所需的展示字段。 */
data class NotificationItem(
    val title: String,
    val content: String,
    val workTitle: String,
    val timeText: String,
    val type: Int = 0,
    val workId: String? = null,
    val storyId: String? = null
) {
    companion object {
        /** 普通通知，不可点击。 */
        const val TYPE_NONE = 0

        /** 对应作品，打开短剧播放器的第一集。 */
        const val TYPE_WORK = 1

        /** 对应剧集，打开剧集创作页。 */
        const val TYPE_STORY = 2
    }
}

class ItemNotificationVm(notice: NotificationItem) : AppItemViewModel<NotificationItem>(notice)
