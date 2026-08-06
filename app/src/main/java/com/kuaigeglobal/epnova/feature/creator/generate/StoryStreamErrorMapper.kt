package com.kuaigeglobal.epnova.feature.creator.generate

import com.kuaigeglobal.epnova.core.network.sse.TaskOutputEvent


/**
 * 流式输出阶段的 UI 错误类型。
 *
 * BusinessFailed 来自后端 event:error；ConnectionBroken 来自建连失败或异常断开。
 */
sealed class StoryStreamErrorUi {
    data class BusinessFailed(val message: String?) : StoryStreamErrorUi()
    data class ConnectionBroken(val message: String?) : StoryStreamErrorUi()
}

/**
 * 只做错误归类，不直接弹 toast/改 UI，方便 Step1/Step2 共用。
 */
object StoryStreamErrorMapper {
    fun fromBusinessError(event: TaskOutputEvent.Error): StoryStreamErrorUi {
        return StoryStreamErrorUi.BusinessFailed(event.errorMessage)
    }

    fun fromConnectionError(event: TaskOutputEvent.ConnectionError): StoryStreamErrorUi {
        return StoryStreamErrorUi.ConnectionBroken(event.throwable?.message)
    }
}
