# Epnova Codex Project Memory

## Response And Collaboration

- Always answer in Chinese.
- Prefer concise, direct engineering communication.
- Project owner is an Android developer working on the Epnova AI short-drama creation app.
- Main language is Kotlin, with some legacy Java.
- Package name: `com.kuaigeglobal.epnova`.
- The project shares the same `nhsd.basic` base framework style as FastFilm.
- When this file conflicts with `design/android-codegen-guidelines-epnova.md`, use `design/android-codegen-guidelines-epnova.md` as the source of truth.

## Project Baseline

- 主业务包名：`com.kuaigeglobal.epnova`
- 主业务工程：`app`
- 现有本地库模块：`apisecurity`、`datepicker`、`magicindicator`、`matisse`。新增业务代码仍优先放在 `app`，不要新增 Gradle Module。
- 现有架构：MVVM + Repository，基于 `nhsd.basic`
- UI 技术：XML + DataBinding/ViewBinding，当前项目已启用 DataBinding 和 ViewBinding
- 代码语言：Kotlin 为主，保留少量历史 Java
- 网络：Retrofit + `ApiResponse<T>`
- 数据库：Room，入口为 `core/db/AppDatabase.kt`
- 协程：优先使用项目已有 `launch`、`request`、`requestWithLoading`、`launchPolling`

## Architecture Rules

新增代码必须贴合当前项目，而不是套用通用 Android 模板：

- 不要把本项目当成纯 ViewBinding 小项目处理。页面 XML 已普遍使用 `<layout>`、`layout` 变量、`item` 变量和自定义 `BindingAdapter`。
- 不要新增 `AppContainer`、`ViewModelFactory` 或 `di` 包。当前 `nhsd.basic` 已负责 `binding`、`mRealVM`、`mRepo`、`mBundle` 等注入。
- 不要新增 Hilt 相关代码。禁止生成 `@Inject`、`@HiltViewModel`、`@AndroidEntryPoint`、`@Module`、`@InstallIn`、`@Provides`、`@Binds`。
- 不要为了“标准 Clean Architecture”新增 `domain`、`UseCase`、`AppResult`、`AppError`。现阶段应沿用项目的 `ApiResponse<T>`、`response(...)` 和 `ApiException` 处理链。
- 不要默认把接口实体再包装成 Domain Model / UiModel。项目中 `core/network/apis/entity` 下的模型已被 UI、ItemVM 和业务逻辑直接使用。只有页面显示字段明显复杂、需要组合多个来源或防止接口字段污染 UI 时，才新增轻量 UiModel/Mapper。

## Package Layout

新增功能优先放在 `feature/<业务域>/` 下，和现有页面保持同一风格。

- `base/`：项目基础 ViewModel、ItemVM、RecyclerViewVM 等，不随意扩展。
- `common/`：通用 adapter、扩展函数、工具类、权限、LiveData 等。
- `core/network/apis/ApiService.kt`：Retrofit API 定义。
- `core/network/apis/request/`：请求体。
- `core/network/apis/response/`：接口响应模型，偏聚合数据。
- `core/network/apis/entity/`：接口实体模型，当前项目中也会被页面直接使用。
- `core/db/dao/`：Room Dao。
- `core/db/entity/`：Room Entity。
- `core/db/mapper/`：数据库模型转换。
- `core/data/CommonRepository.kt`：通用仓库基类。
- `core/manager/`：第三方 SDK、广告、支付、统计、登录等平台能力隔离。
- `router/`：页面路由和跳转。
- `widget/`：自定义 View、Dialog、状态组件。

不要新增多 Gradle Module，除非用户明确要求重构工程结构。

## Base Class Rules

- Activity 直接继承 `com.nhsd.basic.BaseViewModelActivity<VB, VM>`。
- Fragment 直接继承 `com.nhsd.basic.BaseViewModelFragment<VB, VM>`。
- 普通 ViewModel 继承 `AppViewModel`。
- 需要仓库的 ViewModel 继承 `AppRepositoryViewModel<T>`。
- `AppViewModel.id()` 和 `AppRepositoryViewModel.id()` 已固定返回 `BR.layout`，不要重复实现。
- 列表 item VM 继承 `AppItemViewModel<T>`，其 DataBinding 变量名固定为 `item`。
- 中间弹窗优先使用 `BaseVMDialogFragment<VB, VM>` 或 `BaseDialogFragment<VB>`。
- 底部弹窗优先使用 `BaseBottomPushVMDialogFragment<VB, VM>` 或 `BaseBottomPushDialogFragment<VB>`。
- 页面中直接使用基类注入对象：`binding`、`mRealVM`、`mRepo`、`mBundle`。

