package com.kuaigeglobal.epnova.feature.play

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaige.videoedit.resource.UrlResource
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.CurrentWorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.play.data.FeedRepository
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


    // 替代原有的 loadFeed 和 loadWorkInfo
    fun loadDrama(targetWorkId: String?, mode: PlayMode) {
        if (targetWorkId.isNullOrEmpty()) return
        this.workId = targetWorkId
        this.currentPlayMode = mode

        launch {
            // A. 获取作品详情 (共用)
            val detailRes = withContext(Dispatchers.IO) { mRepo.api.simpleWorkInfo(WorkIdBody(workId)) }
            if (detailRes.isSuccess) {
                _currentWorkContext.value = detailRes.data
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

    // 懒加载播放地址的方法保持不变
    suspend fun fetchPlayUrlIfNeeded(episode: EpisodeInfo): EpisodeInfo {
        if (!episode.videoUrl.isNullOrEmpty()) return episode

        val apiResponse = withContext(Dispatchers.IO) {
            mRepo.api.episodePlayInfo(WorkBody(workId, episode.storyId))
        }

        if (apiResponse.isSuccess) {
            // 1. 创建一个全新的对象（假设 EpisodeInfo 是 data class）
            // 如果你的 EpisodeInfo 是普通的 class，请 new 一个新对象并把属性拷贝过去
            val updatedEpisode = episode.copy(
                videoUrl = apiResponse.data?.videoUrl,
                likeCount = apiResponse.data?.likeCount,
                duration = apiResponse.data?.duration?.toLong() ?: 0L,
            )
            _episodeList.update { currentList ->
                currentList.map {
                    if (it.storyId == updatedEpisode.storyId) updatedEpisode else it
                }
            }
            return updatedEpisode
        }
        return episode
    }

    fun getUserPlayStatus(episode: EpisodeInfo?) {
        if (episode?.storyId == null) return
        val targetStoryId = episode.storyId
        launch {
            response(mRepo.api.userPlayerState(WorkBody(workId, targetStoryId))) {
                // 假设 data 里包含最新的点赞状态和数量，或者前端自己取反
                val isLikedNow = data?.liked
                val isCollectedNow = data?.collected

                val workContext = _currentWorkContext.value
                if (workContext != null && isCollectedNow != null) {
                    _currentWorkContext.value = workContext.copy(collected = isCollectedNow)
                }

                isLikedNow?.let {
                    _episodeList.update { currentList ->
                        currentList.map {
                            if (it.storyId == episode.storyId) it.copy(liked = isLikedNow) else it
                        }
                    }
                }
            }
        }
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