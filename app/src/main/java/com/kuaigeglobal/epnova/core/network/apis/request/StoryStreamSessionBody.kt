package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 故事梗概/故事剧本流式 session 与 subscribe 共用请求体。
 *
 * lastEventId 是 SSE 原生 id，不解析、不改写，只按后端返回原样回传。
 * storyId 仅剧本 session 有值时传；outline 与 subscribe 不依赖它。
 */
data class StoryStreamSessionBody(
    // 四个新接口都必传；前端只用 workId 定位作品维度的生成任务。
    val workId: String? = null,
    // 断线或退出再进时携带最近收到的 SSE id；首次订阅或 reset=true 后不传。
    val lastEventId: String? = null,
    // 剧本阶段用于定位当前剧集；梗概接口和首次无 storyId 场景保持为空。
    val storyId: String? = null
) : UserTokenBody()
