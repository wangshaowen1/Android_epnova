package com.kuaigeglobal.epnova.feature.creator.generate

import android.util.Log
import com.google.gson.Gson
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryStreamEventData
import com.kuaigeglobal.epnova.core.network.apis.request.PollScriptData
import com.kuaigeglobal.epnova.core.network.apis.request.PollSummaryData
import com.kuaigeglobal.epnova.core.network.apis.request.RollNovelSummaryResult

/**
 * 将 SSE final.result 收口转换为现有轮询实体。
 *
 * UI 层继续消费 PollSummaryData/PollScriptData，避免直接依赖后端原始 JSON。
 */
object StoryStreamFinalMapper {

    private val gson by lazy { Gson() }

    /**
     * outline final.result 兼容直接返回 RollNovelSummaryResult 和旧 PollSummaryData 包装结构。
     *
     * 后端在不同时机可能返回两种形态：
     * 1. PollSummaryData 包装结构：包含 genStatus/workId/result，直接补齐缺省字段后给旧 UI。
     * 2. RollNovelSummaryResult 裸结果：只有 workTitle/workDesc/structureList，需要包装成 PollSummaryData。
     *
     * 这里集中处理兼容，避免 Step1 Fragment/ViewModel 直接依赖 final.result 的 JSON 形态。
     */
    fun toPollSummaryData(
        eventData: StoryStreamEventData,
        fallbackWorkId: String?
    ): PollSummaryData {
        val resultJson = eventData.result?.toString()
        Log.d(TAG, "outline final.result=${resultJson.previewLog()}")
        val decoded = resultJson?.let { json ->
            runCatching {
                gson.fromJson(json, PollSummaryData::class.java)
            }.onSuccess { result ->
                Log.d(
                    TAG,
                    "outline final parse success genStatus=${result?.genStatus}, workTitle=${result?.result?.workTitle}, structureSize=${result?.result?.structureList?.size ?: 0}"
                )
            }.onFailure {
                Log.e(TAG, "outline final parse failed result=${json.previewLog()}", it)
            }.getOrNull()
        }
        decoded?.takeIf { it.result != null || it.genStatus != null }?.let {
            // 已经是旧轮询实体时保留后端 genStatus；缺省时按 final 成功补 2。
            return it.copy(
                genStatus = it.genStatus ?: GEN_STATUS_SUCCESS,
                workId = it.workId ?: fallbackWorkId
            )
        }
        val summaryResult = resultJson?.let { json ->
            runCatching {
                gson.fromJson(json, RollNovelSummaryResult::class.java)
            }.onSuccess { result ->
                Log.d(
                    TAG,
                    "outline final parse direct result success workTitle=${result?.workTitle}, structureSize=${result?.structureList?.size ?: 0}"
                )
            }.onFailure {
                Log.e(TAG, "outline final parse direct result failed result=${json.previewLog()}", it)
            }.getOrNull()
        }
        return summaryResult?.takeIf { it.hasContent() }?.let {
            // 后端只返回梗概业务结果时，包装成 PollSummaryData 才能复用现有简介和目录 UI。
            PollSummaryData(
                genStatus = GEN_STATUS_SUCCESS,
                result = it,
                workId = fallbackWorkId
            )
        } ?: decoded?.let {
            // decoded 走到这里说明没有 result/genStatus，但仍可能携带 workId 等字段；补成功态兜底。
            it.copy(
                genStatus = GEN_STATUS_SUCCESS,
                workId = it.workId ?: fallbackWorkId
            )
        } ?: PollSummaryData(genStatus = GEN_STATUS_SUCCESS, workId = fallbackWorkId)
    }

    /**
     * session.result 是 RollNovelSummaryResult，包成旧 UI 消费的 PollSummaryData。
     *
     * session 接口只有在 genStatus=2 且 result 非空时才走这里；如果 genStatus 缺省，
     * 按成功态补齐，保证 storySummaryResult.isSuccess() 能驱动最终内容区展示。
     */
    fun normalizeSessionSummaryData(
        data: RollNovelSummaryResult?,
        fallbackWorkId: String?,
        genStatus: Int? = null
    ): PollSummaryData {
        Log.d(
            TAG,
            "outline session result normalize genStatus=$genStatus, workTitle=${data?.workTitle}, structureSize=${data?.structureList?.size ?: 0}"
        )
        return PollSummaryData(
            genStatus = genStatus ?: GEN_STATUS_SUCCESS,
            result = data,
            workId = fallbackWorkId
        )
    }

