# Epnova Android 场景规范

本文档承接 [android-codegen-guidelines-epnova.md](android-codegen-guidelines-epnova.md) 的场景细则。使用时先遵守总规范，再按当前任务命中具体场景和 skill。

## 列表

非分页 DataBinding 列表已抽成 skill：[epnova-android-databinding-list](skills/epnova-android-databinding-list/SKILL.md)。

适用场景：

- 普通 RecyclerView 列表。
- `QuickAdapter<ItemXxxVM>`。
- `AppItemViewModel<T>`。
- `BaseMultiItemAdapter` 多类型列表。
- `submitList` 更新。
- item XML 使用 `item` 变量。
- Adapter 点击事件转发。

规则：

- 普通 DataBinding 列表优先使用 `QuickAdapter<ItemXxxVM>` + `AppItemViewModel<T>`。
- item layout 变量固定为 `item`。
- 简单列表更新使用 `submitList(newItems)`。
- 只有强制全量刷新时才使用 adapter 的 `refresh()`。
- 多类型列表可以使用 `BaseMultiItemAdapter<AppItemViewModel<*>>`，参考 `AssetManagementAdapter`、`StoryboardDesignAdapter`。
- Adapter 只负责渲染和转发点击事件，不直接请求网络。
- 如果列表 item 需要选中态、展开态、编辑态、上传进度、本地临时状态，优先使用 ItemVM 包装接口实体。

分页列表使用 [epnova-android-paging-state-ui](skills/epnova-android-paging-state-ui/SKILL.md)。

- Paging3 按现有 `PagingSource`、`PagingDataAdapter`、`cachedIn(viewModelScope)` 风格实现。
- SmartRefreshLayout、LoadStateViewHolder、MyMultiStatePagingFooterAdapter 按项目已有分页状态 UI 写法处理。
- `RemoteMediator` / `VideoRemoteMediator` 仅在明确需要数据库缓存数据时作为可选分支，不是分页默认项。

## 弹窗

弹窗规范已抽成 skill：[epnova-android-dialog](skills/epnova-android-dialog/SKILL.md)。

基类选择：

- 带 ViewModel 的中间弹窗：`BaseVMDialogFragment<VB, VM>`。
- 带 ViewModel 的底部弹窗：`BaseBottomPushVMDialogFragment<VB, VM>`。
- 不带 ViewModel 的简单中间弹窗：`BaseDialogFragment<VB>`。
- 不带 ViewModel 的简单底部弹窗：`BaseBottomPushDialogFragment<VB>`。
- 极小的 ViewBinding-only 弹窗可以使用 `Dialog(context, style)`。

参数和结果：

- 弹窗参数通过 `arguments` 传递，适合时在 ViewModel 中从 `mBundle` 读取。
- 按钮点击只做 UI 事件转发或关闭弹窗，业务动作放在 ViewModel 或宿主页面。
- Dialog 结果返回优先使用明确 callback、Fragment Result 或宿主提供的方法，不要滥用全局事件。
- 弹窗内部如包含列表，同时使用列表 skill；如来自 Figma/截图，同时使用 Figma UI skill。

## Figma UI

Figma/截图到 Android XML UI 还原规范已抽成 skill：[epnova-android-figma-xml-ui](skills/epnova-android-figma-xml-ui/SKILL.md)。

触发条件：

- 用户提供 Figma 链接、节点、截图、标注图或设计稿导出资源。
- 用户要求“还原设计稿”“按 Figma 实现”“像素级还原”“精确实现 UI”。
- 任务目标是生成页面、组件、弹窗、表单、卡片、列表、仪表盘等可见 UI。

执行要求：

