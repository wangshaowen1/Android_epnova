package com.kuaigeglobal.epnova.kit.media.source

import com.nhsd.basic.utils.Logger
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.common.PagedProxy
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.kit.media.PlaylistListener
import com.kuaigeglobal.epnova.kit.media.PlaylistPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.apply
import kotlin.collections.any
import kotlin.collections.distinctBy
import kotlin.collections.filterNot
import kotlin.collections.filterNotNull
import kotlin.collections.getOrPut
import kotlin.collections.ifEmpty
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.collections.toMutableList
import kotlin.collections.toSet
import kotlin.jvm.javaClass
import kotlin.let
import kotlin.run

/**
 * Singleton object to manage media source loading and playlist updates.
 */
class MediaSourceManager : PlaylistListener {

    companion object {
        private const val LOAD_THRESHOLD_SIZE = 2
        const val LOG_TAG = "MediaSourceManager"
        private val instances = ConcurrentHashMap<String, MediaSourceManager>() // 使用线程安全的Map

        fun with(player: PlaylistPlayer, initialMediasIsCache: Boolean = false): MediaSourceManager {
            val playerId = getPlayerUniqueId(player)
            return instances.getOrPut(playerId) {
                MediaSourceManager().apply {
                    setupPlayer(player, initialMediasIsCache)
                }
            }
        }
        fun get(player: PlaylistPlayer): MediaSourceManager? {
            return instances[getPlayerUniqueId(player)]
        }
        fun release(player: PlaylistPlayer) {
            val playerId = getPlayerUniqueId(player)
            instances[playerId]?.release()
            instances.remove(playerId)
        }

        private fun getPlayerUniqueId(player: PlaylistPlayer): String {
            return "${player.javaClass.simpleName}_${System.identityHashCode(player)}"
        }
    }

    private var job: Job? = null

    // 新增：记录最近一次加载失败的页面
    private var lastFailedPage: Int = -1

    // 数据流
    private val _allLoadedMedias = MutableStateFlow<List<WorkInfo>>(emptyList())
    val allLoadedMedias: StateFlow<List<WorkInfo>> = _allLoadedMedias.asStateFlow()

    // replay 必须为 0，防止 PagingSource 读到上一页的旧数据
    // extraBufferCapacity = 1 允许发射者在没有订阅者时缓存 1 条数据（避免挂起）
    private val _newPageDataFlow = MutableSharedFlow<List<WorkInfo>>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val newPageDataFlow: SharedFlow<List<WorkInfo>> = _newPageDataFlow.asSharedFlow()

    // 错误流
    private val _loadErrorFlow = MutableSharedFlow<Throwable>(replay = 0)
    val loadErrorFlow: SharedFlow<Throwable> = _loadErrorFlow.asSharedFlow()

    private val _preparedMediaDetails = MutableStateFlow<Map<String, WorkInfo>>(emptyMap())

    private var selectionDialogData: List<WorkInfo> = emptyList()

    var mediaSource: TracksPageSource? = null

    var trackListPlayer: PlaylistPlayer? = null
    private val _currentPreparingMedia = MutableSharedFlow<WorkInfo>()
    val currentPreparingMedia: SharedFlow<WorkInfo> = _currentPreparingMedia.asSharedFlow()
    private val proxy = PagedProxy()

    fun hasNextPage(): Boolean = proxy.hasNextPage()

    fun setupPlayer(player: PlaylistPlayer, initialMediasIsCache: Boolean = false): MediaSourceManager {
        if (this.trackListPlayer == player) {
            return this
        }
        this.trackListPlayer?.removeListener(this)
        player.addListener(this)
        this.trackListPlayer = player
        setInitialMedias(player.playlist, initialMediasIsCache)
        return this
    }

    /**
     * 设置初始媒体数据
     */
    fun setInitialMedias(
        initialMedias: List<WorkInfo>?,
        initialMediasIsCache: Boolean = false
    ): MediaSourceManager {
        initialMedias?.let {
            if (trackListPlayer?.playlist != initialMedias) {
                trackListPlayer?.playlist = initialMedias
            }
            updateAllLoadedMedias(initialMedias)
            selectionDialogData = initialMedias
        } ?: run {
            updateAllLoadedMedias(emptyList())
        }
        lastFailedPage = -1
        proxy.reset()
        if (initialMediasIsCache) {
            proxy.currentPage = 0
        } else {
            val totalSize = initialMedias?.size ?: 0
            val pageSize = proxy.pageSize
            val currentPage = totalSize / pageSize
            proxy.currentPage = currentPage
        }
        return this
    }

    // 重试方法
    fun retry() {
        if (lastFailedPage == -1 || job?.isActive == true) return
        Logger.i(LOG_TAG, "重试加载失败的页面: $lastFailedPage")
        loadNextPage()
    }

    fun resetState(){
        job?.cancel()
        Logger.i(LOG_TAG, "开始刷新数据")
        // 重置状态
        proxy.currentPage=0
        lastFailedPage = -1
        // 清空数据（避免新旧数据混合）
        _allLoadedMedias.value = emptyList()
        selectionDialogData = emptyList()
    }

    fun refresh() {
        resetState()
        loadNextPage()
    }