## UI And XML

- XML 默认使用 `<layout>` 根节点。
- 页面 ViewModel 变量名必须是 `layout`。
- 列表 item 变量名必须是 `item`。
- 页面初始化优先放在 Fragment 的 `onPrepareView()` 中，并拆成 `initView()`、`initListener()`、`initObservers()` 或现有相近命名。
- Activity 可以在 `onCreate()` 做启动初始化，但网络和业务动作仍应委托给 ViewModel。
- 点击事件优先使用 `setOnSingleClickListener` 或 XML 中 `layout.clickHandler::onClick`，不要直接使用裸 `setOnClickListener`。
- XML 可以写简单可读的 DataBinding 表达式，复杂业务判断放到 ViewModel 方法或 BindingAdapter。
- 图片加载优先使用项目已有 `loadUri`、`loadHQThumbnailUri`、`loadThumbnailUri`、`loadVideoUrl` 等 BindingAdapter。
- 文案必须走字符串资源，新增 key 只放到以下文件并检查重复：
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh/strings.xml`
  - `app/src/main/res/values-th/strings.xml`
  - `app/src/main/res/values-in/strings.xml`

## ViewModel Rules

- ViewModel 负责页面状态、用户事件处理、调用 Repository、调度协程和处理页面业务。
- ViewModel 不直接依赖 Activity、Fragment、View。
- 简单网络请求可以沿用现有项目风格，通过 `mRepo.api.xxx(...)` 配合 `response(...)`、`request(...)` 或 `requestWithLoading(...)` 调用。
- 当同一业务存在多处复用、包含 IO 切换、数据库组合、缓存策略或多接口组合时，在对应 feature 下新增 Repository 方法，由 ViewModel 调用 `mRepo.xxx(...)`。
- 不直接创建 `ApiService` 或 `AppDatabase`，统一通过 `mRepo.api` 和 `mRepo.appDatabase`。
- 页面状态优先使用 `MutableLiveData`、`ObservableField`、`ObservableBoolean`、`ObservableInt`，和现有 XML DataBinding 风格保持一致。
- 已有 Paging、SSE、播放器状态等 Flow 场景可以使用 `StateFlow` / `Flow`。
- Paging 使用 `Pager(...).flow.cachedIn(viewModelScope)`。
- 轮询、SSE、下载、支付监听等长任务必须保存 `Job` 或 handler，并在 `onCleared()` 中取消。
- 避免为了“不可变暴露”大规模改造已有 `MutableLiveData` 公有字段。新增复杂状态可以采用私有 `MutableLiveData` + 公开 `LiveData`，但以不破坏项目一致性为准。

## Repository Rules

- 通用仓库继承 `AppRepository` 或 `CommonRepository`。
- 功能仓库放在对应 feature 下，例如 `feature/series/SeriesRepository.kt`。
- Repository 可以调用 `api`、`appDatabase`、MMKV、SharedPreferences、DataStore 或文件缓存。
- Repository 返回类型优先保持 `ApiResponse<T>` 或项目已有业务模型，不为单个新功能单独引入 `AppResult<T>`。
- 需要 IO 切换时，Repository 方法可以使用 `withContext(Dispatchers.IO)`。
- API 错误处理优先交给 `response(...)`、`request(...)`、`requestWithLoading(...)`，不要在 Repository 中随意吞异常。
- 只有当错误文案、重试策略、缓存降级等属于数据层职责时，才在 Repository 中封装。

## API And Models

- Retrofit 方法统一写在 `core/network/apis/ApiService.kt`。
- API 路径常量写在 `ApiConstants.kt`。
- 请求体写在 `core/network/apis/request/`。
- 响应聚合模型写在 `core/network/apis/response/`。
- 接口实体写在 `core/network/apis/entity/`。
- Room Entity 写在 `core/db/entity/`。
- Room Dao 写在 `core/db/dao/`，并在 `AppDatabase` 中注册。
- 数据库模型和接口/UI 模型有差异时，在 `core/db/mapper/` 或 feature 内新增 mapper。
- 不修改 `ApiResponse.java`，除非用户明确要求做全局网络层改造。

## Async And Error Handling

优先使用项目已有扩展：

- 无 loading 的网络请求：`request({ mRepo.api.xxx(body) }) { ... }`
- 用户触发且需要 loading 的请求：`requestWithLoading({ mRepo.api.xxx(body) }) { ... }`
- 自定义协程：`launch { ... }`
- 带 loading 的自定义协程：`launchWithLoading { ... }`
- 轮询：`launchPolling(...)`

错误处理原则：

- 接口失败默认通过 `response(...)` 的 error 分支或 `requestWithLoading(showToastError = true)` 处理。
- 需要自定义失败 UI 时使用 `onError` 回调。
- `CancellationException` 必须放行，不要吞掉协程取消。
- UI 只展示用户能理解的 Toast/Dialog/状态页，不展示原始堆栈。

## RecyclerView

- 分页加载 + 状态 UI 规范已抽成项目内 skill：[epnova-android-paging-state-ui](design/skills/epnova-android-paging-state-ui/SKILL.md)。生成或审查 Paging3、SmartRefreshLayout、LoadStateViewHolder、MyMultiStatePagingFooterAdapter、RemoteMediator 相关代码时优先使用该 skill；RemoteMediator/VideoRemoteMediator 仅在明确需要数据库缓存时作为可选分支。
- 普通 DataBinding 列表优先使用 `QuickAdapter<ItemXxxVM>` + `AppItemViewModel<T>`。
- item layout 变量固定为 `item`。
- 简单列表更新使用 `submitList(newItems)`。
- 只有强制全量刷新时才使用 adapter 的 `refresh()`。
- 多类型列表可以使用 `BaseMultiItemAdapter<AppItemViewModel<*>>`，参考 `AssetManagementAdapter`、`StoryboardDesignAdapter`。
- 分页列表使用 Paging3，按现有 `PagingSource`、`RemoteMediator`、`PagingDataAdapter` 风格实现。
- Adapter 只负责渲染和转发点击事件，不直接请求网络。

## UseCase And MVI

默认不生成 UseCase，也不生成完整 MVI。

可以新增 UseCase 的条件：

- 同一业务逻辑被多个 ViewModel 复用。
- 需要组合多个 Repository。
- 规则计算明显复杂，放在 ViewModel 会膨胀。
- 需要独立测试核心业务逻辑。

可以新增轻量 UiState 的条件：

- 页面同时存在 loading、empty、error、content、editing、selecting 等多种状态。
- 页面事件很多，多个异步结果会交叉影响 UI。
- Flow/SSE/Paging/轮询混合后，单独 LiveData 难以维护。

不要生成 Store、Reducer、Intent、Effect 全套结构，除非用户明确要求。

## Third-Party SDK Isolation

广告、支付、推送、统计、登录、归因等能力必须隔离在 `core/manager/` 或对应 `core/google/`、`core/facebook/` 包下。

- 业务页面不要直接散落第三方 SDK 调用。
- 业务代码不要直接 import 第三方 SDK 类，除非该文件就是平台实现层。
- Manager 类应依赖接口，不依赖具体 SDK 类型。
- 已有模式：
  - 广告平台接口：`core/manager/ad/IRewardedAdPlatform.kt`
  - Unity 广告实现：`core/manager/ad/UnityRewardedAdPlatform.kt`
  - 奖励广告统一入口：`core/manager/RewardedAdManager.kt`
  - Google Billing：`core/google/billing/BillingManager.kt`
  - 统计：`core/manager/EventManager.kt`

## Naming And Code Style

命名细则已抽成项目内 skill：[epnova-android-naming](design/skills/epnova-android-naming/SKILL.md)。生成或审查命名时优先使用该 skill。

- Kotlin 缩进 4 空格。
- 优先安全调用和 Elvis 表达式，避免 `!!`。
- 单行长度尽量控制在 120 字符左右。
- 类名使用 `XxxActivity`、`XxxFragment`、`XxxViewModel`、`XxxRepository`、`XxxDialog`、`ItemXxxVM`。
- 常量放到 `companion object`，使用 upper snake case。
- Boolean 命名以 `is`、`has`、`can`、`should` 开头。
- 事件回调以 `onXxx` 命名。
- Intent extra、Bundle key、MMKV key、SharedPreferences key 必须抽成常量。
- `LiveData`/`MutableLiveData` 名称使用业务含义，不加 `LiveData` 后缀，例如 `workInfo`、`loginTaskData`。
- `StateFlow`/`MutableStateFlow` 名称使用业务含义，必要时可加 `Flow` 表示连续流，例如 `feedFlow`。
- Adapter 变量使用 `xxxAdapter`。
- Job 变量使用 `xxxJob`。
- Boolean 命名以 `is`、`has`、`can`、`should` 开头。
- 列表集合使用复数或 `xxxList`。

## Resource Naming

- layout 文件：
  - Activity：`activity_xxx.xml`
  - Fragment：`fragment_xxx.xml`
  - Dialog：`dialog_xxx.xml`
  - RecyclerView item：`item_xxx.xml`
  - include 片段：`layout_xxx.xml` 或 `include_xxx.xml`
- drawable 文件：
  - 图标：`icon_xxx.xml` 或 `ic_xxx.xml`
  - 图片：`img_xxx.webp/png`
  - 纯色背景：`bg_<透明值>_<颜色值>_<圆角位置><圆角值>.xml`
  - 选择器：`selector_xxx.xml`、`cb_xxx.xml`
  - 渐变：`gradient_<透明值1>_<颜色值1>_<透明值2>_<颜色值2>_<方向>_<圆角位置><圆角值>.xml`
  - 圆形/圆角：`oval_xxx.xml`、`shape_xxx.xml`
  - 进度条：`progress_xxx.xml`
- 透明值使用两位小写 16 进制，不带 `#`，例如 `00`、`20`、`50`、`80`、`ff`。
- 颜色值使用六位小写 16 进制，不带 `#`，例如 `ffffff`、`000000`、`121212`。
- 圆角值只写 dp 数字，不带 `dp`，例如 `r8`、`r12`、`tr16`。
- 胶囊类背景一律使用 `r50`，例如 `bg_ff_ffffff_r50.xml`。
- `r<值>` 表示四角圆角，`tr<值>` 表示顶部圆角，`br<值>` 表示底部圆角。
- 通用纯色背景优先用值命名，例如 `bg_20_ffffff_r8.xml`、`bg_80_121212_r16.xml`、`bg_ff_f5f5f5_tr24.xml`。
- 新增通用渐变必须先写两个色值，再写方向和圆角，例如 `gradient_ff_fff1ea_ff_ffffff_top_r16.xml`、`gradient_ff_ff6159_ff_d91f1f_left_r16.xml`、`gradient_80_121212_80_1c1c2d_top_tr16.xml`。
- 渐变方向可用 `top`、`bottom`、`left`、`right`、`ltop`、`rtop`、`lbottom`、`rbottom`；历史资源里已有方向前置命名时不强制重命名，新资源使用色值前置顺序。
- 界面特有或功能语义强的资源可以用 `shape_bg_<功能>_<状态或角色>.xml`，例如 `shape_bg_publish_task.xml`、`shape_bg_story_upload.xml`。
- 不使用 `bg_gray.xml`、`shape_1.xml`、`gradient_button.xml` 这类不可搜索的泛名。

