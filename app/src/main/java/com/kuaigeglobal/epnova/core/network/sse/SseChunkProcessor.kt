package com.kuaigeglobal.epnova.core.network.sse

/**
 * SSE chunk 处理策略（解耦点）
 */
interface SseChunkProcessor {

    /** SSE 开始 */
    fun onSSEStart()

    /** 每一段 SSE data */
    fun onSSEChunk(chunk: String)

    /** SSE 正常结束 */
    fun onSSEComplete()

    /** SSE 发生错误 */
    fun onSSEError(e: Throwable?)
}
