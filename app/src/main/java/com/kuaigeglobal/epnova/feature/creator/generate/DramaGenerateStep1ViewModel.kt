package com.kuaigeglobal.epnova.feature.creator.generate

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.ext.PollingState
import com.kuaigeglobal.epnova.core.ext.launchPolling
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.model.StoryOutlineStreamCache
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryStreamSessionInfo
import com.kuaigeglobal.epnova.core.network.apis.request.ContinueTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.PollSummaryData
import com.kuaigeglobal.epnova.core.network.apis.request.RollNovelSummaryResult
import com.kuaigeglobal.epnova.core.network.apis.request.StoryStreamSessionBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkInfoUpdateBody
import com.kuaigeglobal.epnova.core.network.sse.TaskOutputEvent
import com.kuaigeglobal.epnova.core.network.sse.TaskOutputSseHandler
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 故事梗概生成
 */
class DramaGenerateStep1ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep1ViewModel"
        const val DISPLAY_CONTENT = 0
        const val DISPLAY_LOADING = 1
        const val DISPLAY_STREAMING = 2
        const val DISPLAY_FAIL = 3
        private const val SESSION_POLL_INTERVAL_MS = 2000L
        private const val SUMMARY_TEXT_EMIT_INTERVAL_MS = 64L
        private const val TOAST_COUNTDOWN_INTERVAL_MS = 1000L
    }

    /**
     * 算力不足时，延迟2s后触发充值弹窗（Activity负责弹出并回调 onRecharged）
     */
    val showRechargeDialogEvent = MutableLiveData<Unit>()


    //小说文档扣费后生成的workId，用于轮询
    var workId: String? = null

    //业务类型
    var model: Int? = null

    // 作品名称
    var mWorkName = MutableLiveData<String>()

    val displayChild = ObservableInt(0)
    val isSummaryLoading = ObservableBoolean(true)
    val isSummaryStreaming = ObservableBoolean(false)
    val nextStepEnabled by lazy { ObservableBoolean(false) }

    val summaryStreamingText = MutableLiveData<String>()
    var summaryLastEventId: String? = null
    private var summaryStreamHandler: TaskOutputSseHandler? = null
    private val summaryRawStreamingBuilder = StringBuilder()
    private val summaryStreamingBuilder = StringBuilder()
    private var summaryMarkdownCutKey: String? = null
    private var summaryMarkdownCutMatched = false
    private var summaryStreamCacheRestored = false

    /**
     * 故事梗概结果
     */
    var storySummaryResult = MutableLiveData<PollSummaryData?>()
    val summaryToastTextTime = ObservableField<String>()

    val isBalanceFail = ObservableBoolean(false)

    fun isStoryDocumentsUploadMode(): Boolean {
        return model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS
    }

    fun isInspirationMode(): Boolean {
        return model == DramaGenerateConstants.MODEL_INSPIRATION ||
                model == DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION
    }

    fun useOldSummaryBusiness(): Boolean {
        return !isInspirationMode()
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        saveSummaryStreamCacheIfNeeded()
        novelSummaryPollingJob?.cancel()
        summarySessionJob?.cancel()
        summaryStreamingTextEmitJob?.cancel()
        summaryToastCountdownJob?.cancel()
        summaryStreamHandler?.cancel()
    }

    private var novelSummaryPollingJob: Job? = null
    private var summarySessionJob: Job? = null
    private var summaryStreamingTextEmitJob: Job? = null
    private var summaryToastCountdownJob: Job? = null
    private var summaryToastCountdownKey: String? = null
    private var summaryToastCountdownInitialSeconds: Int? = null

    /**
     * 只关闭前端 session、SSE 和旧结果轮询，不取消后端生成任务。
     */
    fun cancelSummaryStream() {
        Log.d(TAG, "cancel outline stream workId=$workId, lastEventId=$summaryLastEventId")
        saveSummaryStreamCacheIfNeeded()
        summarySessionJob?.cancel()
        summaryStreamHandler?.cancel()
        novelSummaryPollingJob?.cancel()
        cancelSummaryStreamingTextEmit()
        cancelSummaryToastCountdown()
        isSummaryStreaming.set(false)
    }

    /**
     * 新流式入口：先轮询 outline session，待流可订阅后再建立 SSE。
     */
    fun startSummarySession(
        lastEventId: String? = summaryLastEventId,
        resetContent: Boolean = false
    ) {
        if (useOldSummaryBusiness()) {
            pollSummary()
            return
        }
        if (summarySessionJob?.isActive == true) return
        if (!resetContent) {
            restoreSummaryStreamCacheIfNeeded()
        }
        var pollingLastEventId = if (resetContent) null else (lastEventId ?: summaryLastEventId)

        if (resetContent) {
            resetSummaryStreamState()
        } else {
            isSummaryLoading.set(summaryStreamingBuilder.isEmpty())
            isSummaryStreaming.set(summaryStreamingBuilder.isNotEmpty())
            isBalanceFail.set(false)
            nextStepEnabled.set(false)
            displayChild.set(
                if (summaryStreamingBuilder.isEmpty()) DISPLAY_LOADING else DISPLAY_STREAMING
            )
            if (summaryStreamCacheRestored && summaryStreamingBuilder.isNotEmpty()) {
                emitSummaryStreamingText(immediate = true)
            }
        }

        summaryStreamHandler?.cancel()
        novelSummaryPollingJob?.cancel()
        summarySessionJob = launchPolling(
            intervalMs = SESSION_POLL_INTERVAL_MS,
            retryOnNetworkError = true,
            showToastError = false,
            apiBlock = {
                mRepo.api.storyStreamOutlineSession(
                    StoryStreamSessionBody(
                        workId = workId,
                        lastEventId = pollingLastEventId
                    )
                )
            },
            checkState = { response ->
                val sessionInfo = response?.data
                when {
                    sessionInfo?.isNoPower() == true -> PollingState.FAIL
                    sessionInfo?.hasSuccessFinalResult() == true -> PollingState.SUCCESS
                    sessionInfo?.canSubscribe() == true -> PollingState.SUCCESS
                    sessionInfo?.isFailed() == true || sessionInfo?.hasNoTask() == true -> PollingState.FAIL
                    sessionInfo?.shouldPollSession() == true -> PollingState.CONTINUE
                    else -> PollingState.FAIL
                }
            },
            onSuccess = { response ->
                handleSummarySessionReady(response?.data, pollingLastEventId)
            },
            onFail = { response, error ->
                handleSummarySessionFail(response?.data, error)
            },
            onPolling = { response ->
                pollingLastEventId = handleSummarySessionPolling(
                    response?.data,
                    pollingLastEventId
                )
            }
        )
    }

    private fun handleSummarySessionReady(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>?,
        requestedLastEventId: String?
    ) {
        summarySessionJob = null
        if (sessionInfo?.hasSuccessFinalResult() == true) {
            handleSummarySessionFinal(sessionInfo)
            return
        }
        val nextLastEventId = sessionInfo?.let {
            handleSummarySessionResetIfNeeded(it, requestedLastEventId)
        }
        updateSummarySessionToast(sessionInfo)
        updateSummaryMarkdownCutKey(sessionInfo)
        subscribeSummaryStream(nextLastEventId)
    }

    private fun handleSummarySessionPolling(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>?,
        requestedLastEventId: String?
    ): String? {
        val nextLastEventId = if (sessionInfo?.shouldReset() == true) {
            handleSummarySessionResetIfNeeded(sessionInfo, requestedLastEventId)
        } else {
            requestedLastEventId
        }
        updateSummarySessionToast(sessionInfo)
        updateSummaryMarkdownCutKey(sessionInfo)
        return nextLastEventId
    }

    private fun updateSummarySessionToast(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>?
    ) {
        if (sessionInfo?.toastTextProgress.isNullOrBlank() &&
            sessionInfo?.toastTextTime.isNullOrBlank() &&
            sessionInfo?.toastTextEstimatedSeconds == null
        ) return

        val current = storySummaryResult.value
        val estimatedSeconds = getSummaryToastEstimatedSeconds(sessionInfo)
        val nextToastTextTime = if (estimatedSeconds != null) {
            val countdownKey = getSummaryToastCountdownKey(sessionInfo)
            val isSameCountdown = summaryToastCountdownKey == countdownKey &&
                    summaryToastCountdownInitialSeconds == estimatedSeconds
            summaryToastTextTime.get()
                ?: if (isSameCountdown) null else formatToastCountdown(estimatedSeconds)
        } else {
            sessionInfo?.toastTextTime ?: summaryToastTextTime.get() ?: current?.toastTextTime
        }
        summaryToastTextTime.set(nextToastTextTime)
        storySummaryResult.value = PollSummaryData(
            genStatus = current?.genStatus,
            toastTextProgress = sessionInfo?.toastTextProgress ?: current?.toastTextProgress,
            toastTextTime = nextToastTextTime,
            toastTextEstimatedSeconds = sessionInfo?.toastTextEstimatedSeconds
                ?: current?.toastTextEstimatedSeconds,
            novelCharCount = current?.novelCharCount,
            novelFileName = current?.novelFileName,
            toastTextAuditFailed = current?.toastTextAuditFailed,
            result = current?.result,
            workId = current?.workId ?: sessionInfo?.workId ?: workId
        )
        startSummaryToastCountdownIfNeeded(sessionInfo)
    }

    private fun startSummaryToastCountdownIfNeeded(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>?
    ) {
        val info = sessionInfo ?: return
        val estimatedSeconds = getSummaryToastEstimatedSeconds(info) ?: return
        val countdownKey = getSummaryToastCountdownKey(info)
        val isSameCountdown = summaryToastCountdownKey == countdownKey &&
                summaryToastCountdownInitialSeconds == estimatedSeconds
        if (isSameCountdown) {
            val currentToastTime = summaryToastTextTime.get()
            if (summaryToastCountdownJob?.isActive == true || currentToastTime.isNullOrBlank()) return
        }

        cancelSummaryToastCountdown(clearState = false, clearText = false)
        summaryToastCountdownKey = countdownKey
        summaryToastCountdownInitialSeconds = estimatedSeconds
        updateSummaryToastCountdownTime(estimatedSeconds)
        summaryToastCountdownJob = viewModelScope.launch {
            var remainingSeconds = estimatedSeconds
            while (remainingSeconds > 0) {
                delay(TOAST_COUNTDOWN_INTERVAL_MS)
                remainingSeconds -= 1
                updateSummaryToastCountdownTime(remainingSeconds)
            }
            summaryToastCountdownJob = null
        }
    }

    private fun getSummaryToastCountdownKey(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>
    ): String {
        return "${sessionInfo.stage ?: StoryStreamSessionInfo.STAGE_OUTLINE}:" +
                "${sessionInfo.workId ?: workId.orEmpty()}"
    }

    private fun updateSummaryToastCountdownTime(seconds: Int) {
        val current = storySummaryResult.value
        val countdownText = formatToastCountdown(seconds)
        summaryToastTextTime.set(countdownText)
        storySummaryResult.value = current?.copy(
            toastTextTime = countdownText,
            toastTextEstimatedSeconds = summaryToastCountdownInitialSeconds
        ) ?: PollSummaryData(
            toastTextTime = countdownText,
            toastTextEstimatedSeconds = summaryToastCountdownInitialSeconds,
            workId = workId
        )
    }

    private fun cancelSummaryToastCountdown(
        clearState: Boolean = true,
        clearText: Boolean = true
    ) {
        summaryToastCountdownJob?.cancel()
        summaryToastCountdownJob = null
        if (clearState) {
            summaryToastCountdownKey = null
            summaryToastCountdownInitialSeconds = null
        }
        if (clearText) {
            summaryToastTextTime.set(null)
        }
    }

    private fun formatToastCountdown(seconds: Int): String? {
        return seconds.takeIf { it > 0 }?.let { "${it}s" }
    }

    private fun getSummaryToastEstimatedSeconds(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>?
    ): Int? {
        return sessionInfo?.toastTextEstimatedSeconds?.takeIf { it > 0 }
            ?: parseToastCountdownSeconds(sessionInfo?.toastTextTime)
    }

    private fun parseToastCountdownSeconds(text: String?): Int? {
        val value = text?.trim().orEmpty()
        val match = Regex("""^(\d+)\s*(?:s|秒)?$""", RegexOption.IGNORE_CASE)
            .matchEntire(value) ?: return null
        return match.groupValues[1].toIntOrNull()?.takeIf { it > 0 }
    }

    private fun updateSummaryMarkdownCutKey(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>?
    ) {
        val nextCutKey = sessionInfo?.markdownCutKey
        if (nextCutKey.isNullOrBlank()) {
            summaryMarkdownCutKey = null
            summaryMarkdownCutMatched = false
            if (summaryStreamingBuilder.isEmpty() && summaryRawStreamingBuilder.isNotEmpty()) {
                summaryStreamingBuilder.append(summaryRawStreamingBuilder.toString())
                emitSummaryStreamingText(immediate = true)
            }
            return
        }
        if (summaryMarkdownCutKey == nextCutKey) return
        summaryMarkdownCutKey = nextCutKey
        summaryMarkdownCutMatched = false
        summaryStreamingBuilder.clear()
        clearSummaryStreamingText()
    }

    private fun handleSummarySessionFail(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>?,
        error: Throwable?
    ) {
        if (sessionInfo?.isNoPower() == true) {
            handleSummarySessionNoPower(sessionInfo)
            return
        }
        val message = sessionInfo?.errorMessage ?: error?.message
        handleSummaryStreamError(StoryStreamErrorUi.BusinessFailed(message))
    }

    private fun handleSummarySessionNoPower(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>
    ) {
        summarySessionJob = null
        summaryStreamHandler?.cancel()
        cancelSummaryToastCountdown()
        clearSummaryStreamCache()
        summaryRawStreamingBuilder.clear()
        summaryStreamingBuilder.clear()
        clearSummaryStreamingText()
        summaryLastEventId = null
        storySummaryResult.value = PollSummaryData(
            genStatus = sessionInfo.genStatus,
            toastTextProgress = sessionInfo.toastTextProgress
                ?: storySummaryResult.value?.toastTextProgress,
            toastTextTime = sessionInfo.toastTextTime ?: storySummaryResult.value?.toastTextTime,
            toastTextEstimatedSeconds = sessionInfo.toastTextEstimatedSeconds
                ?: storySummaryResult.value?.toastTextEstimatedSeconds,
            workId = sessionInfo.workId ?: workId
        )
        isSummaryLoading.set(true)
        isSummaryStreaming.set(false)
        isBalanceFail.set(false)
        nextStepEnabled.set(false)
        displayChild.set(DISPLAY_LOADING)
        viewModelScope.launch {
            delay(2000)
            showRechargeDialogEvent.value = Unit
        }
    }

    private fun handleSummarySessionFinal(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>
    ) {
        summaryStreamHandler?.cancel()
        isSummaryStreaming.set(false)
        val data = StoryStreamFinalMapper.normalizeSessionSummaryData(
            data = sessionInfo.result,
            fallbackWorkId = sessionInfo.workId ?: workId,
            genStatus = sessionInfo.genStatus
        )
        applySummaryFinalData(data)
    }

    private fun resetSummaryStreamState() {
        clearSummaryStreamCache()
        cancelSummaryToastCountdown()
        summaryStreamHandler?.cancel()
        summarySessionJob?.cancel()
        novelSummaryPollingJob?.cancel()
        summaryRawStreamingBuilder.clear()
        summaryStreamingBuilder.clear()
        clearSummaryStreamingText()
        summaryLastEventId = null
        summaryMarkdownCutKey = null
        summaryMarkdownCutMatched = false
        summaryStreamCacheRestored = false
        isSummaryStreaming.set(false)
        isSummaryLoading.set(true)
        isBalanceFail.set(false)
        nextStepEnabled.set(false)
        displayChild.set(DISPLAY_LOADING)
        storySummaryResult.value = null
    }

    private fun handleSummarySessionResetIfNeeded(
        sessionInfo: StoryStreamSessionInfo<RollNovelSummaryResult>,
        requestedLastEventId: String?
    ): String? {
        return if (sessionInfo.shouldReset()) {
            clearSummaryStreamCache()
            cancelSummaryToastCountdown()
            summaryRawStreamingBuilder.clear()
            summaryStreamingBuilder.clear()
            clearSummaryStreamingText()
            summaryLastEventId = null
            summaryMarkdownCutMatched = false
            summaryStreamCacheRestored = false
            isSummaryLoading.set(true)
            isSummaryStreaming.set(false)
            displayChild.set(DISPLAY_LOADING)
            null
        } else {
            requestedLastEventId
        }
    }

    private fun subscribeSummaryStream(lastEventId: String?) {
        summaryStreamHandler?.cancel()
        isSummaryLoading.set(summaryStreamingBuilder.isEmpty())
        isSummaryStreaming.set(summaryStreamingBuilder.isNotEmpty())
        isBalanceFail.set(false)
        nextStepEnabled.set(false)
        displayChild.set(
            if (summaryStreamingBuilder.isEmpty()) DISPLAY_LOADING else DISPLAY_STREAMING
        )

        summaryStreamHandler = TaskOutputSseHandler(viewModelScope) { event ->
            when (event) {
                TaskOutputEvent.Open -> handleSummaryStreamOpen()
                is TaskOutputEvent.Chunk -> handleSummaryChunk(event)
                is TaskOutputEvent.Final -> handleSummaryFinal(event)
                is TaskOutputEvent.Error -> handleSummaryBusinessError(event)
                is TaskOutputEvent.ConnectionError -> handleSummaryConnectionError(event)
                is TaskOutputEvent.Heartbeat -> {
                    summaryLastEventId = event.lastEventId
                        .takeIf { !it.isNullOrBlank() } ?: summaryLastEventId
                }

                TaskOutputEvent.Close -> Unit
            }
        }.also { handler ->
            handler.start(
                mRepo.api.storyStreamOutlineSubscribe(
                    StoryStreamSessionBody(
                        workId = workId,
                        lastEventId = lastEventId
                    )
                )
            )
        }
    }

    private fun handleSummaryStreamOpen() {
        val hasCachedText = summaryStreamingBuilder.isNotEmpty()
        isSummaryLoading.set(!hasCachedText)
        isSummaryStreaming.set(hasCachedText)
        nextStepEnabled.set(false)
        displayChild.set(if (hasCachedText) DISPLAY_STREAMING else DISPLAY_LOADING)
    }

    private fun handleSummaryChunk(event: TaskOutputEvent.Chunk) {
        val wasDisplayEmpty = summaryStreamingBuilder.isEmpty()
        if (event.text.isNotEmpty()) {
            appendSummaryDisplayText(event.text)
        }
        summaryLastEventId = event.lastEventId
            .takeIf { !it.isNullOrBlank() } ?: summaryLastEventId
        nextStepEnabled.set(false)
        if (summaryStreamingBuilder.isEmpty()) {
            isSummaryLoading.set(true)
            isSummaryStreaming.set(false)
            displayChild.set(DISPLAY_LOADING)
            return
        }
        cancelSummaryToastCountdown()
        isSummaryLoading.set(false)
        isSummaryStreaming.set(true)
        emitSummaryStreamingText(immediate = wasDisplayEmpty)
        displayChild.set(DISPLAY_STREAMING)
    }

    private fun appendSummaryDisplayText(text: String) {
        summaryRawStreamingBuilder.append(text)
        val cutKey = summaryMarkdownCutKey
        if (cutKey.isNullOrBlank()) {
            summaryStreamingBuilder.append(text)
            return
        }
        if (summaryMarkdownCutMatched) {
            summaryStreamingBuilder.append(text)
            return
        }

        val cutIndex = summaryRawStreamingBuilder.indexOf(cutKey)
        if (cutIndex >= 0) {
            summaryMarkdownCutMatched = true
            summaryStreamingBuilder.clear()
            summaryStreamingBuilder.append(summaryRawStreamingBuilder.substring(cutIndex))
        }
    }

    private fun handleSummaryFinal(event: TaskOutputEvent.Final) {
        summaryLastEventId = event.lastEventId
            .takeIf { !it.isNullOrBlank() } ?: summaryLastEventId
        isSummaryStreaming.set(false)
        val data = StoryStreamFinalMapper.toPollSummaryData(
            eventData = event.data,
            fallbackWorkId = workId
        )
        applySummaryFinalData(data)
    }

    private fun applySummaryFinalData(data: PollSummaryData) {
        cancelSummaryStreamingTextEmit()
        cancelSummaryToastCountdown()
        data.result?.let {
            nextStepEnabled.set(data.isSensitive() == false)
            mWorkName.value = it.workTitle ?: ""
        }
        storySummaryResult.value = data
        isSummaryLoading.set(false)
        isSummaryStreaming.set(false)
        displayChild.set(DISPLAY_CONTENT)
        clearSummaryStreamCache()
    }

    private fun handleSummaryBusinessError(event: TaskOutputEvent.Error) {
        summaryLastEventId = event.lastEventId
            .takeIf { !it.isNullOrBlank() } ?: summaryLastEventId
        handleSummaryStreamError(StoryStreamErrorMapper.fromBusinessError(event))
    }

    private fun handleSummaryConnectionError(event: TaskOutputEvent.ConnectionError) {
        Log.d(TAG, "outline stream disconnected: ${event.throwable?.message}")
        summaryStreamHandler?.cancel()
        isSummaryStreaming.set(false)
        startSummarySession(summaryLastEventId)
    }

    private fun handleSummaryStreamError(errorUi: StoryStreamErrorUi) {
        val message = when (errorUi) {
            is StoryStreamErrorUi.BusinessFailed -> errorUi.message
            is StoryStreamErrorUi.ConnectionBroken -> errorUi.message
        }
        message?.takeIf { it.isNotBlank() }?.showToast()
        isSummaryStreaming.set(false)
        isSummaryLoading.set(false)
        nextStepEnabled.set(false)
        displayChild.set(DISPLAY_FAIL)
        cancelSummaryStreamingTextEmit()
        cancelSummaryToastCountdown()
        clearSummaryStreamCache()
    }

    private fun restoreSummaryStreamCacheIfNeeded() {
        if (workId.isNullOrEmpty()) return
        if (!summaryLastEventId.isNullOrBlank() ||
            summaryRawStreamingBuilder.isNotEmpty() ||
            summaryStreamingBuilder.isNotEmpty()
        ) return

        val cache = Config.get().getStoryOutlineStreamCache(workId) ?: return
        summaryLastEventId = cache.lastEventId
        summaryRawStreamingBuilder.clear()
        summaryRawStreamingBuilder.append(cache.rawText.orEmpty())
        summaryStreamingBuilder.clear()
        val restoredDisplayText = cache.displayText.orEmpty().ifEmpty {
            if (cache.markdownCutKey.isNullOrBlank()) cache.rawText.orEmpty() else ""
        }
        summaryStreamingBuilder.append(restoredDisplayText)
        summaryMarkdownCutKey = cache.markdownCutKey?.takeIf { it.isNotBlank() }
        summaryMarkdownCutMatched = cache.markdownCutMatched
        summaryStreamCacheRestored = restoredDisplayText.isNotEmpty()
        emitSummaryStreamingText(immediate = true)
    }

    private fun emitSummaryStreamingText(immediate: Boolean = false) {
        if (immediate) {
            cancelSummaryStreamingTextEmit()
            summaryStreamingText.value = summaryStreamingBuilder.toString()
            return
        }
        if (summaryStreamingTextEmitJob?.isActive == true) return
        summaryStreamingTextEmitJob = viewModelScope.launch {
            delay(SUMMARY_TEXT_EMIT_INTERVAL_MS)
            summaryStreamingText.value = summaryStreamingBuilder.toString()
            summaryStreamingTextEmitJob = null
        }
    }

    private fun clearSummaryStreamingText() {
        cancelSummaryStreamingTextEmit()
        summaryStreamingText.value = ""
    }

    private fun cancelSummaryStreamingTextEmit() {
        summaryStreamingTextEmitJob?.cancel()
        summaryStreamingTextEmitJob = null
    }

    private fun saveSummaryStreamCacheIfNeeded() {
        val cacheWorkId = workId ?: return
        if (!isSummaryLoading.get() && !isSummaryStreaming.get()) return
        if (summaryLastEventId.isNullOrBlank() &&
            summaryRawStreamingBuilder.isEmpty() &&
            summaryStreamingBuilder.isEmpty()
        ) return

        Config.get().storeStoryOutlineStreamCache(
            cacheWorkId,
            StoryOutlineStreamCache(
                lastEventId = summaryLastEventId,
                rawText = summaryRawStreamingBuilder.toString(),
                displayText = summaryStreamingBuilder.toString(),
                markdownCutKey = summaryMarkdownCutKey,
                markdownCutMatched = summaryMarkdownCutMatched
            )
        )
    }

    private fun clearSummaryStreamCache() {
        summaryStreamCacheRestored = false
        Config.get().clearStoryOutlineStreamCache(workId)
    }

    fun consumeSummaryStreamCacheRestored(): Boolean {
        val restored = summaryStreamCacheRestored
        summaryStreamCacheRestored = false
        return restored
    }

    /**
     * 轮询小说文档模式下的故事梗概
     */
    fun pollSummary() {
        if (novelSummaryPollingJob?.isActive == true) return
        summarySessionJob?.cancel()
        summaryStreamHandler?.cancel()
        clearSummaryStreamCache()
        summaryRawStreamingBuilder.clear()
        summaryStreamingBuilder.clear()
        clearSummaryStreamingText()
        summaryLastEventId = null
        summaryMarkdownCutKey = null
        summaryMarkdownCutMatched = false
        summaryStreamCacheRestored = false
        isSummaryLoading.set(true)
        isBalanceFail.set(false)
        isSummaryStreaming.set(false)
        nextStepEnabled.set(false)
        displayChild.set(DISPLAY_LOADING)
        storySummaryResult.value = null

        novelSummaryPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            apiBlock = {
                if (isStoryDocumentsUploadMode()) {
                    mRepo.api.pollNovelDocsSummary(WorkIdBody(workId))
                } else {
                    mRepo.api.pollSummaryResult(WorkIdBody(workId))
                }
            },
            checkState = { response ->
                val data = response?.data
                when {
                    data?.isSuccess() == true -> PollingState.SUCCESS
                    data?.isSensitive() == true -> PollingState.SUCCESS
                    data?.isFail() == true -> PollingState.FAIL
                    data?.isNoPower() == true -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                response?.data?.result?.let {
                    nextStepEnabled.set(response.data?.isSensitive() == false)
                    mWorkName.value = it.workTitle ?: ""
                }
                storySummaryResult.value = response?.data
                isSummaryLoading.set(false)
                displayChild.set(DISPLAY_CONTENT)
            },
            onFail = { response, _ ->
                if (response?.data?.isNoPower() == true) {
                    storySummaryResult.value = response.data
                    // 算力不足：延迟2s后通知Activity弹充值弹窗，期间保持loading状态
                    viewModelScope.launch {
                        delay(2000)
                        showRechargeDialogEvent.value = Unit
                    }
                } else {
                    displayChild.set(DISPLAY_FAIL)
                    storySummaryResult.value = response?.data
                    isSummaryLoading.set(false)
                }
            },
            onPolling = { response ->
                storySummaryResult.value = response?.data
            }
        )
    }

    /**
     * 充值弹窗结果回调（由Activity在弹窗关闭后调用）
     * @param recharged true=充值成功，继续轮询；false=取消或失败，显示错误UI
     */
    fun onRecharged(recharged: Boolean) {
        if (recharged) {
            if (useOldSummaryBusiness()) {
                pollSummary()
            } else {
                startSummarySession()
            }
        } else {
            isBalanceFail.set(true)
        }
    }

    /**
     * 灵感故事梗概失败重新创建任务
     */
    fun retrySummary(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({ mRepo.api.retrySummaryTaskFromInspiration(ContinueTaskBody(workId = workId)) }) {
            onSuccess?.invoke()
        }
    }

    fun reuploadStory(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({ mRepo.api.novelUploadDelete(WorkIdBody(workId)) }) {
            onSuccess?.invoke()
        }
    }

    fun saveTitle(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({
            mRepo.api.updateWorkInfo(
                WorkInfoUpdateBody(
                    workId,
                    workTitle = mWorkName.value
                )
            )
        }) {
            onSuccess?.invoke()
        }
    }
}
