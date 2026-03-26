package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "workId": "作品 ID",
 *     "storyId": "当前上报所对应的剧集 ID",
 *     "playProgress": "当前集播放进度百分比，取值范围 0～100",
 *     "staySeconds": "本次上报窗口内累计停留或有效播放秒数（用于配合「观看满 3 秒记历史」等规则）",
 *     "sortNo": "当前集在作品内的序号（sort_no），可选冗余字段，便于服务端少一次查询"
 * }
 */
data class UserPlayBody(
    val playProgress: Int? = null,
    val sortNo: Int? = null,
    val staySeconds: Int? = null,
    val storyId: String? = null,
    val workId: String? = null
)