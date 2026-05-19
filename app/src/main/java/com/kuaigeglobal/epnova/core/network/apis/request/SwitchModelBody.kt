package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * {
 *     "workId": "作品ID",
 *     "storyId": "剧集ID",
 *     "storyboardModelCode": "分镜模型code，关联 b_work_static_resource_config.config_code（config_type=7）"
 * }
 */
data class SwitchModelBody(
    val workId: String? = null,
    val storyId: String? = null,
    val storyboardModelCode: String? = null,
)