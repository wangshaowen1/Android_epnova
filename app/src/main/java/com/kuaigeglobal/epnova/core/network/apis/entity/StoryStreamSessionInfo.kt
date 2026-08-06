package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 后端流式会话状态。
 *
 * 创建任务接口只负责启动后端任务；进入生成页后，前端通过 session 判断当前阶段是否有任务、
 * SSE 是否已经可以订阅、以及是否已经有最终 result 可以直接展示。
 *
 * 关键规则：
 * 1. hasTask=false 表示当前阶段没有任务，不能订阅。
 * 2. hasTask=true 且 streamAvailable=false 表示任务存在但 SSE 还没开始，继续轮询 session。
 * 3. streamAvailable=true 且 runId 非空时才调用 subscribe。
 * 4. reset=true 表示本地 lastEventId 已不可回放，前端必须清空本地流式缓存并不再传旧 id。
 * 5. genStatus=2 且 result 非空时可以跳过 SSE，直接用 result 刷新结构化 UI。
 */
data class StoryStreamSessionInfo<T>(
    // 剧本阶段的剧集 ID；梗概阶段通常为空。session 返回后用于回填当前 episodeId。
    val storyId: String? = null,
    // 作品 ID。请求里必传，返回里用于兜底刷新旧 UI 依赖的 workId 字段。
    val workId: String? = null,
    // session 轮询阶段展示在 loading 区域的进度提示文案。
    val toastTextProgress: String? = null,
    // session 轮询阶段展示在 loading 区域的预计剩余时间文案。
    val toastTextTime: String? = null,
    // session 轮询阶段展示在 loading 区域的预估总秒数，前端按秒倒计时。
    val toastTextEstimatedSeconds: Int? = null,
    // outline SSE 文本展示起点；命中该字段值后才隐藏 loading 并展示，展示内容包含该值。
    val markdownCutKey: String? = null,
    // 当前生成阶段：outline=故事梗概，script=故事剧本。
    val stage: String? = null,
    // 当前阶段是否存在生成任务；false 时前端不应该订阅 SSE。
    val hasTask: Boolean? = null,
    // SSE 是否已经准备好；false/null 都表示继续轮询 session。
    val streamAvailable: Boolean? = null,
    // 后端流任务状态，仅用于前端判断失败/成功态，不替代 genStatus 的业务判断。
    val streamStatus: String? = null,
    // 关联生成任务状态：0待生成、1生成中、2成功、3失败、6算力不足请充值等。
    val genStatus: Int? = null,
    // 后端本次流式 run 标识；前端不解析、不存储，只作为是否可订阅的就绪条件。
    val runId: String? = null,
    // 后端认为当前可用的最后事件 ID；前端断线重连仍以本地最近收到的 SSE id 为准。
    val lastEventId: String? = null,
    // true 表示前端传入的 lastEventId 已过期或不可回放。
    val reset: Boolean? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    // session 可能直接返回已完成的 final.result，有值时不再订阅 SSE，直接刷新结构化结果。
    val result: T? = null
) {
    /**
     * session 已携带结果体；是否可以直接展示还需要结合具体业务状态判断。
     */
    fun hasFinalResult(): Boolean = result != null

    /**
     * 梗概 session 只有任务状态成功且 result 非空时，才可以直接刷新结构化结果。
     */
    fun hasSuccessFinalResult(): Boolean = genStatus == GEN_STATUS_SUCCESS && result != null

    /**
     * 有任务、有可用流、且 runId 存在时才可以订阅；streamStatus=SUCCESS 时也可能通过订阅回放 final。
     */
    fun canSubscribe(): Boolean = hasTask == true && streamAvailable == true && !runId.isNullOrBlank()

    /**
     * 已有任务但 SSE 还没开始，继续轮询 session，不能切到旧结果轮询。
     */
    fun shouldPollSession(): Boolean = hasTask == true && streamAvailable != true

    /**
     * 当前阶段没有生成任务。
     */
    fun hasNoTask(): Boolean = hasTask == false

    /**
     * lastEventId 已过期时，前端清空本地已展示文本，并不再携带旧 lastEventId。
     */
    fun shouldReset(): Boolean = reset == true

    fun isSuccess(): Boolean = streamStatus == STREAM_STATUS_SUCCESS

    fun isFailed(): Boolean = streamStatus == STREAM_STATUS_FAILED || genStatus == GEN_STATUS_FAILED

    fun isNoPower(): Boolean = genStatus == GEN_STATUS_NO_POWER

    companion object {
        const val STAGE_OUTLINE = "outline"
        const val STAGE_SCRIPT = "script"

        const val STREAM_STATUS_RUNNING = "RUNNING"
        const val STREAM_STATUS_SUCCESS = "SUCCESS"
        const val STREAM_STATUS_FAILED = "FAILED"

        const val GEN_STATUS_SUCCESS = 2
        const val GEN_STATUS_FAILED = 3
        const val GEN_STATUS_NO_POWER = 6
    }
}
