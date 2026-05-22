# Epnova Android Kotlin 代码生成总规范

本文档是 Epnova Android 代码生成的总入口，只保留项目级强约束和通用规则。场景细节和检查清单拆到独立文档，避免单文件继续膨胀。

## 文档分层

1. 总规范：当前文件，说明项目背景、禁止项、目录、ViewModel、Repository、API、XML、异步等基础规则。
2. 场景规范：[android-codegen-scenarios-epnova.md](android-codegen-scenarios-epnova.md)，说明列表、弹窗、Figma UI、Room、SDK 隔离、Paging/SSE/轮询、多语言等场景规则。
3. 代码审查 Checklist：[android-codegen-checklist-epnova.md](android-codegen-checklist-epnova.md)，用于生成前后检查、禁止误引入项和风险提示格式。

使用优先级：

1. 用户本次明确要求。
2. 当前项目中同类功能的现有实现。
3. 本总规范。
4. 场景规范和项目内 skill。
5. Checklist。
6. Android 官方通用建议。

当 AGENTS、旧通用模板或外部建议与本规范体系冲突时，以本规范体系为准。场景规范不得覆盖本文件中的禁止项；如果场景规范更具体且不违反总规范，按场景规范执行。

## 项目背景

- 主业务包名：`com.kuaigeglobal.epnova`
- 主业务工程：`app`
- 现有本地库模块：`apisecurity`、`datepicker`、`magicindicator`、`matisse`
- 新增业务代码优先放在 `app`，不要新增 Gradle Module
- 架构：MVVM + Repository，基于 `nhsd.basic`
- UI：XML + DataBinding/ViewBinding，当前项目已启用 DataBinding 和 ViewBinding
- 语言：Kotlin 为主，保留少量历史 Java
- 网络：Retrofit + `ApiResponse<T>`
- 数据库：Room，入口为 `core/db/AppDatabase.kt`
- 协程：优先使用项目已有 `launch`、`request`、`requestWithLoading`、`launchPolling`

## 禁止项

- 不要把本项目当成纯 ViewBinding 小项目处理。页面 XML 已普遍使用 `<layout>`、`layout` 变量、`item` 变量和自定义 `BindingAdapter`。
- 不要新增 `AppContainer`、`ViewModelFactory` 或 `di` 包。当前 `nhsd.basic` 已负责 `binding`、`mRealVM`、`mRepo`、`mBundle` 等注入。
- 不要新增 Hilt 相关代码。禁止生成 `@Inject`、`@HiltViewModel`、`@AndroidEntryPoint`、`@Module`、`@InstallIn`、`@Provides`、`@Binds`。
- 不要为了“标准 Clean Architecture”新增 `domain`、`UseCase`、`AppResult`、`AppError`。
- 不要默认把接口实体再包装成 Domain Model / UiModel。
- 不要修改 `ApiResponse.java`，除非用户明确要求做全局网络层改造。
- 不要新增多 Gradle Module，除非用户明确要求重构工程结构。
- 不要在 Activity/Fragment 中直接请求网络或访问数据库。
- 不要在 Adapter 中写业务请求。
- 不要在业务代码里直接散落第三方 SDK 调用。
- 不要硬编码 token、secret、api key、私钥。
- 不要在日志中输出 token、手机号、邮箱、用户 ID、订单号、支付参数、授权 code。
- 不要检查或修改 `OssManager.kt`，除非用户明确要求。

接口实体可以直接用于简单 UI 展示，但禁止在接口实体中加入 UI 专用状态字段，例如：

- `isSelected`
- `isExpanded`
- `isEditing`
- `localProgress`
- `displayText`
- `errorMessage`
- `isChecked`

如果列表 item 需要选中态、展开态、编辑态、上传进度、本地临时状态，优先使用 `ItemXxxVM` 包装接口实体，或新增 `XxxUiModel`。

## 目录规则

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

## Base 类规则

