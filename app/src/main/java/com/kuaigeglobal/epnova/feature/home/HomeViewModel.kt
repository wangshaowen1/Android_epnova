package com.kuaigeglobal.epnova.feature.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.CombinedLoadStates
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState as PagingLoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.EntityCache
import com.kuaigeglobal.epnova.core.db.mapper.toDomain
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.BannerBody
import com.kuaigeglobal.epnova.core.network.apis.request.BookShelfSectionCodeBody
import com.kuaigeglobal.epnova.core.network.apis.response.HomeBookShelfData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.home.data.TrendingWaterfallPagingSource
import com.kuaigeglobal.epnova.feature.home.data.VideoRemoteMediator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException


class HomeViewModel : AppRepositoryViewModel<AppRepository>() {

    val bannerData = MutableLiveData<List<BannerInfo>>()
    private val bannerCache: EntityCache<List<BannerInfo>> by lazy {
        object : EntityCache<List<BannerInfo>>("HomeBannerList") {}
    }
    /*
        val bookShelfData = MutableLiveData<HomeBookShelfData>()
        val bookShelfError = MutableLiveData<Throwable?>()
        private val bookShelfCache: EntityCache<HomeBookShelfData> by lazy {
            object : EntityCache<HomeBookShelfData>("HomeBookShelfData") {}
        }*/

    override fun onCreate() {
        super.onCreate()
        bannerCache.cache?.let { bannerData.value = it }
        getData()
        // bookShelfCache.cache?.let { bookShelfData.value = it }
        // loadData()
    }

    fun getData() {
        launch({
            response(mRepo.api.getAppBanner(BannerBody(BannerBody.LOCATION_HOME)), {
                bannerData.value = this.data
                bannerCache.cache(this.data) // 更新缓存
            })
        })
    }

    /*   fun loadData() {
           launch(
               block = {
                   bookShelfError.value = null
                   response(mRepo.api.bookShelfHome(), {
                       bookShelfData.value = data
                       bookShelfCache.cache(data) // 更新缓存
                   }, {
                       bookShelfError.value = this?.apiException ?: RuntimeException()
                   })
               },
               onError = {
                   bookShelfError.value = it
               }
           )
       }


       // 用于触发瀑布流加载的 ID。当它改变时，flatMapLatest 会自动取消旧的 Pager 并创建新的
       val currentWaterfallId = MutableStateFlow<String>("")

       @OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
       val feedFlow: Flow<PagingData<WorkVOInfo>> = currentWaterfallId
           .flatMapLatest { bookshelfId ->
               // 【新增逻辑】：如果后端没有下发瀑布流配置，或者 ID 为空
               if (bookshelfId.isEmpty()) {
                   // 直接返回空的 Paging 流，ConcatAdapter 会自动清空底部瀑布流 UI
                   kotlinx.coroutines.flow.flowOf(PagingData.empty())
               } else {
                   // 如果有真实的 ID，才去构建 Pager 发起数据库和网络请求
                   Pager(
                       config = PagingConfig(pageSize = 10, enablePlaceholders = false, prefetchDistance = 6),
                       remoteMediator = VideoRemoteMediator(bookshelfId, mRepo.appDatabase, mRepo.api),
                       pagingSourceFactory = { mRepo.appDatabase.workDao.getPagingData() }
                   ).flow.map { pagingData ->
                       pagingData.map { entity -> entity.toDomain() }
                   }
               }
           }
           .cachedIn(viewModelScope)
   */
    // Trending 配置驱动的横滑书架和底部瀑布流状态。
    val shelfSections = MutableLiveData<List<ShelfSectionUiModel>>(emptyList())
    val waterfallSection = MutableLiveData<BookShelfSectionInfo?>()
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
    private val waterfallCode = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val trendingFeedFlow: Flow<PagingData<WorkVOInfo>> = waterfallCode
        .flatMapLatest { sectionCode ->
            if (sectionCode == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = Constant.Paged.PAGE_SIZE,
                        initialLoadSize = Constant.Paged.PAGE_SIZE,
                        prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        TrendingWaterfallPagingSource(mRepo.api, requireNotNull(sectionCode))
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    fun loadSections(config: List<BookShelfSectionInfo>) {
        if (sectionConfig != null) return
        sectionConfig = config.filter {
            it.bookshelfType == 1 && it.bookshelfSectionCode != null
        }.distinctBy { it.bookshelfSectionCode }
        val waterfall = config.firstOrNull { it.bookshelfType == 2 }
        waterfallSection.value = waterfall
        waterfallCode.value = waterfall?.bookshelfSectionCode
        refreshSections()
    }

    fun refreshSections() {
        requestSections(sectionConfig.orEmpty())
    }

    fun retrySections() {
        if (failedSections.isNotEmpty()) requestSections(failedSections)
    }

    fun onPagingRefreshStarted() {
        // 没有瀑布流配置时使用 PagingData.empty()，其 refresh() 不会触发新的加载状态。
        // 直接置为完成，避免下拉刷新一直停留在 Loading。
        if (waterfallCode.value == null) {
            pagingRefresh = PagingLoadState.NotLoading(true)
            pagingAppend = PagingLoadState.NotLoading(true)
        } else {
            pagingRefresh = PagingLoadState.Loading
            pagingAppend = PagingLoadState.NotLoading(false)
        }
        updateTrendingLoadState()
    }

    fun onPagingLoadStateChanged(states: CombinedLoadStates, itemCount: Int) {
        pagingRefresh = states.refresh
        pagingAppend = states.append
        pagingItemCount = itemCount
        updateTrendingLoadState()
    }

    private fun requestSections(requestedSections: List<BookShelfSectionInfo>) {
        if (sectionsLoading) return
        sectionError = null
        if (requestedSections.isEmpty()) {
            updateTrendingLoadState()
            return
        }
        sectionsLoading = true
        updateTrendingLoadState()
        launch(
            block = {
                val results = coroutineScope {
                    requestedSections.map { section ->
                        async { section to loadTrendingSection(section) }
                    }.awaitAll()
                }
                val latest = results.associate { (section, result) -> section.bookshelfSectionCode to result }
                val previous = shelfSections.value.orEmpty().associateBy { it.config.bookshelfSectionCode }
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
                updateTrendingLoadState()
            }
        )
    }

    private suspend fun loadTrendingSection(
        section: BookShelfSectionInfo
    ): Result<ShelfSectionUiModel> {
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
            Result.failure(error)
        }
    }

    private fun updateTrendingLoadState() {
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
