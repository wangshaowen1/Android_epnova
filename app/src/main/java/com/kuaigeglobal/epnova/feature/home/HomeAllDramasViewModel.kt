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

class HomeAllDramasViewModel : AppRepositoryViewModel<AppRepository>() {

    val shelfSections = MutableLiveData<List<ShelfSectionUiModel>>(emptyList())
    val pageLoadState = MutableLiveData<LoadState>(LoadState.Loading)
    val footerLoadState = MutableLiveData<PagingLoadState>(PagingLoadState.Loading)
    val isRefreshing = MutableLiveData(true)

    private var sectionConfig: List<BookShelfSectionInfo>? = null
    private var failedSections = emptyList<BookShelfSectionInfo>()
    private var sectionsLoading = false
    private var sectionError: Throwable? = null
    private var pagingRefresh: PagingLoadState = PagingLoadState.Loading
    private var pagingAppend: PagingLoadState = PagingLoadState.NotLoading(false)
    private var pagingItemCount = 0

    val feedFlow = Pager(
        config = PagingConfig(
            pageSize = Constant.Paged.PAGE_SIZE,
            initialLoadSize = Constant.Paged.PAGE_SIZE,
            prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { AllDramasPagingSource(mRepo.api) }
    ).flow.cachedIn(viewModelScope)

    fun loadSections(config: List<BookShelfSectionInfo>) {
        if (sectionConfig != null) return
        // 加载有区块编码的横滑书架，保留配置顺序，不限制具体编码。
        sectionConfig = config.filter {
            it.bookshelfType == 1 && it.bookshelfSectionCode != null
        }.distinctBy { it.bookshelfSectionCode }
        refreshSections()
    }

    fun refreshSections() {
        requestSections(sectionConfig.orEmpty())
    }

    fun retrySections() {
        if (failedSections.isNotEmpty()) requestSections(failedSections)
    }

    fun onPagingRefreshStarted() {
        pagingRefresh = PagingLoadState.Loading
        pagingAppend = PagingLoadState.NotLoading(false)
        updateLoadState()
    }

    fun onPagingLoadStateChanged(states: CombinedLoadStates, itemCount: Int) {
        pagingRefresh = states.refresh
        pagingAppend = states.append
        pagingItemCount = itemCount
        updateLoadState()
    }

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
