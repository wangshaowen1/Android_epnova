package com.kuaigeglobal.epnova.feature.editor

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.core.network.apis.request.GenerateStoryboardBody
import com.kuaigeglobal.epnova.core.network.apis.request.GenerateSubtitleBody
import com.kuaigeglobal.epnova.core.network.apis.request.StoryboardIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.TaskIdBody
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.text.get
import kotlin.text.set

class ItemStoryboardVM(
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    data: SimpleStoryboardInfo?,
    private val onVideoGenerated: ((ItemStoryboardVM) -> Unit)? = null
) : AppItemViewModel<SimpleStoryboardInfo>(data) {
    val selected by lazy { ObservableBoolean(false) }
    private var loopGenStatusJob: Job? = null
    private var loopSubtitleGenStatusJob: Job? = null
    private var cleared = false

    init {
        if (data?.videoGenerating == true) {
            loopGenStatus()
        } else if (data?.videoGenerated == true) {
            loopGenSubtitleStatus()
        }
    }

    fun release() {
        cleared = true
        loopGenStatusJob?.cancel()
        loopSubtitleGenStatusJob?.cancel()
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }

    /**
     * [枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    fun genStatusBig(info: SimpleStoryboardInfo?) =
        when {
            info?.videoGenerating == true -> R.string.video_generating.getResString()
            info?.videoGenerateFailed == true -> R.string.video_generation_failed.getResString()
            else -> null
        }

    /**
     * [枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    fun genStatus(info: SimpleStoryboardInfo?) =
        when {
            info?.videoGenerating == true -> R.string.in_progress.getResString()
            info?.videoGenerateFailed == true -> R.string.loading_failed.getResString()
            else -> R.string.frame_s.getResString(info?.sortNo)
        }

    fun generate(storyInfo: EpisodeInfo?) {
        val storyboard = data.get() ?: return
        if (storyboard.videoGenerating) return
        val preStatus = data.get()?.genStatusVideo
        data.get()?.genStatusVideo = 1
        data.notifyChange()
        launch {
            val body = GenerateStoryboardBody(
                storyboardId = storyboard.storyboardId,
                workId = workId,
                storyId = storyId,
                operateType = if (storyInfo?.isSynthesised == true) 3 else 2
            )
            response(
                bean = ApiFactory.apiService.workStoryboardVideoSingleGenerate(body),
                result = {
                    val storyboard = this@ItemStoryboardVM.data.get() ?: SimpleStoryboardInfo()
                    storyboard.taskId = data?.taskId
                    storyboard.genStatusVideo = data?.genStatus
                    this@ItemStoryboardVM.data.set(storyboard)
                    this@ItemStoryboardVM.data.notifyChange()
                    loopGenStatus()
                },
                error = {
                    this?.msg?.showToast()
                    val item = data.get()?.copy()
                    item?.genStatusVideo = preStatus
                    data.set(item)
                })
        }
    }

    private fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        if (cleared) return
        val storyboard = data.get()
        val taskId = storyboard?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            val wasGenerating = storyboard.videoGenerating
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workStoryboardSingleStatus(TaskIdBody(taskId, 72)).data }
                storyboard.genStatusVideo = taskStatus?.genStatus
                storyboard.videoUrl = taskStatus?.result
                if (taskStatus?.genStatus == 3) {
                    withContext(Dispatchers.Main) {
                        taskStatus.errorMsg?.showToast()
                    }
                }
                data.notifyChange()
            }
            if (storyboard.videoGenerating) {
                delay(3000)
                loopGenStatus()
            } else if (storyboard.videoGenerated) {
                if (wasGenerating) {
                    // 轮询只更新数据，当前分镜需重新走 select 才会更新播放器媒体项
                    onVideoGenerated?.invoke(this@ItemStoryboardVM)
                }
                loopGenSubtitleStatus()
            }
        }
    }

    fun generateSubtitle() {
        val storyboard = data.get()
        val storyboardId = storyboard?.storyboardId
        if (storyboardId.isNullOrEmpty()) return
        storyboard.subtitle?.genStatus = 1
        data.notifyChange()
        launch {
            val body = GenerateSubtitleBody(storyboardId, storyboard.videoUrl, User.get().userId, workId, storyId)
            response(
                bean = ApiFactory.apiService.generateSubtitle(body),
                result = { loopGenSubtitleStatus() },
                error = {
                    this?.msg?.showToast()
                    storyboard.subtitle?.genStatus = 3
                    data.notifyChange()
                })
        }
    }

    private fun loopGenSubtitleStatus() {
        loopSubtitleGenStatusJob?.cancel()
        if (cleared) return
        val storyboard = data.get()
        val storyboardId = storyboard?.storyboardId
        if (storyboardId.isNullOrEmpty()) return
        loopSubtitleGenStatusJob = launch {
            runCatching {
                val subtitleInfo = withContext(Dispatchers.IO) {
                    ApiFactory.apiService.subtitleTaskStatus(StoryboardIdBody(storyboardId)).data
                }
                storyboard.subtitle = subtitleInfo
                data.notifyChange()
                if (subtitleInfo?.generateFailed == true) {
                }
            }
            if (storyboard.subtitleGenerating) {
                delay(3000)
                loopGenSubtitleStatus()
            }
        }
    }
}
