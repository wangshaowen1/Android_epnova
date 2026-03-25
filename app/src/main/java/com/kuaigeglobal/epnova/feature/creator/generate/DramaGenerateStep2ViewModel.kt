package com.kuaigeglobal.epnova.feature.creator.generate

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.ext.PollingState
import com.kuaigeglobal.epnova.core.ext.launchPolling
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.ContinueTaskBody
import com.kuaigeglobal.epnova.core.network.apis.request.PollScriptBody
import com.kuaigeglobal.epnova.core.network.apis.request.PollScriptData
import com.kuaigeglobal.epnova.core.network.apis.request.SaveScriptBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job

/**
 * 故事剧本生成
 */
class DramaGenerateStep2ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep2ViewModel"
    }

    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(5000)
    }
    val input by lazy { MutableLiveData<String?>() }

    //业务类型
    var model: Int? = null
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

    /**
     * 小说文档模式==》故事内容结果
     */
    var storyScriptResult = MutableLiveData<PollScriptData?>()
    var episodeTitle = ObservableField<String>()

    fun isNovelDocuments() =
        model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS || model == DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS

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

    /**
     * 轮询小说文档模式下的故事内容
     */
    fun pollNovelScript() {
        if (scriptPollingJob?.isActive == true) return

        resetPollingState()

        scriptPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            apiBlock = {
                //作品类型：1-灵感创作;2-小说改编;
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
        input.value = ""
        isScriptLoading.set(true)
        nextStepEnabled.set(false)
        outputFail.set(false)
        outputComplete.value = false
        storyScriptResult.value = null
    }

    private fun handlePollingSuccess(data: PollScriptData?) {
        data?.storyContent?.let {
            nextStepEnabled.set(true)
            input.value = it
        }
        episodeId = data?.storyId
        chapterIndex = data?.sortNo
        storyScriptResult.value = data
        outputComplete.value = true
        isScriptLoading.set(false)
    }

    private fun handlePollingFail(data: PollScriptData?) {
        nextStepEnabled.set(false)
        input.value = ""
        storyScriptResult.value = data
        isScriptLoading.set(false)
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