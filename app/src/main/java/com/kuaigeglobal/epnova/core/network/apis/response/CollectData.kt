package com.kuaigeglobal.epnova.core.network.apis.response

/**
 *  {
 *         "collected": "操作完成后，当前用户对该作品是否处于「已收藏」状态",
 *         "collectCount": "展示用收藏总人数（或作品维度收藏计数，与产品文案一致）"
 *     }
 */
data class CollectData(
    val collected: Boolean? = null,
    val collectCount: Int? = null,
)

/**
 *  {
 *         "liked": "操作完成后，当前用户是否处于「已点赞」状态",
 *         "likeCount": "展示用点赞数（虚拟 + 真实合并策略由实现决定）"
 *     }
 */
data class LikeData(
    val liked: Boolean? = null,
    val likeCount: Int? = null,
)

/**
 * {
 *         "collected": "当前用户是否已收藏该作品",
 *         "liked": "当前用户是否已点赞（表结构为作品维度时即作品级点赞态）",
 *         "lastStoryId": "用户在该作品下最近播放的剧集 ID",
 *         "lastSortNo": "最近播放集在作品内的序号（sort_no）",
 *         "playProgress": "最近播放集的进度百分比，0～100",
 *         "resumeStoryId": "本次进入播放器应该续播的剧集 ID",
 *         "resumeSortNo": "本次进入播放器应该续播的剧集序号",
 *         "resumeSecond": "本次进入播放器应该跳转到的秒数；已到片尾时返回 0",
 *         "durationSeconds": "最近播放剧集总时长（秒）",
 *         "finished": "最近进度是否已到片尾"
 *     }
 */
data class UserPlayData(
    val lastStoryId: String? = null,
    val collected: Boolean? = null,
    val lastSortNo: Int? = null,
    val liked: Boolean? = null,
    val playProgress: Int? = null,
    val resumeStoryId: String? = null,
    val resumeSortNo: Int? = null,
    val resumeSecond: Int? = null,
    val durationSeconds: Int? = null,
    val finished: Boolean? = null,
)
