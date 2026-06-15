---
name: android-paging-state-ui
description: "Android Paging3 分页加载与状态 UI 实现规范。仅在本次实际涉及分页列表、Paging3、PagingSource、PagingDataAdapter、RemoteMediator、SmartRefreshLayout 分页刷新、LoadStateViewHolder、MyMultiStatePagingFooterAdapter、分页 empty/error/loading/footer/loadState UI、分页 retry 或 Room 缓存分页时使用。普通非分页列表状态 UI 不触发本 skill。"
---

# Android 分页状态 UI

## 触发条件

当任务涉及 Android 项目的分页列表或分页状态 UI 时使用本 skill：

- 用户提到 pagination、paging、分页、分页加载、上拉加载、加载更多、Paging3、PagingSource、PagingDataAdapter、LoadState 或 cached paging；下拉刷新/刷新只有明确处在分页列表、Paging3 或 LoadState 上下文中才触发。
- 生成或审查分页 Activity/Fragment、ViewModel pager、PagingSource、PagingDataAdapter、item VM、XML RecyclerView 布局、footer 状态、重试逻辑或刷新逻辑。
- 实现或审查分页场景下的 `SmartRefreshLayout`、`LoadStateViewHolder`、`MyMultiStatePagingFooterAdapter`、empty/error/loading/content UI、没有更多数据 UI 或页面级状态切换。
- 新增或审查 `RemoteMediator` / `VideoRemoteMediator`；只有需要 Room 数据库缓存、离线缓存、本地优先展示或跨会话列表持久化时，才使用该分支。
- 修复重复刷新、append 错误状态、空列表展示、生命周期收集、网格 footer span 或 `PagingSource.load()` 副作用相关 bug。

## 核心规则

默认使用 `PagingSource + PagingDataAdapter + LoadStateViewHolder`。

只有用户明确需要 Room 数据库缓存、离线缓存、本地优先展示或跨会话列表持久化时，才加入 `RemoteMediator`。`VideoRemoteMediator` 和其他 `RemoteMediator` 示例属于数据库缓存分支，不是默认分支。

实现或审查分页页面时，先查当前 feature 或相近页面的现有分页实现。只有找不到同类实现、不确定项目分页组件用法、需要新增完整分页页面，或发现分页状态/缓存/生命周期问题时，才读取 [project-examples.md](references/project-examples.md) 对齐当前项目示例和已知坑点。

## 默认结构

使用现有项目栈：

- 页面：`BaseViewModelActivity<VB, VM>` 或 `BaseViewModelFragment<VB, VM>`。
- ViewModel：需要 API 访问时使用 `AppRepositoryViewModel<AppRepository>`。
- XML：`<layout>` 根节点，页面变量名为 `layout`。
- 刷新和状态容器：`SmartRefreshLayout` -> `LoadStateViewHolder` -> `RecyclerView`。
- Adapter：`PagingDataAdapter<T, RecyclerView.ViewHolder>`。
- Footer：`MyMultiStatePagingFooterAdapter`。

普通分页页面不要引入 Hilt、AppContainer、ViewModelFactory、AppResult/AppError、UseCase 或完整 MVI。

## XML 模式

除非附近 feature 已有更强局部模式，否则使用以下结构：

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

全页 `LoadStateViewHolder` 内的 `RecyclerView` 优先使用 `match_parent`。只有列表刻意嵌套在可测量父容器中，并且外层布局明确处理滚动时，才使用 `wrap_content`。

## ViewModel 模式

在 ViewModel 中创建 pager，并使用 `viewModelScope` 缓存。

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

如果查询条件会变化，使用 reload trigger 或 `StateFlow.flatMapLatest`，再接 `cachedIn(viewModelScope)`。

## PagingSource 规则

`PagingSource` 只专注加载分页数据，不要在 `load()` 中更新 `ObservableBoolean`、`LiveData`、Adapter 状态或其他 UI 状态。

优先把 `mRepo.api` 或小型 loader lambda 传入 `PagingSource`。除非正在编辑已经沿用该风格的旧类，新代码避免直接使用 `ApiFactory.apiService`。

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

如果后端忽略 `params.loadSize` 并总是使用固定 page size，判断是否还有更多数据时要和实际请求 page size 对比。

## 页面初始化

Adapter、footer、状态容器和数据收集要一起初始化。

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

Fragment 收集 Paging/Flow 数据时可以使用 `viewLifecycleOwner.lifecycleScope` 和 `viewLifecycleOwner.repeatOnLifecycle(...)`。这只约束数据收集生命周期；如果在 `onPrepareView()` 中注册 AdapterDataObserver、RecyclerView/ViewPager callback、键盘监听等手动回调，默认仍在 `onDestroy()` 解除并清空引用，不要生成到 `onDestroyView()`，除非附近同类页面或基类已经这样做。

