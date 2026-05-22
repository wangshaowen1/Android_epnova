# Epnova Codex 项目记忆

本文件是 AI 在当前项目中的调度入口，不承载完整细则。Android 代码生成规范以 `design/android-codegen-guidelines-epnova.md` 为总入口，并拆分到 `design/android-codegen-scenarios-epnova.md` 和 `design/android-codegen-checklist-epnova.md`。当本文件与上述规范文档冲突时，以上述规范文档为准。

## 回复与协作

- 始终使用中文回答。
- 优先使用简洁、直接的工程沟通方式。
- 修改代码前先读现有实现，不凭通用模板硬套。
- 说明取舍时给出项目内依据，例如同类页面、已有基类、已有 skill 或规范文档。
- 不确定的风险点直接标明，不用猜测填平。

## AI 执行流程

1. 先判断任务类型：问答 / 代码生成 / 代码审查 / UI 还原 / skill 抽取 / 文档维护。
2. Android 代码生成或修改任务，先读取 `design/android-codegen-guidelines-epnova.md` 总入口。
3. 命中具体场景时，再读取 `design/android-codegen-scenarios-epnova.md` 和对应 skill。
4. 修改代码前用 `rg` 查找同类页面、ViewModel、Repository、Adapter、Dialog、资源命名和调用方式。
5. 优先复用项目已有写法；只有找不到合适模式或现有模式明显不适用时，才新增轻量实现。
6. 修改范围保持最小，不做无关重构，不回退用户已有改动。
7. 修改后按 `design/android-codegen-checklist-epnova.md` 做最小相关检查。
8. 最终输出变更文件、复用内容、已验证项、未验证原因和需要人工确认的风险点。

## 文档读取策略

