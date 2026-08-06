package com.kuaigeglobal.epnova.feature.creator.generate

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.ext.PollingState
import com.kuaigeglobal.epnova.core.ext.launchPolling
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryStreamSessionInfo
import com.kuaigeglobal.epnova.core.network.apis.request.ContinueTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.PollScriptBody
import com.kuaigeglobal.epnova.core.network.apis.request.PollScriptData
import com.kuaigeglobal.epnova.core.network.apis.request.SaveScriptBody
import com.kuaigeglobal.epnova.core.network.apis.request.StoryStreamSessionBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.network.sse.TaskOutputEvent
import com.kuaigeglobal.epnova.core.network.sse.TaskOutputSseHandler
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 故事剧本生成
 */
class DramaGenerateStep2ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep2ViewModel"
        private const val SESSION_POLL_INTERVAL_MS = 2000L
        private const val TOAST_COUNTDOWN_INTERVAL_MS = 1000L
    }

    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(5000 * Constants.INTERNATIONALIZATION_RATIO)
    }
    val input by lazy { MutableLiveData<String?>() }

    //业务类型
    var model: Int? = null
        set(value) {
            field = value
            useOldScriptBusiness.set(!isInspirationMode())
        }
    var workType: Int? = null

    //章节序号
    var chapterIndex: Int? = 1

    //作品ID
    var workId = MutableLiveData<String>()
    var continueInputContent: String? = null

    //剧集id
    var episodeId: String? = null

    //上一集的剧集id,只有续写的时候才有
    var previousStoryId: String? = null

    // 作品名称
    var mWorkName = MutableLiveData<String>()

    val nextStepEnabled by lazy { ObservableBoolean(false) }
    val outputComplete by lazy { MutableLiveData(false) }
    val outputFail by lazy { ObservableBoolean(false) }
    val isScriptLoading = ObservableBoolean(false)
    val isScriptStreaming = ObservableBoolean(false)

    var scriptLastEventId: String? = null
    private var scriptStreamHandler: TaskOutputSseHandler? = null
    private val scriptStreamingText = StringBuilder()

    /**
     * 小说文档模式==》故事内容结果
     */
    var storyScriptResult = MutableLiveData<PollScriptData?>()
    val scriptToastTextTime = ObservableField<String>()
    var episodeTitle = ObservableField<String>()
    val useOldScriptBusiness = ObservableBoolean(true)

    fun isNovelDocuments() =
        model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS || model == DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS

    fun isInspirationMode() =
        model == DramaGenerateConstants.MODEL_INSPIRATION || model == DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION

    fun createScriptTask(retry: Boolean = false, callBack: () -> Unit) {
        if (model == DramaGenerateConstants.MODEL_STORY_SINGLE && !retry) {
            callBack.invoke()
            return
        }

        requestWithLoading({
            val body =
                ContinueTaskBody(
                    workInspiration = if (model == DramaGenerateConstants.MODEL_CONTINUE_STORY) continueInputContent else null,
                    workId = workId.value,
                    storyId = episodeId,
                    forceCreation = retry
                )
            when (model) {
                DramaGenerateConstants.MODEL_STORY_DOCUMENTS, DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS -> mRepo.api.continueTaskFromNovelDoc(
                    body
                )

                DramaGenerateConstants.MODEL_STORY_SINGLE -> mRepo.api.retryTaskFromSingleEpisode(body)
                DramaGenerateConstants.MODEL_STORY, DramaGenerateConstants.MODEL_CONTINUE_STORY -> mRepo.api.continueTaskFromNovel(body)
                DramaGenerateConstants.MODEL_INSPIRATION, DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION -> mRepo.api.createTaskFromInspiration(
                    body
                )

                else -> null
            }
        }, onSuccess = {
            resetPollingState()
            callBack.invoke()
        }, onError = {
            handlePollingFail(null)
        })
    }

    private var scriptPollingJob: Job? = null
    private var scriptSessionJob: Job? = null
    private var scriptToastCountdownJob: Job? = null
    private var scriptToastCountdownKey: String? = null
    private var scriptToastCountdownInitialSeconds: Int? = null

    /**
     * 只关闭前端 session、SSE 和旧结果轮询，不取消后端生成任务。
     */
    fun cancelScriptStream() {
        Log.d(
            TAG,
            "cancel script stream workId=${workId.value}, storyId=$episodeId, lastEventId=$scriptLastEventId"
        )
        scriptSessionJob?.cancel()
        scriptStreamHandler?.cancel()
        scriptPollingJob?.cancel()
        cancelScriptToastCountdown()
        isScriptStreaming.set(false)
    }

    /**
     * 新流式入口：先轮询 session，待流可订阅后再建立 SSE。
     */
    fun startScriptSession(
        lastEventId: String? = scriptLastEventId,
        resetContent: Boolean = false
    ) {
        if (useOldScriptBusiness.get()) {
            pollOldScript()
            return
        }
        if (scriptSessionJob?.isActive == true) return

        var pollingLastEventId = if (resetContent) null else lastEventId
        if (resetContent) {
            resetPollingState()
        } else {
            outputFail.set(false)
            outputComplete.value = false
            nextStepEnabled.set(false)
            isScriptLoading.set(input.value.isNullOrEmpty())
        }

        scriptStreamHandler?.cancel()
        scriptPollingJob?.cancel()
        scriptSessionJob = launchPolling(
            intervalMs = SESSION_POLL_INTERVAL_MS,
            retryOnNetworkError = true,
            showToastError = false,
            apiBlock = {
                mRepo.api.storyStreamScriptSession(
                    StoryStreamSessionBody(
                        workId = workId.value,
                        lastEventId = pollingLastEventId,
                        storyId = episodeId
                    )
                )
            },
            checkState = { response ->
                val sessionInfo = response?.data
                when {
                    sessionInfo?.hasFinalResult() == true -> PollingState.SUCCESS
                    sessionInfo?.canSubscribe() == true -> PollingState.SUCCESS
                    sessionInfo?.isFailed() == true || sessionInfo?.hasNoTask() == true -> PollingState.FAIL
                    sessionInfo?.shouldPollSession() == true -> PollingState.CONTINUE
                    else -> PollingState.FAIL
                }
            },
            onSuccess = { response ->
                handleScriptSessionReady(response?.data, pollingLastEventId)
            },
            onFail = { response, error ->
                handleScriptSessionFail(response?.data, error)
            },
            onPolling = { response ->
                pollingLastEventId = handleScriptSessionPolling(response?.data, pollingLastEventId)
            }
        )
    }

    private fun handleScriptSessionReady(
        sessionInfo: StoryStreamSessionInfo<PollScriptData>?,
        requestedLastEventId: String?
    ) {
        episodeId = sessionInfo?.storyId ?: episodeId
        scriptSessionJob = null
        if (sessionInfo?.hasFinalResult() == true) {
            handleScriptSessionFinal(sessionInfo)
            return
        }
        val nextLastEventId = sessionInfo?.let {
            handleScriptSessionResetIfNeeded(it, requestedLastEventId)
        }
        updateScriptSessionToast(sessionInfo)
        subscribeScriptStream(nextLastEventId)
    }

    private fun handleScriptSessionPolling(
        sessionInfo: StoryStreamSessionInfo<PollScriptData>?,
        requestedLastEventId: String?
    ): String? {
        episodeId = sessionInfo?.storyId ?: episodeId
        val nextLastEventId = if (sessionInfo?.shouldReset() == true) {
            handleScriptSessionResetIfNeeded(sessionInfo, requestedLastEventId)
        } else {
            requestedLastEventId
        }
        updateScriptSessionToast(sessionInfo)
        return nextLastEventId
    }

    private fun updateScriptSessionToast(sessionInfo: StoryStreamSessionInfo<PollScriptData>?) {
        if (sessionInfo?.toastTextProgress.isNullOrBlank() &&
            sessionInfo?.toastTextTime.isNullOrBlank() &&
            sessionInfo?.toastTextEstimatedSeconds == null
        ) return

        val current = storyScriptResult.value
        val estimatedSeconds = getScriptToastEstimatedSeconds(sessionInfo)
        val nextToastTextTime = if (estimatedSeconds != null) {
            val countdownKey = getScriptToastCountdownKey(sessionInfo)
            val isSameCountdown = scriptToastCountdownKey == countdownKey &&
                    scriptToastCountdownInitialSeconds == estimatedSeconds
            scriptToastTextTime.get()
                ?: if (isSameCountdown) null else formatToastCountdown(estimatedSeconds)
        } else {
            sessionInfo?.toastTextTime ?: scriptToastTextTime.get() ?: current?.toastTextTime
        }
        scriptToastTextTime.set(nextToastTextTime)
        storyScriptResult.value = PollScriptData(
            workId = current?.workId ?: sessionInfo?.workId ?: workId.value,
            storyId = current?.storyId ?: sessionInfo?.storyId ?: episodeId,
            sortNo = current?.sortNo ?: chapterIndex,
            genStatus = current?.genStatus,
            toastTextProgress = sessionInfo?.toastTextProgress ?: current?.toastTextProgress,
            toastTextTime = nextToastTextTime,
            toastTextEstimatedSeconds = sessionInfo?.toastTextEstimatedSeconds
                ?: current?.toastTextEstimatedSeconds,
            storyContent = current?.storyContent
        )
        startScriptToastCountdownIfNeeded(sessionInfo)
    }

    private fun startScriptToastCountdownIfNeeded(
        sessionInfo: StoryStreamSessionInfo<PollScriptData>?
    ) {
        val info = sessionInfo ?: return
        val estimatedSeconds = getScriptToastEstimatedSeconds(info) ?: return
        val countdownKey = getScriptToastCountdownKey(info)
        val isSameCountdown = scriptToastCountdownKey == countdownKey &&
                scriptToastCountdownInitialSeconds == estimatedSeconds
        if (isSameCountdown) {
            val currentToastTime = scriptToastTextTime.get()
            if (scriptToastCountdownJob?.isActive == true || currentToastTime.isNullOrBlank()) return
        }

        cancelScriptToastCountdown(clearState = false, clearText = false)
        scriptToastCountdownKey = countdownKey
        scriptToastCountdownInitialSeconds = estimatedSeconds
        updateScriptToastCountdownTime(estimatedSeconds)
        scriptToastCountdownJob = viewModelScope.launch {
            var remainingSeconds = estimatedSeconds
            while (remainingSeconds > 0) {
                delay(TOAST_COUNTDOWN_INTERVAL_MS)
                remainingSeconds -= 1
                updateScriptToastCountdownTime(remainingSeconds)
            }
            scriptToastCountdownJob = null
        }
    }

    private fun getScriptToastCountdownKey(
        sessionInfo: StoryStreamSessionInfo<PollScriptData>
    ): String {
        return "${sessionInfo.stage ?: StoryStreamSessionInfo.STAGE_SCRIPT}:" +
                "${sessionInfo.storyId ?: sessionInfo.workId ?: episodeId.orEmpty()}"
    }

    private fun updateScriptToastCountdownTime(seconds: Int) {
        val current = storyScriptResult.value
        val countdownText = formatToastCountdown(seconds)
        scriptToastTextTime.set(countdownText)
        storyScriptResult.value = current?.copy(
            toastTextTime = countdownText,
            toastTextEstimatedSeconds = scriptToastCountdownInitialSeconds
        ) ?: PollScriptData(
            workId = workId.value,
            storyId = episodeId,
            sortNo = chapterIndex,
            toastTextTime = countdownText,
            toastTextEstimatedSeconds = scriptToastCountdownInitialSeconds
        )
    }

    private fun cancelScriptToastCountdown(
        clearState: Boolean = true,
        clearText: Boolean = true
    ) {
        scriptToastCountdownJob?.cancel()
        scriptToastCountdownJob = null
        if (clearState) {
            scriptToastCountdownKey = null
            scriptToastCountdownInitialSeconds = null
        }
        if (clearText) {
            scriptToastTextTime.set(null)
        }
    }

    private fun formatToastCountdown(seconds: Int): String? {
        return seconds.takeIf { it > 0 }?.let { "${it}s" }
    }

    private fun getScriptToastEstimatedSeconds(
        sessionInfo: StoryStreamSessionInfo<PollScriptData>?
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

    private fun handleScriptSessionFail(
        sessionInfo: StoryStreamSessionInfo<PollScriptData>?,
        error: Throwable?
    ) {
        episodeId = sessionInfo?.storyId ?: episodeId
        val message = sessionInfo?.errorMessage ?: error?.message
        handleScriptStreamError(StoryStreamErrorUi.BusinessFailed(message))
    }

    private fun handleScriptSessionFinal(sessionInfo: StoryStreamSessionInfo<PollScriptData>) {
        scriptStreamHandler?.cancel()
        isScriptStreaming.set(false)
        val data = StoryStreamFinalMapper.normalizeSessionScriptData(
            data = sessionInfo.result,
            fallbackWorkId = sessionInfo.workId ?: workId.value,
            fallbackStoryId = sessionInfo.storyId ?: episodeId,
            fallbackSortNo = chapterIndex
        )
        handlePollingSuccess(data)
    }

    private fun handleScriptSessionResetIfNeeded(
        sessionInfo: StoryStreamSessionInfo<PollScriptData>,
        requestedLastEventId: String?
    ): String? {
        return if (sessionInfo.shouldReset()) {
            Log.d(TAG, "script session reset, oldLastEventId=$requestedLastEventId")
            cancelScriptToastCountdown()
            scriptLastEventId = null
            scriptStreamingText.clear()
            input.value = ""
            isScriptLoading.set(true)
            isScriptStreaming.set(false)
            null
        } else {
            requestedLastEventId
        }
    }

    private fun subscribeScriptStream(lastEventId: String?) {
        scriptStreamHandler?.cancel()
        isScriptLoading.set(input.value.isNullOrEmpty())
        isScriptStreaming.set(true)
        outputFail.set(false)
        outputComplete.value = false
        nextStepEnabled.set(false)

        scriptStreamHandler = TaskOutputSseHandler(viewModelScope) { event ->
            when (event) {
                is TaskOutputEvent.Chunk -> handleScriptChunk(event)
                is TaskOutputEvent.Final -> handleScriptFinal(event)
                is TaskOutputEvent.Error -> handleScriptBusinessError(event)
                is TaskOutputEvent.ConnectionError -> handleScriptConnectionError(event)
                is TaskOutputEvent.Heartbeat -> {
                    scriptLastEventId = event.lastEventId
                        .takeIf { !it.isNullOrBlank() } ?: scriptLastEventId
                }

                TaskOutputEvent.Close,
                TaskOutputEvent.Open -> Unit
            }
        }.also { handler ->
            handler.start(
                mRepo.api.storyStreamScriptSubscribe(
                    StoryStreamSessionBody(
                        workId = workId.value,
                        lastEventId = lastEventId,
                        storyId = episodeId
                    )
                )
            )
        }
    }

    private fun handleScriptChunk(event: TaskOutputEvent.Chunk) {
        if (scriptStreamingText.isEmpty() && !input.value.isNullOrEmpty()) {
            scriptStreamingText.append(input.value.orEmpty())
        }
        if (event.text.isNotEmpty()) {
            scriptStreamingText.append(event.text)
        }
        scriptLastEventId = event.lastEventId
            .takeIf { !it.isNullOrBlank() } ?: scriptLastEventId
        isScriptStreaming.set(true)
        nextStepEnabled.set(false)
        outputFail.set(false)
        outputComplete.value = false
        if (scriptStreamingText.isEmpty()) {
            isScriptLoading.set(true)
            return
        }
        cancelScriptToastCountdown()
        input.value = scriptStreamingText.toString()
        isScriptLoading.set(false)
    }

    private fun handleScriptFinal(event: TaskOutputEvent.Final) {
        scriptLastEventId = event.lastEventId
            .takeIf { !it.isNullOrBlank() } ?: scriptLastEventId
        isScriptStreaming.set(false)
        val data = StoryStreamFinalMapper.toPollScriptData(
            eventData = event.data,
            fallbackWorkId = workId.value,
            fallbackStoryId = episodeId,
            fallbackSortNo = chapterIndex
        )
        handlePollingSuccess(data)
    }

    private fun handleScriptBusinessError(event: TaskOutputEvent.Error) {
        scriptLastEventId = event.lastEventId
            .takeIf { !it.isNullOrBlank() } ?: scriptLastEventId
        handleScriptStreamError(StoryStreamErrorMapper.fromBusinessError(event))
    }

    private fun handleScriptConnectionError(event: TaskOutputEvent.ConnectionError) {
        Log.d(TAG, "script stream disconnected: ${event.throwable?.message}")
        scriptStreamHandler?.cancel()
        isScriptStreaming.set(false)
        startScriptSession(scriptLastEventId)
    }

    private fun handleScriptStreamError(errorUi: StoryStreamErrorUi) {
        val message = when (errorUi) {
            is StoryStreamErrorUi.BusinessFailed -> errorUi.message
            is StoryStreamErrorUi.ConnectionBroken -> errorUi.message
        }
        message?.takeIf { it.isNotBlank() }?.showToast()
        isScriptStreaming.set(false)
        isScriptLoading.set(false)
        nextStepEnabled.set(false)
        outputComplete.value = false
        outputFail.set(true)
        cancelScriptToastCountdown()
    }

    /**
     * 旧剧本业务轮询；文档上传走文档专用接口，其他旧模式走通用接口。
     */
    fun pollOldScript() {
        if (scriptPollingJob?.isActive == true) return

        resetPollingState()
        scriptPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            apiBlock = {
                val body = PollScriptBody(workId.value, episodeId, workType = workType)
                if (isNovelDocuments()) {
                    mRepo.api.pollNovelScript(body)
                } else {
                    mRepo.api.pollScriptResult(body)
                }
            },
            checkState = { response ->
                when {
                    response?.data?.isSuccess() == true -> PollingState.SUCCESS
                    response?.data?.isFail() == true -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                handlePollingSuccess(response?.data)
            },
            onFail = { response, _ ->
                handlePollingFail(response?.data)
            }
        )
    }

    private fun resetPollingState() {
        scriptStreamHandler?.cancel()
        scriptSessionJob?.cancel()
        scriptPollingJob?.cancel()
        cancelScriptToastCountdown()
        scriptStreamingText.clear()
        scriptLastEventId = null
        input.value = ""
        isScriptLoading.set(true)
        isScriptStreaming.set(false)
        nextStepEnabled.set(false)
        outputFail.set(false)
        outputComplete.value = false
        storyScriptResult.value = null
    }

    private fun handlePollingSuccess(data: PollScriptData?) {
        cancelScriptToastCountdown()
        data?.storyContent?.let {
            nextStepEnabled.set(true)
            input.value = it
            scriptStreamingText.clear()
            scriptStreamingText.append(it)
        }
        episodeId = data?.storyId
        chapterIndex = data?.sortNo
        storyScriptResult.value = data
        outputComplete.value = true
        isScriptLoading.set(false)
        isScriptStreaming.set(false)
    }

    private fun handlePollingFail(data: PollScriptData?) {
        cancelScriptToastCountdown()
        nextStepEnabled.set(false)
        input.value = ""
        storyScriptResult.value = data
        isScriptLoading.set(false)
        isScriptStreaming.set(false)
        outputFail.set(true)
    }

    @ColorInt
    fun getLimitColor(length: Int?): Int {
        val isMax = length == maxInputLength.get()
        if (isMax) {
            showToast(R.string.input_limit_reached)
        }
        val attr =
            if (isMax) R.attr.funcColorFailure else R.attr.textColor4
        return topActivity?.getThemeColor(attr) ?: Color.BLACK
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        scriptPollingJob?.cancel()
        scriptSessionJob?.cancel()
        scriptStreamHandler?.cancel()
        scriptToastCountdownJob?.cancel()
    }

    /**
     * 确认生成
     * @param onSuccess 成功回调，返回作品ID
     */
    fun confirmGenerate(
        onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)? = null
    ) {
        launchWithLoading {
            response(mRepo.api.saveScript(SaveScriptBody(workId.value, episodeId, input.value))) {
                generateAllAssetDescription(workId.value, episodeId, onSuccess)
            }
        }
    }

    private fun generateAllAssetDescription(
        workId: String?,
        storyId: String?,
        onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?
    ) {
        launchWithLoading {
            response(mRepo.api.generateAllAssetDescription(WorkBody(workId, storyId))) {
                onSuccess?.invoke(workId, storyId, data?.taskId)
            }
        }
    }
}
