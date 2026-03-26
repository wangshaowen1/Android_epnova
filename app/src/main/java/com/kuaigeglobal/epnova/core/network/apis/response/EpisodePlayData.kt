package com.kuaigeglobal.epnova.core.network.apis.response

/**
 *  {
 *         "workId": "作品 ID",
 *         "workTitle": "作品标题（可与播放器顶栏文案一致）",
 *         "storyId": "剧集 ID",
 *         "storyTitle": "剧集标题",
 *         "sortNo": "当前集在作品内的序号（sort_no），从 1 起",
 *         "videoUrl": "视频播放地址",
 *         "duration": "视频时长，单位：秒",
 *         "workRatio": "视频画幅比例，如 16:9、9:16",
 *         "likeCount": "展示用点赞数（虚拟 + 真实合并策略由实现决定）",
 *         "playCount": "展示用该剧集播放量"
 *     }
 */
data class EpisodePlayData(
    val workId: String? = null,
    val duration: Int? = null,
    val likeCount: Int? = null,
    val playCount: Int? = null,
    val sortNo: Int? = null,
    val storyId: String? = null,
    val storyTitle: String? = null,
    val videoUrl: String? = null,
    val workRatio: String? = null,
    val workTitle: String? = null
)