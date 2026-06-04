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
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.creatorplay.data.TopEarnersCreatorFeedRepository
import com.kuaigeglobal.epnova.feature.creatorplay.data.WorkSeriesCreatorFeedRepository
import com.kuaigeglobal.epnova.feature.creatorplay.mapper.TopEarnersCreatorMapper
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 新创作播放器 VM。
 *
 * 这里只保存播放器入口和当前播放态，不承载旧播放器的解锁、点赞、收藏或选集状态。
 */
class CreatorPlayerViewModel : AppRepositoryViewModel<AppRepository>(), DownloadingViewModel.DownloadStatusHolder {

    /**新播放器复用项目通用 toolbar，只保留返回按钮，不展示标题和菜单。*/
    val toolbar = ToolbarViewModel(
        navigationText = null,
        navigationAction = { back() },
        title = null
    )

    /**播放来源决定切换边界：TopEarners 可跨作品，MyCreations/作品详情只在当前作品内播放。*/
    var playSource: CreatorPlaySource = CreatorPlaySource.TOP_EARNERS

    /**入口传入的初始位置，只用于播放器首次渲染后定位。*/
    var initialPosition: Int = 0

    /**MyCreations/作品详情 source 的当前作品 ID；TopEarners 不需要。*/
    var sourceKey: String? = null

    /**当前正在播放的 Creator 播放单元，供后续标题、埋点或调试状态使用。*/
    val currentPlayableEpisode = ObservableField<CreatorPlayableEpisode?>()

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

    /**切换当前播放 item 时同步右上角操作区；非本人显示举报，本人显示编辑/下载。*/
    fun updateCurrentPlayableEpisode(item: CreatorPlayableEpisode?) {
        currentPlayableEpisode.set(item)
        val isMine = item?.workContext?.isMine() == true
        showOwnerActions.set(isMine)
        showMenuAction.set(item != null)
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

    override fun onCleared() {
        cancelDownload()
        super.onCleared()
    }
}
