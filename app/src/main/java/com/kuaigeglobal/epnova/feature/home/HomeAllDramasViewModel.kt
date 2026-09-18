package com.kuaigeglobal.epnova.feature.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.CombinedLoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.BookShelfSectionCodeBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.home.data.AllDramasPagingSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException
import androidx.paging.LoadState as PagingLoadState

/**
 * All Dramas 子页状态持有者：并行加载配置驱动的横滑区块和底部作品分页，
 * 再把两条数据链路合并为页面级和页脚级状态。
 */
class HomeAllDramasViewModel : AppRepositoryViewModel<AppRepository>() {

    /** 已成功加载且有作品的横滑区块，按主页配置顺序提供给顶部适配器。 */
    val shelfSections = MutableLiveData<List<ShelfSectionUiModel>>(emptyList())
    /** 横滑区块与底部分页合并后的页面状态，供 LoadStateViewHolder 使用。 */
    val pageLoadState = MutableLiveData<LoadState>(LoadState.Loading)
    /** 底部分页页脚状态，区分加载更多、失败和没有更多。 */
    val footerLoadState = MutableLiveData<PagingLoadState>(PagingLoadState.Loading)
    /** 横滑区块或底部分页刷新链路加载时保持下拉刷新控件状态。 */
    val isRefreshing = MutableLiveData(true)

    /** 已筛选的横滑区块配置；非空后固定本次子页的区块集合。 */
    private var sectionConfig: List<BookShelfSectionInfo>? = null
    /** 最近一次请求失败的区块，仅重试这些区块以保留已成功内容。 */
    private var failedSections = emptyList<BookShelfSectionInfo>()
    /** 防止刷新回调并发发起重复的横滑区块请求。 */
    private var sectionsLoading = false
    /** 横滑区块最近一次请求错误，用于合并页面错误状态。 */
    private var sectionError: Throwable? = null
    /** 底部分页当前的刷新状态和追加状态。 */
    private var pagingRefresh: PagingLoadState = PagingLoadState.Loading
    private var pagingAppend: PagingLoadState = PagingLoadState.NotLoading(false)
    /** 用于判断底部分页是否已经提供内容。 */
    private var pagingItemCount = 0

    /** 底部 All Dramas 作品分页流，缓存范围跟随当前 ViewModel 生命周期。 */
    val feedFlow = Pager(
        config = PagingConfig(
            pageSize = Constant.Paged.PAGE_SIZE,
            initialLoadSize = Constant.Paged.PAGE_SIZE,
            prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { AllDramasPagingSource(mRepo.api) }
    ).flow.cachedIn(viewModelScope)

    /** 首次接收主页配置时筛选可请求的横滑区块，并触发首次加载。 */
    fun loadSections(config: List<BookShelfSectionInfo>) {
        if (sectionConfig != null) return
        // 加载有区块编码的横滑书架，保留配置顺序，不限制具体编码。
        sectionConfig = config.filter {
            it.bookshelfType == 1 && it.bookshelfSectionCode != null
        }.distinctBy { it.bookshelfSectionCode }
        refreshSections()
    }

    /** 重新请求全部横滑区块，通常由下拉刷新触发。 */
    fun refreshSections() {
        requestSections(sectionConfig.orEmpty())
    }

    /** 只重试上次失败的横滑区块，不重复请求已经成功的区块。 */
    fun retrySections() {
        if (failedSections.isNotEmpty()) requestSections(failedSections)
    }

    /** 在底部分页开始刷新时重置分页状态，并重新计算页面状态。 */
    fun onPagingRefreshStarted() {
        pagingRefresh = PagingLoadState.Loading
        pagingAppend = PagingLoadState.NotLoading(false)
        updateLoadState()
    }

    /** 接收底部分页的刷新/追加状态和当前条目数，驱动页脚与页面状态。 */
    fun onPagingLoadStateChanged(states: CombinedLoadStates, itemCount: Int) {
        pagingRefresh = states.refresh
        pagingAppend = states.append
        pagingItemCount = itemCount
        updateLoadState()
    }

    /** 并行加载指定横滑区块，并保留失败区块之前已经展示的内容。 */
    private fun requestSections(requestedSections: List<BookShelfSectionInfo>) {
        if (sectionsLoading) return
        sectionError = null
        if (requestedSections.isEmpty()) {
            updateLoadState()
            return
        }
        sectionsLoading = true
        updateLoadState()
        launch(
            block = {
                // 区块之间相互独立，并行请求可以避免单个慢区块阻塞整个顶部区域。
                val results = coroutineScope {
                    requestedSections.map { section ->
                        async { section to loadSection(section) }
                    }.awaitAll()
                }
                val latest = results.associate { (section, result) -> section.bookshelfSectionCode to result }
                val previous = shelfSections.value.orEmpty().associateBy { it.config.bookshelfSectionCode }
                // 单个区块失败保留它已有的数据，不丢弃其他成功区块。
                shelfSections.value = sectionConfig.orEmpty().mapNotNull { section ->
                    latest[section.bookshelfSectionCode]?.getOrNull() ?: previous[section.bookshelfSectionCode]
                }.filter { it.works.isNotEmpty() }
                failedSections = results.filter { it.second.isFailure }.map { it.first }
                sectionError = results.firstNotNullOfOrNull { it.second.exceptionOrNull() }
            },
            onError = {
                failedSections = requestedSections
                sectionError = it
            },
            onComplete = {
                sectionsLoading = false
                updateLoadState()
            }
        )
    }

    /** 请求单个横滑区块的预览作品，并把异常限制在当前区块。 */
    private suspend fun loadSection(section: BookShelfSectionInfo): Result<ShelfSectionUiModel> {
        return try {
            var works = emptyList<WorkVOInfo>()
            response(
                bean = mRepo.api.bookShelfHorizontalPreview(
                    BookShelfSectionCodeBody(requireNotNull(section.bookshelfSectionCode))
                ),
                result = { works = data.orEmpty() },
                error = { throw (this?.apiException ?: IllegalStateException()) }
            )
            Result.success(ShelfSectionUiModel(section, works))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // 每个区块独立处理失败，避免一个 async 取消其他区块。
            Result.failure(error)
        }
    }

    /** 将横滑区块和底部分页的状态合并为页面空态、错误、页脚和刷新状态。 */
    private fun updateLoadState() {
        val loading = sectionConfig == null || sectionsLoading || pagingRefresh is PagingLoadState.Loading
        val error = (pagingRefresh as? PagingLoadState.Error)?.error ?: sectionError
        val hasContent = shelfSections.value.orEmpty().any { it.works.isNotEmpty() } || pagingItemCount > 0
        pageLoadState.value = when {
            hasContent -> LoadState.NotLoading(pagingAppend.endOfPaginationReached)
            loading -> LoadState.Loading
            error != null -> LoadState.Error(error)
            else -> LoadState.Empty
        }
        footerLoadState.value = when {
            pagingRefresh is PagingLoadState.Error -> pagingRefresh
            pagingAppend is PagingLoadState.Error -> pagingAppend
            loading || pagingAppend is PagingLoadState.Loading -> PagingLoadState.Loading
            sectionError != null -> PagingLoadState.Error(requireNotNull(sectionError))
            else -> pagingAppend
        }
        isRefreshing.value = loading
    }
}
