package com.kuaigeglobal.epnova.feature.play

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaige.videoedit.resource.UrlResource
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryUnlockInfoData
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CurrentWorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserPlayData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.play.data.FeedRepository
import com.kuaigeglobal.epnova.feature.play.more.EpisodeUnlockController
import kotlinx.coroutines.CoroutineScope
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

class DramasPlayPagerViewModel : AppRepositoryViewModel<CreationRepository>(), DownloadingViewModel.DownloadStatusHolder {

    enum class PlayMode { RECOMMEND, SERIES }

    val currentEpisode by lazy { ObservableField<EpisodeInfo>() }
    var workId: String? = null
    var currentPlayMode = PlayMode.SERIES

    // 1. 作品全局状态
    private val _currentWorkContext = MutableStateFlow<WorkVOInfo?>(null)
    val currentWorkContext = _currentWorkContext.asStateFlow()

    // 2. 剧集列表目录
    private val _episodeList = MutableStateFlow<List<EpisodeInfo>>(emptyList())
    val episodeList = _episodeList.asStateFlow()

    /**
     * 播放器初始化状态。
     *
     * /user/work/player/state 同时承载收藏、点赞和续播信息；Activity 只读取这里的续播字段决定起播位置，
     * 点赞/收藏仍在 ViewModel 内合并到现有页面状态，避免页面层直接拆接口响应。
     */
    private val _userPlayState = MutableStateFlow<UserPlayData?>(null)
    val userPlayState = _userPlayState.asStateFlow()


    fun loadDrama(targetWorkId: String?, mode: PlayMode) {
        if (targetWorkId.isNullOrEmpty()) return

        if (this.workId == targetWorkId && _episodeList.value.isNotEmpty()) {
            return
        }

        this.workId = targetWorkId
        this.currentPlayMode = mode

        launch {
            // A. 获取作品详情 (共用)
            val detailRes = withContext(Dispatchers.IO) { mRepo.api.simpleWorkInfo(WorkIdBody(workId)) }
            if (detailRes.isSuccess) {
                _currentWorkContext.value = detailRes.data
            }

            // 进入播放器前先按作品维度查询一次状态，用于决定本次应该跳到哪一集、哪一秒续播。
            // storyId 传 null 时接口不查单集点赞态，只返回作品维度的初始化状态。
            val playerStateRes = withContext(Dispatchers.IO) { mRepo.api.userPlayerState(WorkBody(workId, null)) }
            if (playerStateRes.isSuccess) {
                applyUserPlayState(playerStateRes.data, null)
            }

            // B. 获取剧集目录数据
            if (mode == PlayMode.SERIES) {
                // 剧集模式：优先尝试吞掉“作品详情”传过来的内存数据
                val preloaded = FeedRepository.consumePreloadedSeries(targetWorkId)
                if (!preloaded.isNullOrEmpty()) {
                    _episodeList.value = preloaded
                    return@launch // 直接返回，省去一次网络请求！
                }
            }

            // 推荐模式，或剧集模式没有拿到预加载数据（兜底）：走网络全量拉取
            val listRes = withContext(Dispatchers.IO) { mRepo.api.episodeDirectoryPage(WorkIdPagedBody(workId, 1, 200)) }
            if (listRes.isSuccess) {
                _episodeList.value = listRes.data?.items ?: emptyList()
            }
        }
    }

    private fun applyUserPlayState(data: UserPlayData?, likedStoryId: String?) {
        _userPlayState.value = data
        if (data == null) return

        val workContext = _currentWorkContext.value
        if (workContext != null && data.collected != null) {
            _currentWorkContext.value = workContext.copy(collected = data.collected)
        }

        val isLikedNow = data.liked
        // 接口文档说明播放器入参不带 storyId 时一般不返回点赞态；只有切到具体剧集后才回填对应集的 liked。
        if (likedStoryId != null && isLikedNow != null) {
            _episodeList.update { currentList ->
                currentList.map {
                    if (it.storyId == likedStoryId) it.copy(liked = isLikedNow) else it
                }
            }
        }
    }

    // --- 自动切剧逻辑 ---
    fun loadNextRandomWork() {
        launchWithLoading {
            // 1. 调接口拿下一个随机作品 ID
            val res = withContext(Dispatchers.IO) { mRepo.api.nextWorkRecommend(CurrentWorkIdBody(workId)) }
            if (res.isSuccess) {
                workId = res.data?.workId
                // 2. 清理旧状态，使用推荐模式加载新剧
                _currentWorkContext.value = res.data
                _episodeList.value = emptyList()
                loadDrama(workId, PlayMode.RECOMMEND)
            } else {
                res.msg?.showToast()
            }
        }
    }

