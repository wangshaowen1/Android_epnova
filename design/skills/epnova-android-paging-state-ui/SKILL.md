---
name: epnova-android-paging-state-ui
description: Epnova Android Paging3 list and state UI implementation rules. Use when the user mentions pagination, paging, 分页, 分页加载, 上拉加载, 下拉刷新, load more, refresh, Paging3, PagingSource, PagingDataAdapter, RemoteMediator, VideoRemoteMediator, SmartRefreshLayout, LoadStateViewHolder, MyMultiStatePagingFooterAdapter, empty/error/loading state UI, retry, status UI, 状态 UI, 空状态, 错误状态, 加载状态, or when generating/reviewing paginated Android Kotlin pages, adapters, ViewModels, XML list layouts, Room cached paging, and refresh/retry behavior.
---

# Epnova Android Paging State UI

## Trigger Conditions

Use this skill when the task includes paginated lists or list state UI in the Epnova Android project:

- The user mentions pagination, paging, 分页, 分页加载, 上拉加载, 下拉刷新, 加载更多, 刷新, Paging3, PagingSource, PagingDataAdapter, LoadState, or cached paging.
- Generating or reviewing a paginated Activity/Fragment, ViewModel pager, PagingSource, PagingDataAdapter, item VM, XML RecyclerView layout, footer state, retry logic, or refresh logic.
- Implementing or reviewing `SmartRefreshLayout`, `LoadStateViewHolder`, `MyMultiStatePagingFooterAdapter`, empty/error/loading/content UI, no-more-data UI, or page-level state switching.
- Adding or reviewing `RemoteMediator` / `VideoRemoteMediator`; only apply that branch when Room database cache, offline cache, local-first display, or cross-session list persistence is required.
- Fixing bugs involving duplicate refresh, append error state, empty list display, lifecycle collection, grid footer span, or `PagingSource.load()` side effects.

## Core Rule

Default to `PagingSource + PagingDataAdapter + LoadStateViewHolder`.

Only add `RemoteMediator` when the user explicitly needs Room database cache, offline cache, local-first display, or cross-session list persistence. Treat `VideoRemoteMediator` and other `RemoteMediator` examples as the database-cache branch, not the default branch.

For current project examples and known pitfalls, read [project-examples.md](references/project-examples.md) when implementing or reviewing a paging page.

## Default Structure

Use the existing project stack:

- Page: `BaseViewModelActivity<VB, VM>` or `BaseViewModelFragment<VB, VM>`.
- ViewModel: `AppRepositoryViewModel<AppRepository>` when API access is needed.
- XML: `<layout>` root, page variable name `layout`.
- Refresh and state container: `SmartRefreshLayout` -> `LoadStateViewHolder` -> `RecyclerView`.
- Adapter: `PagingDataAdapter<T, RecyclerView.ViewHolder>`.
- Footer: `MyMultiStatePagingFooterAdapter`.

Do not introduce Hilt, AppContainer, ViewModelFactory, AppResult/AppError, UseCase, or full MVI for a normal paging page.

## XML Pattern

Use this shape unless the nearby feature already has a stronger pattern:

```xml
<com.scwang.smart.refresh.layout.SmartRefreshLayout
    android:id="@+id/srl"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    app:srlAccentColor="@color/white"
    app:srlPrimaryColor="@color/transparent">

    <com.kuaigeglobal.epnova.widget.LoadStateViewHolder
        android:id="@+id/lsv"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:noData="@string/no_data">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipToPadding="false"
            android:overScrollMode="never"
            android:scrollbars="none" />
    </com.kuaigeglobal.epnova.widget.LoadStateViewHolder>
</com.scwang.smart.refresh.layout.SmartRefreshLayout>
```

Prefer `match_parent` for a full-page `RecyclerView` inside `LoadStateViewHolder`. Use `wrap_content` only when the list is intentionally nested in a measured parent and the surrounding layout is known to handle scrolling.

## ViewModel Pattern

Create the pager in ViewModel and cache it in `viewModelScope`.

```kotlin
fun getXxxPage(): Flow<PagingData<XxxInfo>> {
    return Pager(
        config = PagingConfig(
            initialLoadSize = Constant.Paged.PAGE_SIZE,
            pageSize = Constant.Paged.PAGE_SIZE,
            prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
        ),
        pagingSourceFactory = {
            XxxPagingSource(
                api = mRepo.api,
                queryType = queryType
            )
        }
    ).flow.cachedIn(viewModelScope)
}
```

If the query condition can change, use a reload trigger or `StateFlow.flatMapLatest`, then `cachedIn(viewModelScope)`.

## PagingSource Rules

Keep `PagingSource` focused on loading pages. It should not update `ObservableBoolean`, `LiveData`, adapter state, or other UI state from `load()`.

Prefer passing `mRepo.api` or a small loader lambda into the `PagingSource`. Avoid direct `ApiFactory.apiService` in new code unless editing an old class that already follows that style.

