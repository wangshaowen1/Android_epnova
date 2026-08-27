package com.kuaigeglobal.epnova.feature.editor

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.Player
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.ext.handlePointInsufficientError
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.putAIGCMetadata
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CreationScene
import com.kuaigeglobal.epnova.core.network.apis.request.IdsBody
import com.kuaigeglobal.epnova.core.network.apis.request.TaskIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.kit.media.PlayerFactory
import com.kuaigeglobal.epnova.kit.media.play
import com.kuaigeglobal.epnova.kit.media.setPlayUrl
import com.kuaigeglobal.epnova.widget.GeneratingDialog
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 编辑分镜视频
 */
class EditStoryboardVideoViewModel : AppRepositoryViewModel<EditStoryboardRepository>(), DownloadingViewModel.DownloadStatusHolder {
    var storyInfo: EpisodeInfo? = null
        private set
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyTitle by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val taskId by lazy { mBundle.getString(Constants.Extra.TASK_ID) }
    val workRatio by lazy { ObservableField<String>(mBundle.getString(Constants.Extra.VIDEO_RATIO, "9:16") ?: "9:16") }
    val loadState by lazy { ObservableInt() }
    val model by lazy { mBundle.getInt(Constants.Extra.MODE) }
    val currentStoryboard by lazy { ObservableField<ItemStoryboardVM>() }
    val scrollToItem by lazy { MutableLiveData<Int>(-1) }
    val adapter by lazy { QuickAdapter<ItemStoryboardVM>(R.layout.item_short_drama_editor_segment, emptyList()) }
    val nextStepEnabled by lazy { ObservableBoolean(false) }
    val publishLocked by lazy { ObservableBoolean(false) }
    val storyboardPlayer by lazy {
        PlayerFactory.createPlayer(true, autoAudioFocus = true).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_READY -> {
                        }

                        Player.STATE_ENDED -> {
                            progressJob?.cancel()
                            subtitle.set(null)
                            playNext()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    if (isPlaying) {
                        // 只要 isPlaying 为 true，说明视频已经在动了，必须隐藏 loading
                        // 这能解决缓存命中时，STATE_READY 没能及时更新 UI 的问题
                        startProgressUpdater()
                    } else {
                        progressJob?.cancel()
                        // 注意：这里不要盲目设为 Pause，因为 isPlaying=false 可能是 Buffering
                    }
                }
            })
        }
    }
    val playing by lazy { ObservableBoolean(true) }
    private var synthesisDialog: GeneratingDialog? = null
    private var synthesisTask: VideoSynthesisTask? = null
    private var loopSynthesisStatusJob: Job? = null
    private var progressJob: Job? = null
    private var downloadJob: Job? = null
    val downloading by lazy { MutableLiveData(false) }
    override val downloadProgress by lazy { ObservableInt() }
    val subtitle by lazy { ObservableField<String>() }

    override fun onCreate() {
        super.onCreate()
        storyInfo = mBundle.getParcelableValue(Constants.Extra.DATA, EpisodeInfo::class.java)
        updatePublishLocked()
        getStoryboardList()
        getStoryInfo { }
    }

    override fun onPause() {
        super.onPause()
        storyboardPlayer.pause()
    }

    override fun onDestroy() {
        cancelDownload()
        progressJob?.cancel()
        loopSynthesisStatusJob?.cancel()
        adapter.items.forEach { it.release() }
        super.onDestroy()
        storyboardPlayer.release()
    }

    private fun loopSynthesisStatus() {
        loopSynthesisStatusJob?.cancel()
        loopSynthesisStatusJob = launch {
            val task = synthesisTask
            if (task != null) {
                if (task.status == VideoSynthesisTask.STATUS_SUCCESS) {
                    synthesisDialog?.dismiss()
                    finish()
                } else if (task.isSynthesising()) {
                    delay(1000)
                    loopSynthesisStatus()
                } else if (task.isFailed()) {
                    synthesisDialog?.dismiss()
                    if (task.status == VideoSynthesisTask.STATUS_VIDEO_GENERATE_FAIL) {
                        showToast(R.string.video_composition_failed)
                    } else {
                        task.reason?.showToast()
                    }
                }
            }
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = launch {
            while (isActive) {
                if (storyboardPlayer.isPlaying) {
                    // 每当播放进度改变或发生事件时触发
                    val currentTime = storyboardPlayer.currentPosition

                    // 找到当前时间点应该显示的字幕
                    val currentLine = currentStoryboard.get()?.data?.get()?.subtitle?.subtitle?.find {
                        currentTime in (it.startTime ?: 0)..(it.endTime ?: 0)
                    }?.text
                    subtitle.set(currentLine)
                }
                delay(200)
            }
        }
    }

    fun getStoryboardList() {
        if (adapter.items.isEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val list = withContext(Dispatchers.IO) { mRepo.api.workStoryboardList(WorkBody(workId, storyId)).data }
                if (list == null) {
                    loadState.set(LoadStateViewHolder.RETRY)
                } else if (list.isEmpty()) {
                    loadState.set(LoadStateViewHolder.NO_DATA)
                } else {
                    list.mapIndexed { index, info ->
                        ItemStoryboardVM(workId, storyId, info).apply {
                            onItemClick.value = { v ->
                                if (this != currentStoryboard.get()) {
                                    select()
                                }
                            }
                        }
                    }.let {
                        adapter.items.forEach { it.release() }
                        adapter.submitList(it)
                    }
                    nextStepEnabled.set(allStoryboardGenerated())
                    val currentStoryboard = currentStoryboard.get()
                    if (currentStoryboard == null) {
                        adapter.items.firstOrNull()?.select()
                    } else {
                        adapter.items.firstOrNull { it.data.get()?.storyboardId == currentStoryboard.data.get()?.storyboardId }?.select()
                    }
                    updateLoadState()
                }
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        if (adapter.items.isEmpty()) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    fun play() {
        if (storyboardPlayer.isPlaying) {
            storyboardPlayer.pause()
            playing.set(false)
        } else {
            when (storyboardPlayer.playbackState) {
                Player.STATE_IDLE -> {
                    val videoUrl = currentStoryboard.get()?.data?.get()?.videoUrl
                    if (videoUrl.isNullOrEmpty().not()) {
                        storyboardPlayer.play(videoUrl)
                        playing.set(true)
                    }
                }

                Player.STATE_ENDED -> {
                    storyboardPlayer.seekTo(0)
                    storyboardPlayer.play()
                    playing.set(true)
                }

                else -> {
                    storyboardPlayer.play()
                    playing.set(true)
                }
            }
        }
    }

    fun stopPlay() {
        storyboardPlayer.stop()
        playing.set(false)
    }

    private fun playNext() {
        val current = currentStoryboard.get()
        val storyboard = current?.data?.get()
        if (storyboard?.videoUrl.isNullOrEmpty()) return
        val currentIndex = adapter.items.indexOf(current)
        val next = if (currentIndex > -1 && currentIndex < adapter.items.size - 1) adapter.getItem(currentIndex + 1) else adapter.getItem(0)
        next.select()
    }

    private fun ItemStoryboardVM?.select() {
        currentStoryboard.get()?.selected?.set(false)
        this?.selected?.set(true)
        currentStoryboard.set(this)
        adapter.items.indexOf(this).takeIf { it > -1 }?.let { scrollToItem.value = it }
        val storyboard = currentStoryboard.get()?.data?.get()
        val videoUrl = storyboard?.videoUrl
        if (storyboard?.videoGenerated == true && videoUrl.isNullOrEmpty().not()) {
            if (playing.get()) {
                storyboardPlayer.play(videoUrl)
            } else {
                storyboardPlayer.playWhenReady = false
                storyboardPlayer.setPlayUrl(videoUrl)
                storyboardPlayer.prepare()
            }
        } else {
            storyboardPlayer.clearMediaItems()
            storyboardPlayer.pause()
            playing.set(false)
        }
    }

    private fun allStoryboardGenerated(): Boolean {
        return adapter.items.all { it.data.get()?.videoGenerated == true }
    }

    fun anyStoryboardGenerating(): Boolean {
        return adapter.items.any { it.data.get()?.videoGenerating == true }
    }

    fun anyStoryboardFailed(): Boolean {
        return adapter.items.any { it.data.get()?.videoGenerateFailed == true }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloading.value = false
    }

    fun download(storyboard: SimpleStoryboardInfo, videoUrl: String, watermark: Boolean) {
        if (downloading.value == true) return
        downloadProgress.set(0)
        downloading.value = true
        downloadJob = launch(
            block = {
                val resource = VideoStoryboardResource(videoUrl, storyboard, watermark, RetrofitClient.httpClient)
                resource.download { downloadProgress.set(it) }
                val destFile = File(PathUtils.getExternalDownloadsPath(), resource.file.name)
                val copied = if (destFile.exists()) true else withContext(Dispatchers.IO) { FileUtils.copy(resource.file.putAIGCMetadata(storyboard), destFile) }
                if (copied) {
                    FileUtils.notifySystemToScan(destFile)
                    showToast(R.string.video_saved_to_album)
                } else {
                    showToast(R.string.toast_save_fail)
                }
                downloadJob = null
                downloading.value = false
            },
            onError = {
                showToast(R.string.toast_save_fail)
                downloadJob = null
                downloading.value = false
            }
        )
    }

    val isSynthesised get() = storyInfo?.stepProgress in arrayOf(40, 45, 50)
    fun deleteCurrentStoryboard() {
        if (showPublishLockedToast()) return
        launchWithLoading {
            response(mRepo.api.workStoryboardDelete(IdsBody(null, currentStoryboard.get()?.data?.get()?.storyboardId, if (isSynthesised) 4 else null))) {
                val currentStoryboard = currentStoryboard.get()
                playNext()
                currentStoryboard?.let {
                    adapter.remove(it)
                    if (adapter.items.isEmpty()) {
                        loadState.set(LoadStateViewHolder.NO_DATA)
                    }
                }
            }
        }
    }

    fun synthesis() {
        if (showPublishLockedToast()) return
        if (anyStoryboardGenerating()) {
            showToast(R.string.video_generating_please_wait)
            return
        }
        if (anyStoryboardFailed()) {
            showToast(R.string.video_generation_failed_retry)
            return
        }
        val storyboards = adapter.items.mapNotNull { it.data.get() }
        if (storyboards.isEmpty()) {
            return
        }
        getStoryInfo {
            //重绘已完成
            if (it?.stepProgress == EpisodeInfo.EPISODE_REPAINT_COMPLETED) {
                topActivity?.showCommonDialog(
                    title = R.string.confirm_synthesize_and_overwrite.getResString(),
                    tip = R.string.overwrite_warning_message.getResString(),
                    cancelText = R.string.back_to_edit.getResString(),
                    cancel = { },
                    confirmText = R.string.confirm_synthesize.getResString(),
                    confirm = {
                        doSynthesis(storyboards)
                    })
            } else {
                doSynthesis(storyboards)
            }
        }
    }

    private fun doSynthesis(storyboards: List<SimpleStoryboardInfo>) {
        topActivity?.let {
            val dialog = GeneratingDialog(it, R.string.video_synthesizing.getResString(), R.string.do_not_exit_or_lock_screen.getResString())
            synthesisDialog = dialog
            dialog.show()
        }
        val task = VideoSynthesisTask(workId, storyId, storyboards)
        synthesisTask = task
        VideoSynthesisManager.synthesis(task)
        loopSynthesisStatus()
    }

    fun updateStoryboard(storyboardInfo: SimpleStoryboardInfo) {
        adapter.items.firstOrNull { it.data.get()?.storyboardId == storyboardInfo.storyboardId }?.data?.set(storyboardInfo)
    }

    fun getStoryInfo(callback: (story: EpisodeInfo?) -> Unit) {
        launchWithLoading {
            response(mRepo.api.singleEpisodeDetails(WorkBody(workId, storyId))) {
                data?.let { storyInfo = it }
                updatePublishLocked()
                data?.workRatio?.let { workRatio.set(it) }
                callback.invoke(storyInfo)
            }
        }
    }

    fun dropRepaint() {
        launchWithLoading {
            response(mRepo.api.dropRepaint(WorkBody(workId, storyId))) {
                finish()
            }
        }
    }

    private var cleared = false

    /**
     * 重新生成分镜
     */
    fun regenerateStoryboards() {
        if (showPublishLockedToast()) return
        launch {
            loadState.set(LoadStateViewHolder.LOADING)
            //先查询分镜列表
            val res = runCatching { withContext(Dispatchers.IO) { mRepo.api.workStoryboardList(WorkBody(workId, storyId)) } }.getOrNull()
            if (res?.isSuccess == true) {
                //分镜列表为空，开始生成分镜描述
                if (res.data.isNullOrEmpty()) {
                    val task = runCatching {
                        withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId)).data }
                    }.getOrNull()
                    //分镜描述生成失败
                    if (task == null || task.genStatus == 3) {
                        loadState.set(LoadStateViewHolder.NO_DATA)
                        return@launch
                    }
                    val taskId = task.taskId
                    //轮询描述生成状态
                    var generatingDescription = true
                    while (generatingDescription && !cleared) {
                        delay(3000)
                        val task = runCatching {
                            withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescriptionStatus(TaskIdBody(taskId, null)).data }
                        }.getOrNull()
                        when (task?.genStatus) {
                            2 -> {
                                //分镜描述生成成功
                                generatingDescription = false
                            }

                            3 -> {
                                //分镜描述生成失败
                                loadState.set(LoadStateViewHolder.NO_DATA)
                                generatingDescription = false
                                return@launch
                            }
                        }
                    }
                }
                if (cleared) return@launch
                //获取分镜列表
                val storyboards =
                    runCatching { withContext(Dispatchers.IO) { mRepo.api.workStoryboardList(WorkBody(workId, storyId)).data } }.getOrNull()
                if (storyboards.isNullOrEmpty()) {
                    loadState.set(LoadStateViewHolder.NO_DATA)
                } else {
//                    val cost = runCatching {
//                        val price = withContext(Dispatchers.IO) { mRepo.api.queryAiPrice(StrategyTypeBody(3)).data?.pointNum ?: 0 }
//                        storyboards.sumOf { it.duration ?: 0 } * price
//                    }.getOrNull()
                    response(
                        bean = mRepo.api.workStoryboardVideoGenerate(WorkBody(workId, storyId)),
                        result = { getStoryboardList() },
                        error = {
                            val handled = handlePointInsufficientError(
                                creationScene = CreationScene.VIDEO_GENERATION,
                                errorCode = this?.code,
                            )
                            if (!handled) {
                                this?.msg?.showToast()
                            }
                            loadState.set(LoadStateViewHolder.NO_DATA)
                        })
                }
            } else {
                loadState.set(LoadStateViewHolder.NO_DATA)
            }

        }
    }

    fun generateCurrentStoryboard() {
        if (showPublishLockedToast()) return
        currentStoryboard.get()?.generate(storyInfo)
    }

    fun generateCurrentSubtitle() {
        if (showPublishLockedToast()) return
        currentStoryboard.get()?.generateSubtitle()
    }

    fun showPublishLockedToast(): Boolean {
        if (!publishLocked.get()) return false
        showToast(R.string.drama_ep_function_unavailable_review)
        return true
    }

    private fun updatePublishLocked() {
        publishLocked.set(storyInfo?.isPublishedOrUnderReview() == true)
    }

}