    // 懒加载播放地址（已替换为 storyUnlockPreload，兼顾自动解锁）
    suspend fun fetchPlayUrlIfNeeded(episode: EpisodeInfo): EpisodeInfo {
        // URL 已有且集不处于锁定态，直接返回
        if (!episode.videoUrl.isNullOrEmpty() && episode.lockStatus != 2) return episode

        val autoUnlock = Config.get().episodeAutoUnlock
        if (autoUnlock) {
            val apiResponse = withContext(Dispatchers.IO) {
                mRepo.api.storyUnlockPreload(WorkBody(workId, episode.storyId))
            }
            if (apiResponse.isSuccess) {
                val storyData = apiResponse.data?.currentStory
                val finalLockStatus = storyData?.lockStatus ?: episode.lockStatus
                val updatedEpisode = episode.copy(
                    videoUrl = storyData?.videoUrl,
                    likeCount = storyData?.likeCount,
                    duration = storyData?.duration?.toLong() ?: 0L,
                    lockStatus = finalLockStatus,
                )
                // 服务端处理后仍为锁定 → 需要手动解锁
                if (finalLockStatus == 2) {
                    throw EpisodeLockedEx(updatedEpisode, isAutoUnlockFailed = true)
                }
                _episodeList.update { currentList ->
                    currentList.map { if (it.storyId == updatedEpisode.storyId) updatedEpisode else it }
                }
                return updatedEpisode
            }
        } else {
            val apiResponse = withContext(Dispatchers.IO) {
                mRepo.api.episodePlayInfo(WorkBody(workId, episode.storyId))
            }
            if (apiResponse.isSuccess) {
                val episodePlayData = apiResponse.data
                val updatedEpisode = episode.copy(
                    videoUrl = episodePlayData?.videoUrl,
                    likeCount = episodePlayData?.likeCount,
                    duration = episodePlayData?.duration?.toLong() ?: 0L,
                    lockStatus = episode.lockStatus,
                    costNums = episode.costNums,
                )
                _episodeList.update { currentList ->
                    currentList.map { if (it.storyId == updatedEpisode.storyId) updatedEpisode else it }
                }
                return updatedEpisode
            }
        }
        return episode
    }

    /**
     * 创建解锁覆盖层控制器。
     * 由 Activity 传入自身的 lifecycleScope，使协程与页面生命周期对齐。
     */
    fun createUnlockController(episode: EpisodeInfo, scope: CoroutineScope): EpisodeUnlockController {
        return EpisodeUnlockController(mRepo.api, scope, workId, episode)
    }

    /** 手动解锁成功后从外部更新列表状态，清空 videoUrl 触发重新拉取 */
    fun onEpisodeManuallyUnlocked(episode: EpisodeInfo) {
        _episodeList.update { list ->
            list.map {
                if (it.storyId == episode.storyId) it.copy(lockStatus = 1, videoUrl = null) else it
            }
        }
    }

    /** 查询首充礼包弹窗数据 */
    suspend fun fetchFirstRechargeInfo(): FirstRechargeDialogData? {
        val res = withContext(Dispatchers.IO) { mRepo.api.getFirstRechargeDialogInfo() }
        return if (res.isSuccess) res.data else null
    }

    /**
     * 查询解锁弹窗所需信息（广告次数、金币余额等）
     * 供自动解锁失败后、PowerRechargeDialog 关闭前使用
     */
    suspend fun fetchUnlockInfo(episode: EpisodeInfo): StoryUnlockInfoData? {
        val res = withContext(Dispatchers.IO) {
            mRepo.api.storyUnlockInfo(WorkBody(workId, episode.storyId))
        }
        return if (res.isSuccess) res.data else null
    }

    fun getUserPlayStatus(episode: EpisodeInfo?) {
        if (episode?.storyId == null) return
        val targetStoryId = episode.storyId
        launch {
            response(mRepo.api.userPlayerState(WorkBody(workId, targetStoryId))) {
                applyUserPlayState(data, targetStoryId)
            }
        }
    }

    fun reportPlaybackProgress(
        episode: EpisodeInfo?,
        progressMs: Long,
        durationMs: Long,
        completed: Boolean = false,
        progressPercent: Int? = null,
    ) {
        // 播放进度上报统一收口到 EventManager，避免页面/ViewModel 分散直调接口。
        EventManager.reportPlaybackProgress(workId, episode, progressMs, durationMs, completed, progressPercent)
    }

    fun likeToggle(episode: EpisodeInfo?) {
        if (episode == null) return
        launchWithLoading {
            response(mRepo.api.likeToggle(WorkBody(workId, episode.storyId))) {
                // 假设 data 里包含最新的点赞状态和数量，或者前端自己取反
                val isLikedNow = data?.liked ?: (episode.liked == true) // 替换为真实字段
                val likeCountNow = data?.likeCount ?: episode.likeCount
                _episodeList.update { currentList ->
                    currentList.map {
                        if (it.storyId == episode.storyId) it.copy(liked = isLikedNow, likeCount = likeCountNow) else it
                    }
                }
            }
        }
    }

    fun collectToggle() {
        launchWithLoading {
            response(mRepo.api.collectToggle(WorkIdBody(workId))) {
                val workContext = _currentWorkContext.value ?: return@response
                // 假设 data 里包含最新的收藏状态和数量
                val isCollectedNow = data?.collected ?: (workContext.collected == true)
                val collectCountNow = data?.collectCount ?: workContext.collectCount

                // 更新整个作品流
                _currentWorkContext.value = workContext.copy(
                    collected = isCollectedNow,
                    collectCount = collectCountNow
                )
                if (isCollectedNow) {
                    showToast(R.string.collection_success)
                }
            }
        }
    }

    override val downloadProgress by lazy { ObservableInt() }
    val downloading by lazy { MutableLiveData(false) }
    override fun cancelDownload() {
    }

    fun downloadVideo(url: String) {
        launch(block = {
            downloadProgress.set(0)
            downloading.value = true
            val resource = UrlResource(url, RetrofitClient.httpClient)
            resource.download { downloadProgress.set(it) }
            if (resource.file.exists()) {
                val destFile = File(PathUtils.getExternalDownloadsPath(), resource.file.name)
                val copied = if (destFile.exists()) true else withContext(Dispatchers.IO) { FileUtils.copy(resource.file, destFile) }
                if (copied) {
                    FileUtils.notifySystemToScan(destFile)
                    showToast(R.string.download_success_check_album)
                } else {
                    showToast(R.string.video_download_failed_retry)
                }
            } else {
                showToast(R.string.video_download_failed_retry)
            }
            downloading.value = false
        }, onError = {
            showToast(R.string.video_download_failed_retry)
            downloading.value = false
        })
    }

}