不要把 `onRefreshListener = { adapter.refresh() }` 传给 `startRefreshWithAdapter`；该扩展已经会调用 `pagingDataAdapter.refresh()`。`onRefreshListener` 只用于重启轮询、刷新头部数据等额外工作。

## Adapter 规则

Adapter 只负责展示：

- 继承 `PagingDataAdapter<T, RecyclerView.ViewHolder>`。
- ViewHolder 优先继承 `BindingViewHolder<VB>`，不要手写 `RecyclerView.ViewHolder(binding.root)`，除非同类旧代码已明确采用该写法。
- `areItemsTheSame` 使用稳定业务 id。
- `areContentsTheSame` 默认使用 `oldItem == newItem`，除非有明确理由只比较特定字段。
- 现有 item layout 使用 DataBinding 时，XML `item` 变量优先绑定 `ItemXxxVM`，通过 `binding.getVm(...)` 获取或复用 item VM，不直接绑定接口实体或临时 UI data class。
- 分页 item 的复杂点击/选择/操作、多选、跨页面刷新或批量操作优先通过 `BaseItemEventsViewModel` 或当前 feature 已有事件 VM 转发，由 Fragment/Activity/ViewModel 收事件处理业务；简单局部 item 操作可以沿用当前 feature 的 item VM callback 或同类 `onItemClick` 写法。Adapter 不直接请求网络、跳转或执行业务。

## 状态 UI 规则

使用项目 helper，不要手写 loading/empty/error 切换：

- `SmartRefreshLayout.startRefreshWithAdapter(...)`
- `LoadStateViewHolder.showLoading()`
- `LoadStateViewHolder.showContent()`
- `LoadStateViewHolder.showEmpty()`
- `LoadStateViewHolder.showRetry(...)`
- `MyMultiStatePagingFooterAdapter`

Page-level state is driven by `adapter.loadState.refresh` in `startRefreshWithAdapter`.

Footer 状态通过 `withLoadStateFooter(...)` 由 `append` 驱动。

网格列表设置带 footer 的 Adapter 后，调用 `applyGridSpanSizeLookup()`，确保 footer/空状态横跨所有列。

如果页面通过 `ConcatAdapter` 添加非分页 header，需要明确验证空状态行为。`startRefreshWithAdapter` 使用 paging adapter 的 item count，不是 concat adapter 总数。

## RemoteMediator 分支

只有需求包含数据库缓存时，才使用 `RemoteMediator`。

使用时：

- API、DAO 和 database 依赖保持从 `mRepo` 传入。
- 使用 `database.withTransaction`。
- `LoadType.REFRESH` 只清理当前作用域缓存。
- 存储 page key 或 remote key，保证 `APPEND` 可靠。
- `ApiResponse` 失败时返回 `MediatorResult.Error(ApiException(...))`。
- 重新抛出 `CancellationException`。
- 在 Adapter 外把 Room entity 转成 API/domain 模型，通常放在 `Pager.flow.map { pagingData.map { it.toDomain() } }`。

不要为了让普通网络列表看起来更“架构化”而新增 Room entity、DAO、mapper 或 `RemoteMediator`。

## 审查清单

分页页面完成前检查：

- `startRefreshWithAdapter` 只调用一次，并且没有重复调用 `adapter.refresh()`。
- Fragment 收集数据可以使用 view 生命周期；`onPrepareView()` 中的手动注册回调用 `onDestroy()` 清理。
- `PagingSource.load()` 没有 UI 副作用。
- 除非为了保留附近历史风格，新代码避免直接使用 `ApiFactory.apiService`。
- 没有吞掉 `CancellationException`。
- API 失败转成 `LoadResult.Error(ApiException(...))`。
- 全页列表中的 `RecyclerView` 没有误用 `wrap_content`。
- DataBinding item XML 绑定的是 `ItemXxxVM`，Adapter ViewHolder 使用 `BindingViewHolder` 和 `binding.getVm(...)`。
- 复杂 item 点击/选择/操作没有直接落在 Adapter lambda 中；多选、跨页面刷新、批量操作或分页业务动作优先通过 `BaseItemEventsViewModel` 或同类事件 VM 转发，简单局部 item 操作可沿用当前 feature 的 callback / `onItemClick` 写法。
- Footer 重试使用 `adapter.retry()`，页面重试使用 `adapter.refresh()`。
- 网格/瀑布流 footer 横跨全宽。
- 空/错/加载文案和图标使用 string/drawable 资源与项目 style。