- 新页面/页面骨架规范已抽成 skill：[epnova-android-feature-scaffold](skills/epnova-android-feature-scaffold/SKILL.md)。
- Activity 直接继承 `com.nhsd.basic.BaseViewModelActivity<VB, VM>`。
- Fragment 直接继承 `com.nhsd.basic.BaseViewModelFragment<VB, VM>`。
- 普通 ViewModel 继承 `AppViewModel`。
- 需要仓库的 ViewModel 继承 `AppRepositoryViewModel<T>`。
- `AppViewModel.id()` 和 `AppRepositoryViewModel.id()` 已固定返回 `BR.layout`，不要重复实现。
- 列表 item VM 继承 `AppItemViewModel<T>`，其 DataBinding 变量名固定为 `item`。
- 页面中直接使用基类注入对象：`binding`、`mRealVM`、`mRepo`、`mBundle`。

## XML 与 UI 总规则

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

涉及 Figma、截图还原、页面/弹窗/卡片等可见 UI 时，按场景规范中的 Figma UI 规则执行，优先读取设计源并精确还原尺寸、颜色、字体、间距、圆角、阴影和资源。

## ViewModel 规则

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

## Repository 规则

- 通用仓库继承 `AppRepository` 或 `CommonRepository`。
- 功能仓库放在对应 feature 下，例如 `feature/series/SeriesRepository.kt`。
- Repository 可以调用 `api`、`appDatabase`、MMKV、SharedPreferences、DataStore 或文件缓存。
- Repository 返回类型优先保持 `ApiResponse<T>` 或项目已有业务模型，不为单个新功能单独引入 `AppResult<T>`。
- 需要 IO 切换时，Repository 方法可以使用 `withContext(Dispatchers.IO)`。
- API 错误处理优先交给 `response(...)`、`request(...)`、`requestWithLoading(...)`，不要在 Repository 中随意吞异常。
- 只有当错误文案、重试策略、缓存降级等属于数据层职责时，才在 Repository 中封装。

## API 与模型规则

- Retrofit 接口接入与 ViewModel 请求调用规范已抽成 skill：[epnova-android-api-request](skills/epnova-android-api-request/SKILL.md)。
- Retrofit 方法统一写在 `core/network/apis/ApiService.kt`。
- API 路径常量写在 `ApiConstants.kt`。
- 请求体写在 `core/network/apis/request/`。
- 响应聚合模型写在 `core/network/apis/response/`。
- 接口实体写在 `core/network/apis/entity/`。
- Room Entity 写在 `core/db/entity/`。
- Room Dao 写在 `core/db/dao/`，并在 `AppDatabase` 中注册。
- 数据库模型和接口/UI 模型有差异时，在 `core/db/mapper/` 或 feature 内新增 mapper。

ViewModel 可以通过 `mRepo.api.xxx(...)` 处理简单请求，但必须同时满足：

- 只服务当前页面。
- 不涉及数据库、缓存、文件、MMKV、DataStore。
- 不涉及多接口组合。
- 不涉及复杂错误降级、重试、兜底数据。
- 不被多个页面复用。
- 不需要复杂模型转换。

否则必须在 Repository 中新增方法，由 ViewModel 调用 `mRepo.xxx(...)`。

## 异步与错误处理总规则

轮询、SSE、生成任务、下载任务等异步长任务已抽成 skill：[epnova-android-async-task](skills/epnova-android-async-task/SKILL.md)。

优先使用项目已有扩展：

- 无 loading 的网络请求：`request({ mRepo.api.xxx(body) }) { ... }`
- 用户触发且需要 loading 的请求：`requestWithLoading({ mRepo.api.xxx(body) }) { ... }`
- 自定义协程：`launch { ... }`
- 带 loading 的自定义协程：`launchWithLoading { ... }`
- 轮询：`launchPolling(...)`

长任务总规则：