- 简单问答：优先读 AGENTS 和相关代码；只有涉及项目规范时再读 design 文档。
- Android 代码生成/修改：读总规范 + 相关场景规范 + 命中的 skill。
- UI/Figma/截图还原：读 Figma XML UI skill，并优先检查项目 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector` 和 BindingAdapter。
- 代码审查：读 Checklist，按风险优先输出，不把改动摘要放在问题前面。
- skill 抽取/更新：先读现有同类 skill，再更新 AGENTS 的 Skill 路由和总规范索引。
- 文档维护：优先保持“AGENTS 调度入口、design 总规范、design 场景规范、design Checklist、skills 细则”的分层。

## Skill 路由

生成或审查代码时，命中以下场景应优先读取对应 skill：

- 新页面/页面骨架：`design/skills/epnova-android-feature-scaffold/SKILL.md`
- API 接入/请求调用：`design/skills/epnova-android-api-request/SKILL.md`
- 命名/资源命名：`design/skills/epnova-android-naming/SKILL.md`
- 分页 + 状态 UI：`design/skills/epnova-android-paging-state-ui/SKILL.md`
- 非分页列表：`design/skills/epnova-android-databinding-list/SKILL.md`
- 弹窗：`design/skills/epnova-android-dialog/SKILL.md`
- 页面跳转/事件通信：`design/skills/epnova-android-navigation-events/SKILL.md`
- Figma/截图到 XML：`design/skills/epnova-android-figma-xml-ui/SKILL.md`
- 轮询/SSE/下载/生成任务：`design/skills/epnova-android-async-task/SKILL.md`

组合场景需要组合 skill，例如：

- 新页面 + API：`feature-scaffold` + `api-request`
- 新页面 + 分页：`feature-scaffold` + `paging-state-ui`
- 普通列表 + item 点击：`databinding-list` + 必要时 `navigation-events`
- Figma 弹窗：`figma-xml-ui` + `dialog`
- 生成任务页面：`feature-scaffold` + `api-request` + `async-task`

## 项目基础

- 当前项目是 Epnova AI 短剧创作应用。
- 主业务包名：`com.kuaigeglobal.epnova`
- 主业务工程：`app`
- 现有本地库模块：`apisecurity`、`datepicker`、`magicindicator`、`matisse`
- 主语言：Kotlin，保留少量历史 Java
- 架构：MVVM + Repository，基于 `nhsd.basic`
- UI：XML + DataBinding/ViewBinding，当前项目已启用 DataBinding 和 ViewBinding
- 网络：Retrofit + `ApiResponse<T>`
- 数据库：Room，入口为 `core/db/AppDatabase.kt`
- 协程：优先使用项目已有 `launch`、`request`、`requestWithLoading`、`launchPolling`

## 硬性禁止项

- 不要把本项目当成纯 ViewBinding 小项目处理。页面 XML 已普遍使用 `<layout>`、`layout` 变量、`item` 变量和自定义 `BindingAdapter`。
- 不要新增 `AppContainer`、`ViewModelFactory` 或 `di` 包。当前 `nhsd.basic` 已负责 `binding`、`mRealVM`、`mRepo`、`mBundle` 等注入。
- 不要新增 Hilt 相关代码。禁止生成 `@Inject`、`@HiltViewModel`、`@AndroidEntryPoint`、`@Module`、`@InstallIn`、`@Provides`、`@Binds`。
- 不要为了“标准 Clean Architecture”新增 `domain`、`UseCase`、`AppResult`、`AppError`。
- 不要默认把接口实体再包装成 Domain Model / UiModel。
- 不要修改 `ApiResponse.java`，除非用户明确要求做全局网络层改造。
- 不要新增多 Gradle Module，除非用户明确要求重构工程结构。
- Activity/Fragment 不直接请求网络或访问数据库。
- Adapter 不写业务请求。
- 业务代码不要直接散落第三方 SDK 调用。
- 禁止硬编码 token、secret、api key、私钥。
- 禁止在日志中输出 token、手机号、邮箱、用户 ID、订单号、支付参数、授权 code。

## 基础写法摘要

新增功能优先放在 `feature/<业务域>/` 下，和现有页面保持同一风格。

- Activity 直接继承 `com.nhsd.basic.BaseViewModelActivity<VB, VM>`。
- Fragment 直接继承 `com.nhsd.basic.BaseViewModelFragment<VB, VM>`。
- 普通 ViewModel 继承 `AppViewModel`。
- 需要仓库的 ViewModel 继承 `AppRepositoryViewModel<T>`。
- `AppViewModel.id()` 和 `AppRepositoryViewModel.id()` 已固定返回 `BR.layout`，不要重复实现。
- 页面中直接使用基类注入对象：`binding`、`mRealVM`、`mRepo`、`mBundle`。
- XML 默认使用 `<layout>` 根节点，页面 ViewModel 变量名为 `layout`，列表 item 变量名为 `item`。
- 页面初始化优先放在 Fragment 的 `onPrepareView()` 中，并拆成 `initView()`、`initListener()`、`initObservers()` 或现有相近命名。
- 点击事件优先使用 `setOnSingleClickListener` 或 XML 中 `layout.clickHandler::onClick`。
- ViewModel 不直接依赖 Activity、Fragment、View，不直接创建 `ApiService` 或 `AppDatabase`。
- 简单请求可通过 `mRepo.api.xxx(...)` 配合 `response(...)`、`request(...)` 或 `requestWithLoading(...)` 调用。
- 多接口组合、缓存、数据库、文件、MMKV、DataStore、复杂错误降级或多处复用时，新增 Repository 方法。
- Repository 返回类型优先保持 `ApiResponse<T>` 或项目已有业务模型。
- Retrofit 接口统一写在 `core/network/apis/ApiService.kt`，路径常量写在 `ApiConstants.kt`。
- 请求体写在 `core/network/apis/request/`，响应聚合模型写在 `core/network/apis/response/`，接口实体写在 `core/network/apis/entity/`。

## 场景摘要

- 列表：普通列表优先 `QuickAdapter<ItemXxxVM>` + `AppItemViewModel<T>`；分页走 Paging3 和 `epnova-android-paging-state-ui`。
- 弹窗：中间弹窗优先 `BaseVMDialogFragment<VB, VM>` / `BaseDialogFragment<VB>`；底部弹窗优先 `BaseBottomPushVMDialogFragment<VB, VM>` / `BaseBottomPushDialogFragment<VB>`。
- 异步：轮询、SSE、下载、生成任务等长任务必须保存 `Job` 或 handler，并在 `onCleared()` 取消。
- 导航：外部可配置跳转优先走 `AppRouter`；跨页面一对多刷新用 `FlowEventBus` + `Events`；明确目标页面时可直接使用目标 Activity 的 `start(...)` / `getIntent(...)`。
- Room：新增或修改 Entity 必须检查 `AppDatabase` version 和 Migration，禁止随意 `fallbackToDestructiveMigration`。
- 第三方 SDK：广告、支付、推送、统计、登录、归因等能力隔离在 `core/manager/` 或对应平台包下。
- 多语言：新增 string 只放入 `values`、`values-zh`、`values-th`、`values-in` 的 `strings.xml`。
- 命名：包名、类名、变量、函数、常量、View id、layout、drawable、string key 优先按 `epnova-android-naming`。

## Figma 与 UI 还原

- Figma/截图到 Android XML UI 还原优先使用 `design/skills/epnova-android-figma-xml-ui/SKILL.md`。
- 访问 Figma 设计稿时，优先使用可用 MCP 工具，或通过 curl 调用 Figma REST API；不要使用 WebFetch 读取 Figma URL。
- Android UI 实现应优先把 Figma 数值映射到本地 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector` 和 BindingAdapter。
- 识别到 Figma 标注 `Text1`、`Text2`、`Text3`、`Text4`、`Text5` 等文本层级时，优先映射为 `?attr/textColor1`、`?attr/textColor2`、`?attr/textColor3`、`?attr/textColor4`、`?attr/textColor5`。
- 涉及 TextView、Button、EditText、标签等文案控件时，宽高尽量不要写死；优先用 `wrap_content`、`0dp` 约束、`minWidth/minHeight`、`maxWidth`、`maxLines`、`ellipsize`、padding/margin。
- 字体尽量使用本地已有 `Text.Bold`、`Text.SemiBold`、`Text.Medium`、`Text.Regular`、`Text28/Text20/Text18/Text16/Text14` 等 style。
- 不把设计说明文案写进页面 UI，除非设计稿本身包含这些文案。

## 输出要求

代码生成或修改完成后，按任务规模说明：

- 新增/修改文件。
- 复用了哪些项目已有类、方法、BindingAdapter、style、drawable 或 skill。
- 是否新增 API、request、response、entity、dao、mapper。
- 是否新增 Repository、UiModel、UiState、UseCase，以及原因。
- 是否涉及 Room version / Migration。
- 是否涉及多语言 string。
- 是否涉及长任务，以及在哪里取消。
- 已运行的验证命令，或无法验证的原因。
- 需要人工确认的风险点。

代码审查时，问题优先，按严重程度输出：文件路径、问题、影响、建议。没有发现问题时明确说明，并列出剩余测试缺口或残余风险。

## 忽略规则

- 等同 Claude ignore：`**/OssManager.kt`。
- 除非用户明确要求，不要查看或编辑 `OssManager.kt`。
