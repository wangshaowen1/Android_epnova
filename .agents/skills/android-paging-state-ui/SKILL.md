---
name: android-paging-state-ui
description: "Android Paging3 分页加载与状态 UI 实现规范。仅在本次实际涉及分页列表、Paging3、PagingSource、PagingDataAdapter、RemoteMediator、SmartRefreshLayout 分页刷新、LoadStateViewHolder、MyMultiStatePagingFooterAdapter、分页 empty/error/loading/footer/loadState UI、分页 retry 或 Room 缓存分页时使用。普通非分页列表状态 UI 不触发本 skill。"
---

# Android 分页状态 UI

多 Tab + ViewPager2 容器同时读取 `android-tab-viewpager`；本 skill 只负责每个子 Fragment 自己的 Pager、PagingSource、Adapter 和 LoadState。只有附近分页实现不足时读取 [项目分页索引](references/project-examples.md)。

## 默认结构

- 默认 `PagingSource + PagingDataAdapter + LoadStateViewHolder`。
- 新页面优先 `SmartRefreshLayout -> LoadStateViewHolder -> RecyclerView`；当前 feature 已稳定使用 `CoolRefreshView` 等容器时沿用其现有扩展，同一页面不混用两套状态链路。
- Footer 使用 `MyMultiStatePagingFooterAdapter`。
- 只有明确需要 Room 离线缓存、本地优先或跨会话持久化时使用 `RemoteMediator`。
- 全页 LoadState 容器内的 RecyclerView 通常为 `match_parent`；只有外层明确负责测量和滚动时使用 `wrap_content`。

## Pager 与查询条件

- 固定查询条件：创建一次 `Pager(...).flow.cachedIn(viewModelScope)`。
- 可变查询条件：使用 reload trigger，或 `StateFlow.distinctUntilChanged().flatMapLatest { Pager(...) }` 后再 `cachedIn(viewModelScope)`。
- 不用 getter 每次访问都新建 Pager，也不让固定 Pager 捕获之后会变化的普通字段。
- page size/prefetch 复用当前常量或附近同类配置。

## PagingSource

- `load()` 只加载数据，不写 Observable/LiveData、Adapter 或页面 UI。
- 优先从 `mRepo.api` 传入 ApiService 或小型 loader；新代码不直接抓全局 `ApiFactory.apiService`。
- API 业务失败转为 `LoadResult.Error(ApiException(...))`。
- 空列表或小于真实请求 page size 时结束；若后端忽略 `params.loadSize`，按实际请求值判断。
- 捕获异常时重新抛出 `CancellationException`。

## 页面初始化与状态

- Adapter、footer、LoadState 和 Flow 收集一起初始化。
- `startRefreshWithAdapter(...)` 只绑定一次；它已调用 `adapter.refresh()`，不要再传重复 refresh 的 `onRefreshListener`。该回调只用于额外刷新头部或重启其他任务。
- Fragment 使用 `viewLifecycleOwner.lifecycleScope + repeatOnLifecycle(STARTED)` 收集；手动 Observer/callback 的清理点仍按当前 Base View 缓存行为判断。
- 网格/瀑布流添加 footer 后调用 `applyGridSpanSizeLookup()`。
- 页面 retry 使用 `adapter.refresh()`；footer retry 使用 `adapter.retry()`。
- ConcatAdapter 有非分页 header 时单独验证空状态：项目 helper 通常读取 paging adapter 的 item count，不是 concat 总数。

## Adapter 与 ItemVM

- Adapter 只展示和转发；`areItemsTheSame` 使用稳定业务 id，`areContentsTheSame` 默认完整比较。
- DataBinding item 使用 `ItemXxxVM` 时，变量名遵循实际 `BR.item`。
- 仅在模型存在非空、稳定、唯一的业务 key 时使用 `binding.getVm(key, ...)`；不使用 null、position、可变展示字段或废弃 position 重载。
- XML 可直接读取实体展示字段时，不生成 ItemVM 透传 getter/Observable。
- 多选、跨页刷新、批量操作和复杂 item 动作通过当前 feature 事件 VM/callback/ViewModel 收敛；Adapter 不请求网络或直接导航。

## 多 Tab 与缓存分支

- 每个 Tab 子 Fragment 独立持有参数、ViewModel、Pager、PagingSource、Adapter 和 LoadState；容器不共享 PagingData 或可变查询字段。
- `RemoteMediator` 使用事务、作用域清理和可靠 remote key；API 失败返回 `MediatorResult.Error`，取消异常继续抛出。
- Room entity 在 Adapter 外映射成页面实际消费模型；不为普通网络分页默认新增数据库或 domain 层。

## 检查

- Pager 没有重复创建或捕获失效条件，`startRefreshWithAdapter` 没有重复 refresh。
- PagingSource 无 UI 副作用，业务失败和取消处理正确。
- Fragment 收集与手动回调使用各自真实生命周期。
- footer、网格 span、ConcatAdapter 空状态和 retry 语义已验证。
- ItemVM key 稳定且非空，没有 position 重载或透传字段。
- 多 Tab 子页独立；RemoteMediator 只在真实缓存需求下出现。
