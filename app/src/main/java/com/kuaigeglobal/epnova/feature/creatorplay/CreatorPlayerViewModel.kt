package com.kuaigeglobal.epnova.feature.creatorplay

import androidx.databinding.ObservableField
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import androidx.paging.map
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaige.videoedit.resource.UrlResource
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.core.network.apis.PagedList
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkContext
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.creatorplay.data.TopEarnersCreatorFeedRepository
import com.kuaigeglobal.epnova.feature.creatorplay.data.WorkSeriesCreatorFeedRepository
import com.kuaigeglobal.epnova.feature.creatorplay.mapper.CreatorWorkSeriesMapper.toCreatorWorkContext
import com.kuaigeglobal.epnova.feature.creatorplay.mapper.TopEarnersCreatorMapper
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 新创作播放器 VM。
 *
 * 保存播放器入口、播放态并查询续创条件，不承载旧播放器的解锁、点赞、收藏或选集状态。
 */
class CreatorPlayerViewModel : AppRepositoryViewModel<AppRepository>(), DownloadingViewModel.DownloadStatusHolder {

    /**新播放器复用项目通用 toolbar，只保留返回按钮，不展示标题和菜单。*/
    val toolbar = ToolbarViewModel(
        navigationText = null,
        navigationAction = { back() },
        title = null
    )

    /**仅我的创作和作品剧集入口展示底部操作区，不按当前作品归属判断。*/
    val showBottomLayout = ObservableBoolean(false)

    /**播放器非必要控件统一显隐；页面操作区和 feed item 共用这一状态。*/
    val showPlaybackControls = ObservableBoolean(true)

    /**播放来源决定切换边界：TopEarners 可跨作品，MyCreations/作品详情只在当前作品内播放。*/
    var playSource: CreatorPlaySource = CreatorPlaySource.TOP_EARNERS
        set(value) {
            field = value
            showBottomLayout.set(
                value == CreatorPlaySource.MY_CREATIONS || value == CreatorPlaySource.WORK_SERIES
            )
        }

    /**入口传入的初始位置，只用于播放器首次渲染后定位。*/
    var initialPosition: Int = 0

    /**MyCreations/作品详情 source 的当前作品 ID；TopEarners 不需要。*/
    var sourceKey: String? = null

    /**当前正在播放的 Creator 播放单元，供后续标题、埋点或调试状态使用。*/
    val currentPlayableEpisode = ObservableField<CreatorPlayableEpisode?>()

    val showKeepCreating = ObservableBoolean(false)
    val checkingNextEpisode = ObservableBoolean(false)

    /**保留查询后的作品状态，避免切集时用播放器旧快照重新显示已完结作品的按钮。*/
    private var latestWorkContext: CreatorWorkContext? = null

    /**右上角更多按钮；本人作品菜单内是下载，非本人作品菜单内是举报。*/
    val showMenuAction = ObservableBoolean(false)

    /**本人作品展示编辑按钮；下载入口保持在右上角更多菜单内，跟旧播放器一致。*/
    val showOwnerActions = ObservableBoolean(false)

    /**下载弹窗的进度，由 DownloadingDialog 读取。*/
    override val downloadProgress = ObservableInt()

    /**是否正在下载，用于 Activity 展示和关闭 DownloadingDialog。*/
    val downloading = MutableLiveData(false)

    /**当前下载任务；退出页面或取消下载时需要终止，避免后台继续写文件。*/
    private var downloadJob: Job? = null

    /**按入口 source 返回播放器 feed；TopEarners 必须从共享 work feed 派生，不能重新请求接口。*/
    fun playableFeed(): Flow<PagingData<CreatorPlayableEpisode>> {
        return when (playSource) {
            CreatorPlaySource.TOP_EARNERS -> topEarnersPlayableFeed()
            CreatorPlaySource.MY_CREATIONS,
            CreatorPlaySource.WORK_SERIES -> WorkSeriesCreatorFeedRepository.getPlayableFeed(playSource, sourceKey)
        }
    }

