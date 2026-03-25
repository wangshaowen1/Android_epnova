package com.kuaigeglobal.epnova.feature.play

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.feature.creator.data.CreationRepository
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.play.data.FeedRepository
import com.kuaige.videoedit.resource.UrlResource
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DramasPlayPagerViewModel : AppRepositoryViewModel<CreationRepository>(), DownloadingViewModel.DownloadStatusHolder {

    enum class PlayMode { RECOMMEND, SERIES }

    val currentWork by lazy { ObservableField<WorkInfo>() }
    var currentPlayMode = PlayMode.SERIES

    // 对外暴露的数据流
    private val _feedFlow = MutableStateFlow<PagingData<WorkInfo>>(PagingData.empty())
    val feedFlow: Flow<PagingData<WorkInfo>> = _feedFlow

    init {
        // 默认加载一次，方便进入页面直接看效果
        // loadFeed(true, null)
    }

    fun loadFeed(isSeriesMode: Boolean, id: String?) {
        // 1. 设置当前模式
        currentPlayMode = if (isSeriesMode) PlayMode.SERIES else PlayMode.RECOMMEND

        viewModelScope.launch {
            // Step A: 获取原始 Flow
            val rawFlow = if (isSeriesMode) {
                // [Series模式]：获取专用数据流（内存预加载 或 独立网络请求）
                FeedRepository.getSeriesFlowOrNetwork(id ?: "", true)
            } else {
                // [Feed模式]：获取全局共享流
                // Repository 内部已经用 repositoryScope 缓存过了
                FeedRepository.getFeedFlow(false, id)
            }

            // Step B: 决定是否需要在当前 ViewModel 缓存
            val finalFlow = if (isSeriesMode) {
                // Series 模式下，Flow 可能是新创建的（比如来自 flowOf(PagingData) 或 新的 Pager），
                // 需要绑定到当前 ViewModel 生命周期，防止配置变更（如旋转）丢失状态
                rawFlow.cachedIn(viewModelScope)
            } else {
                // [关键修改] Feed 模式下，直接使用 Repository 的流。
                // 因为 Repository 已经 cachedIn(repositoryScope)，它是全局存活的。
                // 如果这里再 cachedIn(viewModelScope)，会导致 Flow 被包装，可能丢失原有的共享状态。
                rawFlow
            }

            // Step C: 收集并发送给 UI
            finalFlow.collectLatest { pagingData ->
                _feedFlow.emit(pagingData)
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