    private fun RollNovelSummaryResult.hasContent(): Boolean {
        return !workTitle.isNullOrBlank() || !workDesc.isNullOrBlank() || !structureList.isNullOrEmpty()
    }

    /**
     * script final.result 优先按 PollScriptData 解析，后端若只返回字符串则退化为 storyContent。
     *
     * 剧本页面最终仍以 PollScriptData 驱动：episodeId、sortNo、storyContent 都依赖这个实体。
     * 如果后端 result 是字符串，decodeStringResult 会把它作为 storyContent 兜底。
     */
    fun toPollScriptData(
        eventData: StoryStreamEventData,
        fallbackWorkId: String?,
        fallbackStoryId: String?,
        fallbackSortNo: Int?
    ): PollScriptData {
        val resultJson = eventData.result?.toString()
        Log.d(TAG, "script final.result=${resultJson.previewLog()}")
        val decoded = resultJson?.let { json ->
            runCatching {
                gson.fromJson(json, PollScriptData::class.java)
            }.onSuccess { result ->
                Log.d(
                    TAG,
                    "script final parse success workId=${result?.workId}, storyId=${result?.storyId}, sortNo=${result?.sortNo}, toastTextProgress=${result?.toastTextProgress}, toastTextTime=${result?.toastTextTime}, toastTextEstimatedSeconds=${result?.toastTextEstimatedSeconds}, contentLength=${result?.storyContent?.length ?: 0}"
                )
            }.onFailure {
                Log.e(TAG, "script final parse PollScriptData failed result=${json.previewLog()}", it)
            }.getOrNull()
        }
        val textResult = resultJson?.let { decodeStringResult(it) }
        return PollScriptData(
            // workId/storyId/sortNo 是后续 saveScript 和资产生成入口的必需上下文，缺省时用页面当前值兜底。
            workId = decoded?.workId ?: fallbackWorkId,
            storyId = decoded?.storyId ?: fallbackStoryId,
            sortNo = decoded?.sortNo ?: fallbackSortNo,
            genStatus = decoded?.genStatus ?: GEN_STATUS_SUCCESS,
            toastTextProgress = decoded?.toastTextProgress,
            toastTextTime = decoded?.toastTextTime,
            toastTextEstimatedSeconds = decoded?.toastTextEstimatedSeconds,
            storyContent = decoded?.storyContent ?: eventData.content ?: textResult
        )
    }

    /**
     * session.result 已经是剧本实体时直接使用，只补齐旧 UI 依赖的成功态和兜底字段。
     *
     * 这条路径用于 session 返回“任务已完成但无需再订阅 SSE”的情况。
     * 不能再调旧 result 接口兜底，否则会破坏 session/subscribe 的单一状态机。
     */
    fun normalizeSessionScriptData(
        data: PollScriptData?,
        fallbackWorkId: String?,
        fallbackStoryId: String?,
        fallbackSortNo: Int?
    ): PollScriptData {
        Log.d(
            TAG,
            "script session result normalize workId=${data?.workId}, storyId=${data?.storyId}, sortNo=${data?.sortNo}, contentLength=${data?.storyContent?.length ?: 0}"
        )
        return data?.let {
            it.copy(
                workId = it.workId ?: fallbackWorkId,
                storyId = it.storyId ?: fallbackStoryId,
                sortNo = it.sortNo ?: fallbackSortNo,
                genStatus = it.genStatus ?: GEN_STATUS_SUCCESS
            )
        } ?: PollScriptData(
            workId = fallbackWorkId,
            storyId = fallbackStoryId,
            sortNo = fallbackSortNo,
            genStatus = GEN_STATUS_SUCCESS
        )
    }

    private fun decodeStringResult(json: String): String? {
        return runCatching {
            gson.fromJson(json, String::class.java)
        }.onSuccess {
            Log.d(TAG, "script final parse string success contentLength=${it?.length ?: 0}")
        }.getOrNull()
    }

    private fun String?.previewLog(): String {
        if (this == null) return "null"
        return if (length <= MAX_LOG_LENGTH) this else take(MAX_LOG_LENGTH) + "...(truncated, length=$length)"
    }

    private const val TAG = "StoryStreamFinalMapper"
    private const val MAX_LOG_LENGTH = 1000
    private const val GEN_STATUS_SUCCESS = 2
}