    fun setMediaSource(mediaSource: TracksPageSource?): MediaSourceManager {
        job?.cancel()
        // ✅ 核心修复：检查数据源是否发生变更
        val isDifferentSource = this.mediaSource?.sourceId != mediaSource?.sourceId
        this.mediaSource = mediaSource
        mediaSource?.let {
            // 如果数据源变了（例如从剧集B切回剧集A），或者当前没有数据
            // 必须重置状态，防止 B 的数据残留在 A 的 Manager 中
            if (isDifferentSource || _allLoadedMedias.value.isEmpty()) {
                Logger.i(LOG_TAG, "数据源变更或初始化，重置状态: ${mediaSource.sourceId}")
                // 这里调用 resetState 清理 _allLoadedMedias 和 proxy
                resetState()
                loadNextPage()
            } else {
                // 如果是同一个数据源（例如只是息屏再亮屏），则检查是否需要加载更多
                checkAndLoadIfNeeded()
            }
        }
        return this
    }


    private fun checkAndLoadIfNeeded() {
        trackListPlayer?.let { player ->
            val playlistSize = player.playlist.size
            val currentPlayingIndex = player.getCurrentPlayingIndex()
            if (playlistSize - currentPlayingIndex <= LOAD_THRESHOLD_SIZE) {
                if (proxy.hasNextPage() && job?.isActive != true) {
                    Logger.i(LOG_TAG, "接近列表末尾，自动加载更多")
                    loadNextPage()
                }
            }
        }
    }

    fun loadNextPageForSelection(): Boolean {
        return if (proxy.hasNextPage()) {
            if (job?.isActive != true) {
                loadNextPage()
            }
            true
        } else {
            false
        }
    }

    fun mergeSelectionData(newData: List<WorkInfo>) {
        selectionDialogData = newData
        val currentAllData = _allLoadedMedias.value.toMutableList()
        val newItems = newData.filterNot { newItem ->
            currentAllData.any { it.workId == newItem.workId }
        }

        if (newItems.isNotEmpty()) {
            currentAllData.addAll(newItems)
            updateAllLoadedMedias(currentAllData)
            trackListPlayer?.let { player ->
                if (player.playlist.size < currentAllData.size) {
                    player.addToPlaylist(newItems)
                }
            }
        }
    }

    fun getSelectionDialogInitialData(): List<WorkInfo> {
        return selectionDialogData.ifEmpty { _allLoadedMedias.value }
    }

    override fun onTrackChanged(workInfo: WorkInfo?) {
        super.onTrackChanged(workInfo)
        checkAndLoadIfNeeded()
    }

    private fun loadNextPage(): Boolean {
        if (!proxy.hasNextPage()) return false
        job?.cancel()
        proxy.toNextPage()
        job = ioScope.launch {
            try {
                val currentPage = proxy.currentPage
                Logger.i(LOG_TAG, "加载页面: $currentPage, 每页大小: ${proxy.pageSize}")

                val response = mediaSource?.nextPage(currentPage, proxy.pageSize)
                if (response?.isSuccess != true || response.data == null) {
                    throw ApiException(response?.code ?: 0, response?.msg ?: "Unknown error")
                }
                val data = response.data
                val items = data?.items ?: emptyList()

                Logger.i(LOG_TAG, "加载完成: $currentPage, 数量: ${items.size}")
                lastFailedPage = -1
                if (!proxy.isPageCountSet) {
                    data?.totalSize?.let { proxy.dataCount = it }
                }

                if (items.isEmpty()) {
                    proxy.setReachEnd(true)
                }
                processLoadedItems(currentPage, items)
            } catch (error: Throwable) {
                lastFailedPage = proxy.currentPage
                Logger.i(LOG_TAG, "加载错误: cause = ${error.cause} message = ${error.message}")
                proxy.toPrePage()
                // 发射错误到 Flow
                _loadErrorFlow.emit(error)
            }
        }

        return true
    }

    private suspend fun processLoadedItems(currentPage: Int, items: List<WorkInfo?>) {
        val validItems = items.filterNotNull()
        val existingIds = _allLoadedMedias.value.map { it.workId }.toSet()
        val newItems = validItems.filterNot { item ->
            item.workId in existingIds
        }.distinctBy { it.workId }
        Logger.i(LOG_TAG, "加载条数=${validItems.size}, 去重后=${newItems.size}")
        if (newItems.isEmpty() && proxy.hasNextPage()) {
            Logger.i(LOG_TAG, "页面无新数据，尝试下一页")
            loadNextPage()
        } else {
            updateAllLoadedMedias(_allLoadedMedias.value + newItems)
            trackListPlayer?.addToPlaylist(newItems)
            Logger.i(LOG_TAG, "更新数据，发送更新事件")
            _newPageDataFlow.emit(newItems)
        }
    }

    /**
     * 更新所有已加载的数据
     */
    private fun updateAllLoadedMedias(newData: List<WorkInfo>) {
        _allLoadedMedias.value = newData
    }

    fun getCurrentPage(): Int = proxy.currentPage


    fun release() {
        val playerId = trackListPlayer?.let { getPlayerUniqueId(it) }
        Logger.i(LOG_TAG, "Releasing MediaSourceManager for player: $playerId")

        job?.cancel() // 取消正在进行的请求
        trackListPlayer?.removeListener(this) // 移除监听

        // 发送空数据或特定状态，防止 UI 此时正好在订阅
        // _allLoadedMedias.value = emptyList() // 可选：视业务需求而定

        // 关键：断开引用
        trackListPlayer = null
        mediaSource = null

        // 从静态 Map 移除
        playerId?.let { instances.remove(it) }
    }

}