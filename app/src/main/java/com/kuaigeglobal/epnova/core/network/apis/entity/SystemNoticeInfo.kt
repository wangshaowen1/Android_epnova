package com.kuaigeglobal.epnova.core.network.apis.entity

import kotlin.collections.isNullOrEmpty
import kotlin.text.isNullOrEmpty
import kotlin.text.split

data class SystemNoticeInfo(
    val jumpUri: String?,
    val createTime: Long?,
    val messagesContent: String?,
    val messagesImage: String?,
    val messagesTitle: String?,
    val messagesType: String?,
    val messagesImageList: String?,
    val messagesId: String?,
) {
    val imageList
        get() = messagesImageList?.split(",")

    val showViewDetail
        get() = !jumpUri.isNullOrEmpty() || !imageList.isNullOrEmpty()
}


/**
 * {
 *                 "likeUserId": "发起点赞用户ID，实际返回String类型",
 *                 "likeUserNickName": "发起点赞用户昵称",
 *                 "likeUserAvatar": "发起点赞用户头像URL",
 *                 "likeTargetType": "[枚举]点赞目标类型 [1=短剧作品]",
 *                 "likeTargetBusId": "点赞目标业务ID[点赞目标类型=1时为短剧id] 实际返回String类型",
 *                 "likeTargetCoverUrl": "点赞目标封面",
 *                 "createTime": "创建时间"
 *             }
 */
data class SystemMessageLikeInfo(
    val createTime: Long? = null,
    val likeTargetBusId: String? = null,
    val likeTargetCoverUrl: String? = null,
    val likeTargetType: Int? = null,
    val likeUserAvatar: String? = null,
    val likeUserId: String? = null,
    val likeUserNickName: String? = null
)

/**
 * {
 *                 "dramasSeriesId": "剧集ID，实际返回String类型",
 *                 "seriesCoverUrl": "剧集封面的URL",
 *                 "watchingUserId": "发起追剧用户ID，实际返回String类型",
 *                 "watchingUserNickName": "发起追剧用户昵称",
 *                 "watchingUserAvatar": "发起追剧用户头像URL",
 *                 "createTime": "创建时间"
 *             }
 */
data class SystemMessageWatchingInfo(
    val createTime: Long? = null,
    val dramasSeriesId: String? = null,
    val seriesCoverUrl: String? = null,
    val watchingUserAvatar: String? = null,
    val watchingUserId: String? = null,
    val watchingUserNickName: String? = null
)