1. 优先读取设计源。有 Figma MCP 工具时优先读取节点数据、设计 token、布局属性和资源；不要用 WebFetch 读取 Figma URL。
2. 精确还原视觉属性：尺寸、间距、颜色、字体、字号、字重、行高、圆角、边框、阴影、透明度、层级、对齐方式。
3. 精确还原布局关系：栅格、约束、Auto Layout/Flex 方向、gap、padding、宽高、最小/最大尺寸和响应式行为。
4. Android XML 实现时，先查 `styles.xml`、`attrs.xml`、`colors.xml`、`dimens.xml`、已有 drawable/selector 和 BindingAdapter。
5. 优先映射到项目已有 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector`，不要使用 CSS/SCSS 变量等 Web 语境。
6. 字体尽量使用本地已有 `@style/Text.Bold`、`@style/Text.SemiBold`、`@style/Text.Medium`、`@style/Text.Regular`、`Text28/Text20/Text18/Text16/Text14` 等 style。
7. 识别到 Figma 标注 `Text1`、`Text2`、`Text3`、`Text4`、`Text5` 等文本层级时，优先映射为 `?attr/textColor1`、`?attr/textColor2`、`?attr/textColor3`、`?attr/textColor4`、`?attr/textColor5`。
8. 涉及 TextView、Button、EditText、标签等文案控件时，宽高尽量不要写死；优先使用 `wrap_content`、`0dp` 约束、`minWidth/minHeight`、`maxWidth`、`maxLines`、`ellipsize`、padding/margin。
9. 图标、图片、插画、背景、头像等视觉资产应使用设计稿导出资源或项目已有资产，不要用随意占位图替代明确资产。
10. 按钮、输入框、选择器、Tab、弹窗等组件需要覆盖设计中出现或业务必需的状态。
11. 不得擅自调整品牌色、圆角风格、阴影层级、字体层级、布局密度或组件形态。

输出时说明已按 Figma 精确模式处理的范围，并列出无法精确还原的内容及原因。

## Room

Room 是数据库场景，不是普通 API 模型场景。

- Room Entity 写在 `core/db/entity/`。
- Room Dao 写在 `core/db/dao/`，并在 `AppDatabase` 中注册。
- 数据库模型和接口/UI 模型有差异时，在 `core/db/mapper/` 或 feature 内新增 mapper。
- 新增或修改 Room Entity 时，必须检查 `AppDatabase` version。
- 修改表结构、字段、索引、主键时，必须同步处理 Migration 或 AutoMigration。
- 禁止随意使用 `fallbackToDestructiveMigration`。
- 除非用户明确允许清空本地数据，否则不能生成破坏性迁移。
- 如果无法判断当前数据库版本和历史 schema，必须在输出中标明需要人工确认。

## 支付、广告、登录、统计

广告、支付、推送、统计、登录、归因等能力必须隔离在 `core/manager/` 或对应 `core/google/`、`core/facebook/` 包下。

通用规则：

- 业务页面不要直接散落第三方 SDK 调用。
- 业务代码不要直接 import 第三方 SDK 类，除非该文件就是平台实现层。
- Manager 类应依赖接口，不依赖具体 SDK 类型。
- 涉及支付、登录、授权、广告奖励发放时，不能只依赖客户端状态作为最终结果。
- 不在日志中输出订单号、支付参数、授权 code、token 或用户敏感信息。

已有模式：

- 广告平台接口：`core/manager/ad/IRewardedAdPlatform.kt`
- Unity 广告实现：`core/manager/ad/UnityRewardedAdPlatform.kt`
- 奖励广告统一入口：`core/manager/RewardedAdManager.kt`
- Google Billing：`core/google/billing/BillingManager.kt`
- 统计：`core/manager/EventManager.kt`

## 页面跳转与事件通信

页面跳转和事件通信已抽成 skill：[epnova-android-navigation-events](skills/epnova-android-navigation-events/SKILL.md)。

- 服务端下发、Push、H5、scheme、deep link 等外部可配置跳转优先走 `AppRouter.jump(...)` 或 `AppRouter.openFromPush(...)`。
- 登录拦截、真实登录判断、登录页/弹窗和 Token 过期统一走 `AppNavigator`、`SignUtils`、`Events.UserChangedEvent`。
- 明确目标页面且调用方就在同业务上下文时，优先使用目标 Activity 的 `start(...)` / `getIntent(...)`。
- 跨页面一对多刷新用 `FlowEventBus` + `Events` + `observeEvents`，事件必须带足过滤字段，观察方按 `isActive` 和业务 id 过滤。
- 主 tab 切换通过 `Events.MainTabEvent` 交给 `MainActivity` 统一处理。
- 父子页面、Dialog 结果、Adapter item 点击和页面内部一次性事件不要滥用全局事件。

## Paging、SSE、轮询

分页场景使用 [epnova-android-paging-state-ui](skills/epnova-android-paging-state-ui/SKILL.md)。

- Paging 使用 `Pager(...).flow.cachedIn(viewModelScope)`。
- 分页接口返回 `ApiResponse<PagedList<T>?>` 时，页面实现必须走项目分页规范，不要用普通列表请求临时拼分页。
- `RemoteMediator` / `VideoRemoteMediator` 仅在明确需要数据库缓存时使用。

异步长任务使用 [epnova-android-async-task](skills/epnova-android-async-task/SKILL.md)。

- 后端任务状态轮询：优先使用 `launchPolling(...)`，保存返回的 `Job`。
- SSE 流式文本生成：使用 `SSEStreamHandler` + 对应 `SseChunkProcessor`，不要手写 OkHttp SSE。
- 下载/导出视频：复用 `DownloadTask`、`DownloadingDialog`、`DownloadingViewModel.DownloadStatusHolder`。
- 本地播放器进度、视频合成状态、临时 UI 进度模拟：可以保存普通 `Job`，用 `while (isActive)` 或延迟递归，但必须有取消入口。
- `onCleared()` 必须取消所有保存的 `Job`、SSE handler、下载任务、播放器/合成器等资源。
- `CancellationException` 必须放行。

## 多语言资源

新增文案必须走 string 资源，且只写入以下文件：

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `app/src/main/res/values-th/strings.xml`
- `app/src/main/res/values-in/strings.xml`

规则：

- 新增前先检查重复 key 和相近文案。
- 新 key 插入到相关业务分组附近。
- key 使用语义名，不用页面位置名，例如 `feedback_submit_success` 优于 `text_button_1`。
- 不把设计说明文案写进页面 UI，除非设计稿本身包含这些文案。
- 多语言文案长度可能差异很大，涉及 TextView、Button、EditText、标签时避免写死宽高。
- 如果无法提供准确翻译，可以先保留英文/中文占位，但输出中必须标明需要人工确认。

## 资源命名

资源命名细则优先使用 [epnova-android-naming](skills/epnova-android-naming/SKILL.md)。

layout 文件：

- Activity：`activity_xxx.xml`
- Fragment：`fragment_xxx.xml`
- Dialog：`dialog_xxx.xml`
- RecyclerView item：`item_xxx.xml`
- include 片段：`layout_xxx.xml` 或 `include_xxx.xml`

drawable 文件：

- 图标：`icon_xxx.xml` 或 `ic_xxx.xml`
- 图片：`img_xxx.webp/png`
- 纯色背景：`bg_<透明值>_<颜色值>_<圆角位置><圆角值>.xml`
- 选择器：`selector_xxx.xml`、`cb_xxx.xml`
- 渐变：`gradient_<透明值1>_<颜色值1>_<透明值2>_<颜色值2>_<方向>_<圆角位置><圆角值>.xml`
- 圆形/圆角：`oval_xxx.xml`、`shape_xxx.xml`
- 进度条：`progress_xxx.xml`

drawable 细则：

- 透明值使用两位小写 16 进制，不带 `#`，例如 `00`、`20`、`50`、`80`、`ff`。
- 颜色值使用六位小写 16 进制，不带 `#`，例如 `ffffff`、`000000`、`121212`。
- 圆角值只写 dp 数字，不带 `dp`，例如 `r8`、`r12`、`tr16`。
- 胶囊类背景一律使用 `r50`，例如 `bg_ff_ffffff_r50.xml`。
- `r<值>` 表示四角圆角，`tr<值>` 表示顶部圆角，`br<值>` 表示底部圆角。
- 通用纯色背景优先用值命名，例如 `bg_20_ffffff_r8.xml`、`bg_80_121212_r16.xml`、`bg_ff_f5f5f5_tr24.xml`。
- 新增通用渐变必须先写两个色值，再写方向和圆角，例如 `gradient_ff_fff1ea_ff_ffffff_top_r16.xml`、`gradient_ff_ff6159_ff_d91f1f_left_r16.xml`、`gradient_80_121212_80_1c1c2d_top_tr16.xml`。
- 渐变方向可用 `top`、`bottom`、`left`、`right`、`ltop`、`rtop`、`lbottom`、`rbottom`。
- 历史资源里已有方向前置命名时不强制重命名，新资源使用色值前置顺序。
- 界面特有或功能语义强的资源可以用 `shape_bg_<功能>_<状态或角色>.xml`，例如 `shape_bg_publish_task.xml`、`shape_bg_story_upload.xml`。
- 不使用 `bg_gray.xml`、`shape_1.xml`、`gradient_button.xml` 这类不可搜索的泛名。

color 资源：

- 通用透明度颜色沿用 `color_XX_xxxxxx`。
- 业务语义颜色使用 `func_success_dark`、`text_color_3_dark` 这类语义命名。

dimen 资源：

- 通用间距优先复用 `spacing_xs/s/m/l`、`radius_s/m/l`。
- 业务独有尺寸使用 `业务名_属性`，例如 `main_bottom_nav_height`。
