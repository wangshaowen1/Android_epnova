package com.kuaigeglobal.epnova.core.network.apis.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * SSE data: 后面的大括号业务 JSON。
 *
 * event/type/id 属于 SSE 帧元信息，由 SSEEvent.Message 承载，不放在这个模型里。
 * 这个模型只对应 data: {...} 里的内容；断点续传使用 SSEEvent.Message.id，不使用业务字段。
 */
@Serializable
data class StoryStreamEventData(
    // 后端业务 code：chunk=1、heartbeat=0、final=-1、error=2 等，仅用于日志和兜底判断。
    val code: Int? = null,
    // chunk 文本增量；final 不依赖该字段。
    val content: String? = null,
    // final 结构化结果；outline 兼容 RollNovelSummaryResult 和旧 PollSummaryData 包装结构，script 按 PollScriptData 解析。
    val result: JsonElement? = null,
    // event:error 的业务错误信息；网络断开不会走这些字段。
    val errorCode: String? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    // 业务错误可能携带命中词，首版主要用于日志和后续扩展。
    val hitWords: List<String>? = null
)