    /**TopEarners 播放 feed，从同一个 Creator work PagingData 派生，确保列表分页后播放器同步更新。*/
    private fun topEarnersPlayableFeed(): Flow<PagingData<CreatorPlayableEpisode>> {
        return TopEarnersCreatorFeedRepository.getWorkFeed().map { pagingData ->
            var feedIndex = 0L
            pagingData.map { work ->
                TopEarnersCreatorMapper.toPlayableEpisode(work, feedIndex = feedIndex++)
            }
        }
    }

    /**切换当前播放项时同步作品完结状态和操作区。*/
    fun updateCurrentPlayableEpisode(item: CreatorPlayableEpisode?) {
        currentPlayableEpisode.set(item)
        val workContext = latestWorkContext?.takeIf { it.workId == currentWorkId() }
            ?: item?.workContext
        showKeepCreating.set(item != null && workContext?.isAllEpisodesCompleted() == false)
        val isMine = item?.workContext?.isMine() == true
        showOwnerActions.set(isMine)
        // 下载按钮迁移到底部之后，只需要有举报按钮，因此逻辑改变为非本人才显示更多菜单
        showMenuAction.set(!isMine)
    }

    /**按作品完整剧集进度检查续创条件，页面只负责最终跳转。*/
    fun keepCreating(
        onReady: (CreatorWorkInfo, CreatorEpisodeInfo, CreatorEpisodeInfo?) -> Unit
    ) {
        if (!showBottomLayout.get() || checkingNextEpisode.get()) return
        val workId = currentWorkId()
        if (workId == null) {
            showToast(R.string.loading_failed)
            return
        }
        checkingNextEpisode.set(true)
        launchWithLoading(
            block = {
                try {
                    var workInfo: CreatorWorkInfo? = null
                    response(
                        block = { mRepo.api.getWorkInfo(WorkIdBody(workId)) },
                        result = { workInfo = data },
                        error = { throw (this?.apiException ?: IllegalStateException()) }
                    )
                    val work = workInfo?.copy(workId = workId)
                    if (work == null) {
                        showToast(R.string.loading_failed)
                        return@launchWithLoading
                    }
                    if (currentWorkId() != workId) return@launchWithLoading
                    latestWorkContext = work.toCreatorWorkContext()
                    updateCurrentPlayableEpisode(currentPlayableEpisode.get())
                    if (latestWorkContext?.isAllEpisodesCompleted() == true) return@launchWithLoading

                    val total = work.totalEpisodeCnt ?: 0
                    if (total <= 0 || work.workType == null) {
                        showToast(R.string.loading_failed)
                        return@launchWithLoading
                    }
                    val episodes = loadCreationEpisodes(workId, total) ?: return@launchWithLoading
                    if (currentWorkId() != workId) return@launchWithLoading

                    // 优先找第一集待创建项；全部已开始制作时，恢复第一集未完成项。
//                    val targetIndex = episodes.indexOfFirst { it.isNotCreated() }
//                        .takeIf { it >= 0 }
//                        ?: episodes.indexOfFirst { !it.isCompleted() }
                    val targetIndex = episodes.indexOfFirst { !it.isCompleted() }
                    if (targetIndex < 0) {
                        // 列表已全部完成时，即使作品汇总计数尚未刷新，也不再提供续创入口。
                        latestWorkContext = latestWorkContext?.copy(finishedEpisodeCnt = episodes.size)
                        updateCurrentPlayableEpisode(currentPlayableEpisode.get())
                        return@launchWithLoading
                    }
                    val target = episodes[targetIndex]
                    if (target.isNotCreated()) {
                        // 原型要求提示第一个未进入分镜预览的前置剧集，不能跳过更早的断档。
                        val blocker = episodes.take(targetIndex).firstOrNull {
                            (it.stepProgress ?: 0) < CreatorEpisodeInfo.PROGRESS_STORYBOARD_GENERATING
                        }
                        if (blocker != null) {
                            R.string.please_complete_episode_content.getResString(blocker.sortNo).showToast()
                            return@launchWithLoading
                        }
                    }
                    val previous = episodes.getOrNull(targetIndex - 1)
                    if (target.stepProgress == null ||
                        (!target.isNotCreated() && target.storyId.isNullOrBlank()) ||
                        (target.isNotCreated() && (target.sortNo ?: 0) > 1 && previous?.storyId.isNullOrBlank())
                    ) {
                        showToast(R.string.loading_failed)
                        return@launchWithLoading
                    }
                    onReady(work, target, previous)
                } finally {
                    checkingNextEpisode.set(false)
                }
            },
            onError = { error ->
                if (error is CancellationException) throw error
                (parseMessage(error)?.takeIf { it.isNotBlank() }
                    ?: R.string.loading_failed.getResString()).showToast()
            }
        )
    }

