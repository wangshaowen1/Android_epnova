---
name: android-tab-viewpager
description: "Android 多 Tab + ViewPager2 Fragment 容器规范。仅在本次实际新增、重构或审查 TabLayout/自定义 Tab、ViewPager2、FragmentStateAdapter、Tab 与页面双向同步、Fragment 列表更新或手动页面回调生命周期时使用。Banner/轮播 ViewPager、只改 Tab 文案或样式、只设置 currentItem、只修一个已有回调时不要使用。"
---

# Android Tab 与 ViewPager2

先搜索当前分支的容器、Adapter 和 Tab 实现，区分 Fragment 容器与 Banner/轮播控件。需要选择现有 Adapter、指示器接法或判断回调风险时，再读 [项目索引](references/project-index.md)。

## 容器与子页边界

- 容器只持有 Tab 标题、当前下标和跨 Tab 的汇总状态；列表数据、加载状态和页内交互归对应 Fragment。
- 子页结构和行为一致时复用参数化 Fragment；行为或布局不同才拆成不同 Fragment。
- Tab 参数通过 `arguments` 固定传入。不要让多个 Fragment 读取容器里的可变下标来决定请求参数。
- 每个 Tab 是独立分页列表时，同时读取 `android-paging-state-ui`；容器不持有各子页的 `PagingData`、Adapter 或 LoadState。

## Fragment 列表稳定性

当前项目的 `FragmentStateCacheAdapter` 保存预创建 Fragment，并以实例 `hashCode()` 作为 itemId；`setFragments()` 只替换内存列表，不调用任何 `notify...`。

- 在容器初始化时构造稳定 Fragment 列表，调用一次 `setFragments(...)`，然后再设置给 ViewPager2。
- Tab 点击只切换 `currentItem`，不要重新创建 Fragment 或再次调用 `setFragments(...)`。
- 如果需求确实包含动态增删 Tab，不能假设现有 `setFragments()` 已支持。必须连同稳定业务 ID、`getItemId()`、`containsItem()` 和通知时机一起设计并验证；否则先向用户说明现有 Adapter 的能力边界。

## Tab 与页面同步

- 只选用当前分支已有的一套接法：`TabLayoutMediator`，或 MagicIndicator / 自定义 Tab 的 ViewPager2 绑定；不要叠加两套 page callback。
- Tab 点击负责设置 `currentItem`；页面切换回调只在下标实际变化时更新选中态或 ViewModel 状态。
- 不要让 Tab 点击和页面切换回调分别触发同一网络加载。页数据由子 Fragment 的稳定参数和自身生命周期驱动。
- 显式初始下标应在标题、Fragment 数量和 Adapter 都就绪后设置一次；不要在 Observer、刷新回调或每次 `onPrepareView()` 中反复重置到第 0 页。

## 手动回调生命周期

- 手动注册 `ViewPager2.OnPageChangeCallback` 时保存同一实例，并在当前基类真实匹配的清理点注销；不要注册匿名实例后失去注销句柄。
- Fragment 的 Flow/Paging 使用 `viewLifecycleOwner` 收集，不等于手动 page callback 会自动注销。先检查当前 baselibrary 是否缓存 View，以及附近同类页面的注册/清理配对。

## 静态检查

- Tab 标题数、Fragment 数和可选下标一致，`currentItem` 不越界。
- Fragment 列表只初始化一次，Tab 点击和数据刷新路径没有重建列表。
- 点击与页面回调不会重复请求或互相反复写同一选中态。
- 手动注册的 callback 有同实例注销路径。
- 子页状态属于子 Fragment；分页子页各自持有完整分页链路。
