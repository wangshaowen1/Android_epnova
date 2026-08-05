---
name: android-feature-scaffold
description: "Android 页面骨架生成规范。仅在本次实际新增页面、创建 Activity/Fragment/ViewModel/XML 骨架、重构页面基础结构，或判断 Base 类、包路径、start/newInstance、mBundle 模式时使用。修改已有页面的局部逻辑、局部 XML、字段绑定、Toast、点击事件或状态同步时不要触发。"
---

# Android 页面骨架

多 Tab + ViewPager2 Fragment 容器同时读取 `android-tab-viewpager`；子页实际使用 Paging3 时再读取 `android-paging-state-ui`。只有附近页面不足以确认 Base 类或生命周期时，才读取 [项目页面索引](references/project-examples.md)。

## 生成前决策

1. 在相同 `feature/<domain>` 搜索最近页面和真实入口。
2. 根据调用入口、Manifest、父容器和同 feature 页面选择 Activity 或 Fragment；弹窗转到 Dialog skill。
3. 只读参数和维护页面状态时优先 `AppViewModel`；需要 API、数据库或仓库能力时使用 `AppRepositoryViewModel<AppRepository>` 或已有 feature Repository。
4. 只生成当前功能需要的文件；没有真实需求时不增加 Repository、Adapter、UiState 或 mapper。
5. 只有存在实际初始化职责时重写 `onCreate()` / `onPrepareView()`；不生成空函数，也不把一两行代码机械拆成 `init/setup/bind/update`。

## 默认骨架

- Activity：`BaseViewModelActivity<VB, VM>`。
- Fragment：`BaseViewModelFragment<VB, VM>`。
- 页面 XML：`<layout>` 根节点，变量名 `layout`。
- 标准标题/返回栏：`ToolbarViewModel` + `@layout/view_model_toolbar`。
- Activity 入口用 `start(context, ...)`；Fragment 用 `newInstance(...)` + arguments。
- 跨页面参数优先使用现有 `Constants.Extra.*` 或局部私有 key，ViewModel 从 `mBundle` 延迟读取。
- 文件放入最近的 `feature/<domain>/`；列表、Repository 等附属文件只在真实需要时新增。

## 生命周期边界

- 本项目 `onPrepareView()` 不是通用模板的 `onViewCreated()`。注册 ViewPager callback、AdapterDataObserver、键盘或事件监听前，先查当前 baselibrary、基类是否缓存 View 和附近同类的配对清理点。
- Fragment 收集 LiveData/Flow 使用 `viewLifecycleOwner`；连续 Flow 可用 `viewLifecycleOwner.lifecycleScope + repeatOnLifecycle`。
- 数据观察使用 view lifecycle，不代表所有手动回调都固定在 `onDestroyView()` 清理；按回调持有对象和当前基类判断。
- ViewModel 拥有的轮询、SSE handler、临时下载或媒体进度任务在 `onCleared()` 取消；Service、系统下载和应用级 Manager 的任务只解绑页面观察。

## 状态、XML 与事件

- 只有 XML 或观察者需要自动刷新时使用 `MutableLiveData` / `Observable*`；内部缓存和临时判断用普通字段。
- 只有已经需要 Paging、SSE、轮询或连续数据时使用 Flow。
- 新页面优先复用现有 style/attr/dimen/drawable，具体映射归 XML 资源规则。
- 保存、提交、支付、生成、跳转、登录等有副作用动作使用 `setOnSingleClickListener` 或项目 click binding；简单关闭、toggle、播放器和 widget 内部回调沿用附近监听。
- `observeEvents` / `FlowEventBus` 只用于跨页面应用事件；父子页面能直接回调或共享状态时不升级为全局事件。

## 检查

- 页面位于最近业务包，Activity/Fragment、VM 和 XML 采用当前项目 Base 体系。
- `layout`、Toolbar、start/newInstance、arguments/mBundle 与附近代码一致。
- 页面层只做 View 初始化、观察、导航和展示，不直接请求网络或数据库。
- 没有空初始化函数、无需求附属层或过量 Observable。
- 手动回调和长任务所有者、清理点已按当前基类确认。