    private fun currentWorkId(): String? {
        val item = currentPlayableEpisode.get()
        return item?.workContext?.workId?.takeIf { it.isNotBlank() }
            ?: item?.episode?.workId?.takeIf { it.isNotBlank() }
            ?: sourceKey?.takeIf { it.isNotBlank() }
    }

    /**不能用播放器的有视频列表校验续创；按接口分页补齐待创建及制作中的剧集。*/
    private suspend fun loadCreationEpisodes(workId: String, total: Int): List<CreatorEpisodeInfo>? {
        val episodes = mutableListOf<CreatorEpisodeInfo>()
        val episodeNumbers = mutableSetOf<Int>()
        var pageNumber = 1
        var pages = 1
        var expectedCount = total
        do {
            var pageData: PagedList<CreatorEpisodeInfo>? = null
            // 拿到所有剧集信息
            response(
                block = { mRepo.api.episodeList(WorkIdPagedBody(workId, pageNumber, total)) },
                result = { pageData = data },
                error = { throw (this?.apiException ?: IllegalStateException()) }
            )
            val page = pageData
            val items = page?.items.orEmpty()
            val invalidEpisodeOrder = items.any {
                val number = it.sortNo
                number == null || number <= 0 || !episodeNumbers.add(number)
            }
            if (page == null || items.isEmpty() || invalidEpisodeOrder) {
                showToast(R.string.loading_failed)
                return null
            }
            if (pageNumber == 1) {
                pages = page.pages.coerceAtLeast(1)
                expectedCount = maxOf(total, page.totalSize)
            }
            episodes.addAll(items)
            pageNumber++
        } while (pageNumber <= pages || episodes.size < expectedCount)
        return episodes.sortedBy { it.sortNo }
    }

    /**取消下载并复位弹窗状态。*/
    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloading.value = false
    }

    /**
     * 下载当前播放器视频。
     *
     * Creator 播放器按 FastFilm 下载口径处理：无水印版本直接保存原视频，
     * 带水印版本先叠加 AI 水印再保存，不接入分镜字幕重导出。
     */
    fun downloadVideo(videoUrl: String?, watermark: Boolean) {
        if (videoUrl.isNullOrEmpty()) {
            showToast(R.string.video_download_failed_retry)
            return
        }
        if (downloading.value == true) return
        downloadProgress.set(0)
        downloading.value = true
        downloadJob = launch(
            block = {
                val resource = if (watermark) {
                    VideoWithWatermarkResource(videoUrl, RetrofitClient.httpClient)
                } else {
                    UrlResource(videoUrl, RetrofitClient.httpClient)
                }
                resource.download { downloadProgress.set(it) }
                val destFile = File(PathUtils.getExternalDownloadsPath(), resource.file.name)
                val copied = if (destFile.exists()) {
                    true
                } else {
                    withContext(Dispatchers.IO) { FileUtils.copy(resource.file, destFile) }
                }
                if (copied) {
                    FileUtils.notifySystemToScan(destFile)
                    showToast(R.string.download_success_check_album)
                } else {
                    showToast(R.string.video_download_failed_retry)
                }
                downloadJob = null
                downloading.value = false
            },
            onError = {
                downloadJob = null
                downloading.value = false
                showToast(R.string.video_download_failed_retry)
            }
        )
    }

    override fun onDestroy() {
        cancelDownload()
        super.onDestroy()
    }
}