```kotlin
class XxxPagingSource(
    private val api: ApiService,
    private val queryType: Int,
) : PagingSource<Int, XxxInfo>() {

    override fun getRefreshKey(state: PagingState<Int, XxxInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, XxxInfo> {
        return try {
            val pageNum = params.key ?: 1
            val response = api.xxxPage(XxxPagedBody(pageNum = pageNum, pageSize = params.loadSize))
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            val items = response.data?.items?.filterNotNull().orEmpty()
            val prevKey = if (pageNum > 1) pageNum - 1 else null
            val nextKey = if (items.isEmpty() || items.size < params.loadSize) null else pageNum + 1
            LoadResult.Page(data = items, prevKey = prevKey, nextKey = nextKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
```

If the backend ignores `params.loadSize` and always uses a fixed page size, compare returned size with the actual requested page size.

## Page Setup

Set the adapter, footer, state holder, and data collection together.

```kotlin
private val xxxAdapter by lazy { XxxAdapter(eventVm) }

private fun initView() {
    binding.recyclerView.apply {
        itemAnimator = null
        adapter = xxxAdapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = xxxAdapter,
                noData = getString(R.string.no_data),
                noMoreThreshold = 12
            ) {
                xxxAdapter.retry()
            }
        )
        applyGridSpanSizeLookup()
    }

    binding.srl.startRefreshWithAdapter(
        pagingDataAdapter = xxxAdapter,
        loadStateHolder = binding.lsv
    )
}

private fun dataUpdate() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            mRealVM.getXxxPage().collectLatest { pagingData ->
                xxxAdapter.submitData(pagingData)
            }
        }
    }
}
```

For `Fragment`, prefer `viewLifecycleOwner.lifecycleScope` plus `viewLifecycleOwner.repeatOnLifecycle(...)` when available.

Do not pass `onRefreshListener = { adapter.refresh() }` to `startRefreshWithAdapter`; the extension already calls `pagingDataAdapter.refresh()`. Use `onRefreshListener` only for extra work such as restarting polling or refreshing header data.

## Adapter Rules

Keep adapters display-only:

- Extend `PagingDataAdapter<T, RecyclerView.ViewHolder>`.
- Use stable business id in `areItemsTheSame`.
- Use `oldItem == newItem` in `areContentsTheSame` unless there is a measured reason to compare specific fields.
- Use `binding.getVm(...)` for item VM when the existing item layout uses DataBinding.
- Forward click/selection events through item event VM or callback. Do not request network in adapter.

## State UI Rules

Use project helpers instead of hand-rolling loading/empty/error switching:

- `SmartRefreshLayout.startRefreshWithAdapter(...)`
- `LoadStateViewHolder.showLoading()`
- `LoadStateViewHolder.showContent()`
- `LoadStateViewHolder.showEmpty()`
- `LoadStateViewHolder.showRetry(...)`
- `MyMultiStatePagingFooterAdapter`

Page-level state is driven by `adapter.loadState.refresh` in `startRefreshWithAdapter`.

Footer state is driven by `append` through `withLoadStateFooter(...)`.

For grid lists, call `applyGridSpanSizeLookup()` after setting an adapter with footer so footer/empty state spans all columns.

If the page has non-paging headers through `ConcatAdapter`, verify empty state behavior explicitly. `startRefreshWithAdapter` uses the paging adapter item count, not the concat adapter total count.

## RemoteMediator Branch

Use `RemoteMediator` only when database cache is part of the requirement.

When using it:

- Keep API, DAO, and database dependencies passed from `mRepo`.
- Use `database.withTransaction`.
- Clear only the scoped cache on `LoadType.REFRESH`.
- Store page keys or remote keys for reliable `APPEND`.
- Return `MediatorResult.Error(ApiException(...))` when `ApiResponse` is unsuccessful.
- Rethrow `CancellationException`.
- Map Room entity to API/domain model outside the adapter, usually in `Pager.flow.map { pagingData.map { it.toDomain() } }`.

Do not add Room entity, DAO, mapper, or `RemoteMediator` just to make a normal network list look more architectural.

## Review Checklist

Before finishing a paging page, check:

- `startRefreshWithAdapter` is called once and does not duplicate `adapter.refresh()`.
- Fragment collection uses the view lifecycle.
- `PagingSource.load()` has no UI side effects.
- New code avoids direct `ApiFactory.apiService` unless preserving nearby legacy style.
- `CancellationException` is not swallowed.
- API failure becomes `LoadResult.Error(ApiException(...))`.
- `RecyclerView` is not accidentally `wrap_content` in a full-page list.
- Footer retry uses `adapter.retry()`, page retry uses `adapter.refresh()`.
- Grid/staggered footer spans full width.
- Empty/error/loading text and icons use string/drawable resources and project styles.