## Comments

- Use Chinese comments when comments are needed.
- Class comments may use the project style:

```kotlin
/**
 * @Introduce: XXX界面
 */
```

- Comments should explain why, not restate obvious code.
- TODO format: `//TODO xxx`.

## Dialogs

- Center dialog with ViewModel: `BaseVMDialogFragment<VB, VM>`。
- Bottom dialog with ViewModel: `BaseBottomPushVMDialogFragment<VB, VM>`。
- Simple center dialog without ViewModel: `BaseDialogFragment<VB>`。
- Very small ViewBinding-only dialog may use `Dialog(context, style)`。
- Dialog arguments should be passed through `arguments` and read from `mBundle` in ViewModel where applicable.

## Generation Checklist

生成代码前先判断：

- 这个功能已有相似页面或仓库吗？优先复用同类写法。
- 新页面是否应该继承 `BaseViewModelActivity` / `BaseViewModelFragment`？
- XML 是否需要 `<layout>`，变量名是否为 `layout` 或 `item`？
- API 是否应加到 `ApiService.kt`，请求体是否放在 `request/`？
- 是否真的需要新 Repository，还是 `AppRepository` 已足够？
- 是否真的需要 UiModel、UseCase、UiState？
- 是否有长任务需要在 `onCleared()` 取消？
- 是否新增了字符串资源并同步多语言文件？
- 是否误引入了 Hilt、AppContainer、ViewModelFactory、AppResult/AppError 等当前项目没有采用的结构？