- 后端任务状态检查优先使用 `launchPolling(...)`，保存返回的 `Job` 并在 `onCleared()` 取消。
- SSE 流式生成优先使用 `SSEStreamHandler`，启动新流前取消旧 handler。
- 下载/导出进度优先复用 `DownloadingViewModel.DownloadStatusHolder`、`DownloadingDialog` 和已有 `DownloadTask`。
- 所有长任务必须有重复启动保护，并在成功、失败、异常、取消后复位 loading/progress/generating 等 UI 状态。
- `CancellationException` 必须放行，不要吞掉协程取消。

错误处理原则：

- 接口失败默认通过 `response(...)` 的 error 分支或 `requestWithLoading(showToastError = true)` 处理。
- 需要自定义失败 UI 时使用 `onError` 回调。
- UI 只展示用户能理解的 Toast/Dialog/状态页，不展示原始堆栈。

## UseCase 与 MVI

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

## 命名与资源总规则

命名细则已抽成 skill：[epnova-android-naming](skills/epnova-android-naming/SKILL.md)。生成或审查包名、类名、变量、函数、常量、View id、layout、drawable、string key 时优先使用该 skill。

关键摘要：

- Kotlin 缩进 4 空格。
- 优先安全调用和 Elvis 表达式，避免 `!!`。
- 单行长度尽量控制在 120 字符左右。
- 类名使用 `XxxActivity`、`XxxFragment`、`XxxViewModel`、`XxxRepository`、`XxxDialog`、`ItemXxxVM`。
- 常量放到 `companion object`，使用 upper snake case。
- Boolean 命名以 `is`、`has`、`can`、`should` 开头。
- 事件回调以 `onXxx` 命名。
- Intent extra、Bundle key、MMKV key、SharedPreferences key 必须抽成常量。
- Job 变量使用 `xxxJob`。
- layout 文件使用 `activity_xxx.xml`、`fragment_xxx.xml`、`dialog_xxx.xml`、`item_xxx.xml`。
- 通用纯色背景使用 `bg_<透明值>_<颜色值>_<圆角位置><圆角值>.xml`。
- 胶囊类背景一律使用 `r50`。
- 通用渐变使用 `gradient_<透明值1>_<颜色值1>_<透明值2>_<颜色值2>_<方向>_<圆角位置><圆角值>.xml`。
- 界面特有或功能语义强的资源可以用 `shape_bg_<功能>_<状态或角色>.xml`。

## Skill 索引

- 页面骨架：[epnova-android-feature-scaffold](skills/epnova-android-feature-scaffold/SKILL.md)
- API 请求：[epnova-android-api-request](skills/epnova-android-api-request/SKILL.md)
- 命名：[epnova-android-naming](skills/epnova-android-naming/SKILL.md)
- 分页与状态 UI：[epnova-android-paging-state-ui](skills/epnova-android-paging-state-ui/SKILL.md)
- 非分页列表：[epnova-android-databinding-list](skills/epnova-android-databinding-list/SKILL.md)
- 弹窗：[epnova-android-dialog](skills/epnova-android-dialog/SKILL.md)
- 页面跳转与事件通信：[epnova-android-navigation-events](skills/epnova-android-navigation-events/SKILL.md)
- Figma 到 XML UI：[epnova-android-figma-xml-ui](skills/epnova-android-figma-xml-ui/SKILL.md)
- 异步长任务：[epnova-android-async-task](skills/epnova-android-async-task/SKILL.md)

## 安全与日志

- 禁止硬编码 token、secret、api key、私钥。
- 禁止在日志中输出 token、手机号、邮箱、用户 ID、订单号、支付参数、授权 code。
- 禁止把接口完整 request/response 原样打印到线上日志。
- UI 不展示原始异常堆栈。
- 涉及支付、登录、授权、广告奖励发放时，不能只依赖客户端状态作为最终结果。

## 忽略规则

- 等同 Claude ignore：`**/OssManager.kt`。
- 除非用户明确要求，不要查看或编辑 `OssManager.kt`。
