package com.kuaigeglobal.epnova.feature.creator.continuewriting

import android.graphics.Color
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SaveSequelNovelStoryInfo
import com.kuaigeglobal.epnova.core.network.apis.request.AIExpandBody
import com.kuaigeglobal.epnova.core.network.apis.request.LatestEpisodeBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveSequelNovelStoryBody
import com.kuaigeglobal.epnova.core.network.sse.SSEStreamHandler
import com.kuaigeglobal.epnova.core.network.sse.processor.PlainTextSseProcessor
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

class ContinueWritingViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.untitled_title.getResString(),
        background = Color.TRANSPARENT,
    )

    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val workName by lazy { mBundle.getString(Constants.Extra.NAME) }
    val input by lazy { MutableLiveData(Global.continueWritingContent[workId]) }

    val episodeInfo = MutableLiveData<EpisodeInfo>()

    val textLimitNum by lazy { Config.get().appConfig.shortDramas }
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(5000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val isVip = ObservableBoolean(true)
    val isSSEStreaming = ObservableBoolean(false)

    //标题
    val combinationTitle = ObservableField("")

    private var sseHandler: SSEStreamHandler? = null


    override fun onCreate() {
        super.onCreate()
        workName?.let {
            mToolbarVM.mTitle.set(R.string.continued_creation_s.getResString(it))
        }
        getEpisodeInfo()
    }

    /**
     *获取剧集上一集剧本详情
     */
    fun getEpisodeInfo(function: (() -> Unit)? = null) {
        launchWithLoading {
            response(
                mRepo.api.latestEpisode(
                    LatestEpisodeBody(
                        workType = LatestEpisodeBody.WORK_TYPE_NOVEL,
                        workId = workId,
                        storyId = storyId
                    )
                )
            ) {
                episodeInfo.value = data
                gradeAssembly()
                function?.invoke()
            }
        }
    }

    fun sseGenerate() {
        promptGenerating.value = true to (R.string.ai_expanding.getResString())
        val sseFlow = mRepo.api.sseContentAIExpand(
            AIExpandBody(
                storyId = episodeInfo.value?.storyId,
                workId = workId,
//                bizType = DramaGenerateConstants.MODEL_CONTINUE_STORY_EXPAND,
                workInspiration = input.value,
            )
        )
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = PlainTextSseProcessor(
                scope = viewModelScope,
                onStart = {
                    isSSEStreaming.set(true)
                    promptGenerating.value = false to ""
                    Logger.d("[SSE]", "Open")
                    input.value = ""
                },
                onUpdate = { fullContent ->
                    Logger.d("[SSE]", "Update: $fullContent")
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    input.value = fullContent
                },
                onComplete = {
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                    isSSEStreaming.set(false)
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    // 根据需要处理特定错误
                    promptGenerating.value = false to ""
                    parseMessage(e)?.showToast()
                    isSSEStreaming.set(false)
                })
        )
        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = $input")
        sseHandler?.start(sseFlow)
    }


    private fun gradeAssembly() {
        val episode = episodeInfo.value
        if (episode == null) {
            combinationTitle.set("")
            return
        }
        combinationTitle.set(R.string.episode_number_format.getResString(episode.sortNo))
    }


    /**
     *小说-保存续创剧集（只有小说有）
     */
    fun saveSequelNovelStory(function: ((SaveSequelNovelStoryInfo) -> Unit)? = null) {
        launchWithLoading {
            response(
                mRepo.api.saveSequelNovelStory(
                    SaveSequelNovelStoryBody(
                        storySummary = input.value,
                        workId = workId,
                        storyId = storyId
                    )
                )
            ) {
                function?.invoke(data)
            }
        }
    }


    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
    }
}