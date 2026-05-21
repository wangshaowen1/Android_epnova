# 项目示例与已知问题

## 推荐默认示例

普通无数据库缓存的分页页面，优先参考 `RoleImageSelectActivity`：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/roleimage/RoleImageSelectActivity.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/roleimage/RoleImageSelectViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/data/RoleImagePagingSource.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/roleimage/RoleImageAdapter.kt`
- `app/src/main/res/layout/activity_role_image_select.xml`

可参考写法：

- `SmartRefreshLayout` + `LoadStateViewHolder` + `RecyclerView`.
- `PagingDataAdapter.withLoadStateFooter(MyMultiStatePagingFooterAdapter(...))`.
- `repeatOnLifecycle(Lifecycle.State.STARTED)` around paging collection.
- `applyGridSpanSizeLookup()` for grid footer span.
- Adapter uses item VM through `binding.getVm(...)`.

不要直接照搬：

- `RoleImagePagingSource` 直接调用 `ApiFactory.apiService`。新代码优先把 `mRepo.api` 传入 `PagingSource`。
- `RoleImagePagingSource` 在 `load()` 中通过 `totalCountBack` 更新 `ObservableBoolean`。新代码不要从 `PagingSource.load()` 修改 UI 状态。

## 简单 Fragment 示例

`ComputingPowerDetailsFragment` 和 `MineChildFragment` 可作为紧凑 Fragment 分页参考：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/details/ComputingPowerDetailsFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/details/ComputingPowerDetailsFragmentViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/details/ComputingPowerDetailsPagingSource.kt`
- `app/src/main/res/layout/fragment_points_detail.xml`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/child/MineChildFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/child/MineChildViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/child/MineChildPagingSource.kt`
- `app/src/main/res/layout/fragment_mine_child.xml`

Bug 与改进点：

- `MineChildFragment` 给 `startRefreshWithAdapter` 传了 `onRefreshListener = { mineAdapter.refresh() }`，但扩展内部已经会调用 `pagingDataAdapter.refresh()`，可能触发重复刷新。
- 这些 Fragment 示例直接用 `lifecycleScope.launch` 收集分页数据。新代码优先使用 `viewLifecycleOwner.lifecycleScope` 和 `repeatOnLifecycle`。
- 这些 `PagingSource` 类直接调用 `ApiFactory.apiService`。新代码优先传入 `mRepo.api`。
- `fragment_points_detail.xml` 和 `fragment_mine_child.xml` 在全页状态容器内使用了 `RecyclerView` 高度 `wrap_content`。新全页列表优先使用 `match_parent`。
- `ComputingPowerDetailsAdapter.areContentsTheSame` 比较 `hashCode()`。新代码优先使用 `oldItem == newItem`。

## 数据库缓存示例

只有需要 Room 缓存时才参考这些示例：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/home/HomeFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/home/HomeViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/home/data/VideoRemoteMediator.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/MyWorksFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/MyWorksViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/WorkInformationRemoteMediator.kt`

可参考写法：

- `RemoteMediator` 与 Room DAO、`database.withTransaction` 配套使用。
- Refresh 时先清理缓存行，再插入新的第一页。
- API 失败返回 `MediatorResult.Error(ApiException(...))`，不要当成列表结束。
- UI 仍使用相同的页面级 `LoadStateViewHolder` 和 footer adapter。

Bug 与改进点：

- `RemoteMediator` 应重新抛出 `CancellationException`。
- 如果缓存数据可能在分页外被插入、删除或重排，append 计算优先使用 remote key。
- 当同一张表支持多个列表时，按 query/user/list type 限定缓存清理范围。
- Fragment 观察与 view 绑定的 LiveData/Flow 时，优先使用 `viewLifecycleOwner`。

## 共享 Helper

相关共享代码：

- `app/src/main/java/com/kuaigeglobal/epnova/common/ext/PagingExtensions.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/LoadStateViewHolder.java`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/stateful/paging/MyMultiStatePagingFooterAdapter.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/stateful/paging/MultiStatePagingFooterAdapter.kt`

重要行为：

- `SmartRefreshLayout.startRefreshWithAdapter` 内部会调用 `pagingDataAdapter.refresh()`。
- 初始状态会调用 `contentSwitcher.showLoading()`。
- Refresh `LoadState.Error` 且没有已加载 item 时展示页面重试；否则保留内容并展示 toast。
- Refresh `LoadState.NotLoading` 且 `pagingDataAdapter.itemCount == 0` 时展示空状态。
- Append 错误由 `withLoadStateFooter` 添加的 footer 处理。
