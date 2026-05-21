# Project Examples And Bug Notes

## Recommended Default Example

Use `RoleImageSelectActivity` as the closest current example for a normal paging page without database cache:

- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/roleimage/RoleImageSelectActivity.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/roleimage/RoleImageSelectViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/data/RoleImagePagingSource.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/roleimage/RoleImageAdapter.kt`
- `app/src/main/res/layout/activity_role_image_select.xml`

Good parts to copy:

- `SmartRefreshLayout` + `LoadStateViewHolder` + `RecyclerView`.
- `PagingDataAdapter.withLoadStateFooter(MyMultiStatePagingFooterAdapter(...))`.
- `repeatOnLifecycle(Lifecycle.State.STARTED)` around paging collection.
- `applyGridSpanSizeLookup()` for grid footer span.
- Adapter uses item VM through `binding.getVm(...)`.

Do not copy blindly:

- `RoleImagePagingSource` calls `ApiFactory.apiService` directly. New code should prefer `mRepo.api` passed into the `PagingSource`.
- `RoleImagePagingSource` updates `ObservableBoolean` through `totalCountBack` from `load()`. New code should not mutate UI state from `PagingSource.load()`.

## Simpler Fragment Examples

`ComputingPowerDetailsFragment` and `MineChildFragment` are useful for compact Fragment paging references:

- `app/src/main/java/com/kuaigeglobal/epnova/feature/details/ComputingPowerDetailsFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/details/ComputingPowerDetailsFragmentViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/details/ComputingPowerDetailsPagingSource.kt`
- `app/src/main/res/layout/fragment_points_detail.xml`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/child/MineChildFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/child/MineChildViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/child/MineChildPagingSource.kt`
- `app/src/main/res/layout/fragment_mine_child.xml`

Bug and improvement notes:

- `MineChildFragment` passes `onRefreshListener = { mineAdapter.refresh() }` to `startRefreshWithAdapter`, while the extension already calls `pagingDataAdapter.refresh()`. This can trigger duplicate refreshes.
- These Fragment examples use `lifecycleScope.launch` directly for paging collection. Prefer `viewLifecycleOwner.lifecycleScope` and `repeatOnLifecycle`.
- The `PagingSource` classes call `ApiFactory.apiService` directly. Prefer passing `mRepo.api`.
- `fragment_points_detail.xml` and `fragment_mine_child.xml` use `RecyclerView` height `wrap_content` inside a full-page state holder. Prefer `match_parent` for new full-page lists.
- `ComputingPowerDetailsAdapter.areContentsTheSame` compares `hashCode()`. Prefer `oldItem == newItem`.

## Database Cache Examples

Use these only when Room cache is required:

- `app/src/main/java/com/kuaigeglobal/epnova/feature/home/HomeFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/home/HomeViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/home/data/VideoRemoteMediator.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/MyWorksFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/MyWorksViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/WorkInformationRemoteMediator.kt`

Good parts to copy:

- `RemoteMediator` is paired with Room DAO and `database.withTransaction`.
- Refresh clears cached rows before inserting the new first page.
- API failure returns `MediatorResult.Error(ApiException(...))` instead of being treated as end of list.
- UI still uses the same page-level `LoadStateViewHolder` and footer adapter.

Bug and improvement notes:

- `RemoteMediator` should rethrow `CancellationException`.
- Prefer remote keys for append calculation when cached data can be inserted, deleted, or reordered outside paging.
- Scope cache clearing by query/user/list type when the table can back multiple lists.
- In Fragment observers, prefer `viewLifecycleOwner` for LiveData/Flow tied to the view.

## Shared Helpers

Relevant shared code:

- `app/src/main/java/com/kuaigeglobal/epnova/common/ext/PagingExtensions.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/LoadStateViewHolder.java`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/stateful/paging/MyMultiStatePagingFooterAdapter.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/stateful/paging/MultiStatePagingFooterAdapter.kt`

Important behavior:

- `SmartRefreshLayout.startRefreshWithAdapter` calls `pagingDataAdapter.refresh()` internally.
- Initial state calls `contentSwitcher.showLoading()`.
- Refresh `LoadState.Error` shows page retry if there are no loaded items; otherwise it keeps content and shows a toast.
- Refresh `LoadState.NotLoading` shows empty if `pagingDataAdapter.itemCount == 0`.
- Append errors are handled by the footer from `withLoadStateFooter`.
