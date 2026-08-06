package com.kuaigeglobal.epnova.core.network.sse
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryStreamEventData

/**
 * 新 task-output SSE 协议的内部事件。
 *
 * ViewModel 只消费这些业务事件，不直接解析原始 SSE 帧。
 */
sealed class TaskOutputEvent {
    /** chunk: 追加展示 content，并保存 SSE 原生 id。 */
    data class Chunk(val lastEventId: String?, val text: String) : TaskOutputEvent()

    /** final: 只携带原始 result，由业务 mapper 转成现有轮询实体。 */
    data class Final(val lastEventId: String?, val data: StoryStreamEventData) : TaskOutputEvent()

    /** event:error: 后端业务失败，不等同于网络断开。 */
    data class Error(
        val lastEventId: String?,
        val errorCode: String?,
        val errorType: String?,
        val errorMessage: String?,
        val hitWords: List<String>?
    ) : TaskOutputEvent()

    /** SSEEvent.Error 或异常断开：连接失败，重试时先查 session。 */
    data class ConnectionError(val throwable: Throwable?) : TaskOutputEvent()

    data class Heartbeat(val lastEventId: String?) : TaskOutputEvent()
    object Open : TaskOutputEvent()
    object Close : TaskOutputEvent()
}