## Figma And Design Access

- Access Figma designs through available MCP tools or curl with the Figma REST API.
- Do not use WebFetch for Figma URLs.
- Android UI implementation should first map Figma values to local `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector` and BindingAdapter resources.
- Do not describe or implement Android UI in CSS/SCSS terms. If a design token looks like a Web/CSS token, translate it into the closest Android project style/theme resource.

## Figma UI 精确实现规范

> 涉及 Figma 设计稿、截图还原、视觉走查、UI 页面/组件实现时，默认启用“精确模式”。

### 触发条件

以下任一情况必须按精确模式执行：

- 用户提供 Figma 链接、节点、截图、标注图或设计稿导出资源
- 用户要求“还原设计稿”“按 Figma 实现”“像素级还原”“精确实现 UI”
- 任务目标是生成页面、组件、弹窗、表单、卡片、列表、仪表盘等可见 UI

### 执行要求

1. **优先读取设计源**：有 Figma 插件/连接器时，优先读取 Figma 节点数据、设计 Token、布局属性和资源；没有可用连接器时，明确说明缺少设计源，并基于用户提供的截图/标注谨慎实现。
2. **精确还原视觉属性**：尺寸、间距、颜色、字体、字号、字重、行高、圆角、边框、阴影、透明度、层级、对齐方式必须按设计稿实现，不用“差不多”的估算值替代。
3. **精确还原布局关系**：保持设计稿中的栅格、约束、Auto Layout/Flex 方向、gap、padding、宽高、最小/最大尺寸和响应式行为。
4. **优先使用项目 Style/Theme/Attr**：Android XML 实现时，先查 `styles.xml`、`attrs.xml`、`colors.xml`、`dimens.xml`、已有 drawable/selector 和 BindingAdapter；优先映射到 `@style/Text.Bold`、`@style/Text.SemiBold`、`@style/Text.Medium`、`@style/Text.Regular`、`?attr/textColor1`、`?attr/bgColor1`、`?attr/themeColor1`、通用 `spacing/radius` 等项目规范。不要使用 CSS/SCSS 变量等 Web 语境；仅当项目没有对应规范时，才使用局部精确值并说明。
5. **文案与文本布局**：涉及 TextView、Button、EditText、标签等文案控件时，宽高尽量不要写死；优先使用 `wrap_content`、`0dp` 约束、`minWidth/minHeight`、`maxWidth`、`maxLines`、`ellipsize`、padding/margin 来适配文案长度。字体尽量使用本地已有 `Text.Bold`、`Text.SemiBold`、`Text.Medium`、`Text.Regular`、`Text28/Text20/Text18/Text16/Text14` 等 style，不要重复手写 `fontFamily`、`textStyle`。识别到 Figma 标注 `Text1`、`Text2`、`Text3`、`Text4`、`Text5` 等文本层级时，优先映射为项目实际主题色 `?attr/textColor1`、`?attr/textColor2`、`?attr/textColor3`、`?attr/textColor4`、`?attr/textColor5`。
6. **资产真实落地**：图标、图片、插画、背景、头像等视觉资产应使用设计稿导出资源或项目已有资产；不要用随意占位图替代明确资产。
7. **交互状态完整**：按钮、输入框、选择器、Tab、弹窗等组件需要覆盖默认、hover、active、focus、disabled、loading、error、empty 等设计中出现或业务必需的状态。
8. **禁止自行改设计**：不得擅自调整品牌色、圆角风格、阴影层级、字体层级、布局密度或组件形态；如设计稿存在明显缺失或矛盾，先说明假设再实现。
9. **实现后自检**：完成后检查实际页面与设计稿在首屏结构、关键间距、颜色、字体、对齐和响应式表现上的差异；能运行项目时，优先用浏览器截图或构建结果验证。

### 输出标准

- 说明已按 Figma 精确模式处理的范围。
- 列出无法精确还原的内容及原因，例如缺少字体文件、缺少图片资产、设计稿未提供移动端状态。
- 不把设计说明文案写进页面 UI，除非设计稿本身包含这些文案。

## Ignore Rules

- Claude ignore equivalent: `**/OssManager.kt`.
- Do not inspect or edit `OssManager.kt` unless explicitly requested.
