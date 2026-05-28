# Android 快速规则

本文件用于标准 Android 小改和常规生成任务的首读。它只负责路由、项目硬约束和最小验证；具体 API、分页、弹窗、Figma/XML、命名等细节按本次实际修改范围再读取对应 skill。

## 执行范围

- 快速局部修改：只读目标文件、直接相关代码和必要资源；不读完整规范，不读无关 skill。
- 标准代码生成：先读本文件，再按本次实际新增或修改的领域读取最小必要 skill。
- Figma/UI 还原：先读目标 Figma 节点和 `android-figma-xml-quick.md`；本文件只作为 Android 项目基线补充。
- 用户明确说“不走 Gradle”时，只做静态检查。

## 查找方式

- 先用 `rg -n` 定位符号、id、资源名、字符串 key 和调用点；不要先整文件读取。
- 只在命中行不足以判断时读取附近片段。
- 同类实现搜索先限定当前 feature、目标 layout、直接资源目录；找不到再扩大范围。
- 资源检查先按明确路径、候选资源名、颜色值、圆角值、描边值精确查询；不要默认扫描整个 `res`。
- 文档和当前代码不一致时，当前代码同类实现优先。

## 项目基线

- 主工程默认 `app`；包名、namespace、flavor、依赖以当前分支为准。
- Kotlin + MVVM + Repository，XML + DataBinding/ViewBinding，基于 `nhsd.basic`。
- 页面 XML 默认 `<layout>`，ViewModel 变量名 `layout`，item 变量名 `item`。
- Activity/Fragment 优先继承项目已有 base 类；ViewModel 优先继承 `AppViewModel` / `AppRepositoryViewModel`。
- 保存、提交、支付、生成、跳转、登录等用户高频动作优先使用 `setOnSingleClickListener` 或 XML `layout.clickHandler::onClick`；简单关闭、toggle、播放器点击、adapter retry、widget 内部回调跟随当前代码。
- Fragment 中手动注册的 ViewPager2 callback、AdapterDataObserver、键盘监听、事件监听等回调，先看当前类、基类和同类页面生命周期模式；没有明确反例时，沿用项目常见的 `onDestroy()` 清理。

## 场景路由

- API：只有新增/修改 `ApiConstants`、`ApiService`、request/response/entity、Repository 方法或 ViewModel 接口调用链路时，才读取 `skills/android-api-request/SKILL.md`。
- 异步长任务：只有涉及轮询、SSE、下载、视频合成、Job 生命周期、重复启动保护或取消逻辑时，才读取 `skills/android-async-task/SKILL.md`。
- 分页：只有涉及 Paging3、PagingSource、PagingDataAdapter、LoadState/footer 或分页刷新/重试时，才读取 `skills/android-paging-state-ui/SKILL.md`。
- 普通列表：只有新增或重构非分页 RecyclerView、Adapter、ItemVM、item XML 或列表点击转发时，才读取 `skills/android-databinding-list/SKILL.md`。
- 命名：只有命名冲突、不确定命名、批量新增资源或用户要求命名审查时，才读取 `skills/android-naming/SKILL.md`。
- 弹窗、导航、页面骨架按本次实际修改范围读取对应 skill；不要因为相关词出现在上下文里连锁读取。

## 硬约束

- 不新增 Hilt、`AppContainer`、`ViewModelFactory`、`di` 包。
- 不为普通任务新增 Clean Architecture、UseCase、AppResult/AppError、完整 MVI。
- Activity/Fragment 不直接请求网络或访问数据库；Adapter 不写业务请求。
- 不硬编码 secret/token/key，不输出敏感日志。
- 除非用户明确要求，不查看或编辑 `OssManager.kt`。
- XML 触达行能映射项目资源时优先使用 `@dimen`、`?attr`、`@color`、`@style`、`@drawable`；完整 token 规则见 `android-codegen-xml-resource-rules.md` 和 `android-figma-xml-quick.md`。
- 新增 drawable 前先查等价资源；纯色、透明色、圆角、描边背景按项目现有值命名规则处理。

## 最小验证

- Kotlin/逻辑小改：用 `rg` 确认新旧调用点、旧逻辑残留和关键引用。
- XML 小改：解析目标 XML，检查直接 `@drawable/@string/@color/@dimen/@style` 引用是否存在。
- Figma/XML 小改：只检查本次触达行的 token、字体、行高、字距、drawable 命名。
- 未运行 Gradle 时，在结果中说明“未跑 Gradle，按用户要求只做静态检查”。

## 输出

- 快速局部修改：
  - 改动：说明改了哪个文件和行为。
  - 静态检查：说明已确认的新旧调用点、直接资源引用、XML 解析或旧逻辑残留。
  - 风险/未验证：说明未跑 Gradle、视觉未实机确认、接口字段待确认等与本次相关的点。
- 标准代码生成：
  - 文件：列出新增/修改的关键文件。
  - 复用：说明复用了哪些项目已有 base 类、Repository、BindingAdapter、style、drawable 或 skill。
  - 验证：说明已做的静态检查；未跑 Gradle 时明确原因。
  - 需要确认：只列与本次任务直接相关的后端字段、设计资产、文案翻译、SDK 差异等。
- 代码审查：
  - 问题优先：按严重程度列文件、行号、问题、影响、建议。
  - 无问题：明确说明“未发现阻断问题”，并列剩余测试缺口或残余风险。
- 只在实际触及时说明 API、Repository、Room、长任务、多语言、SDK 差异等条目，不为了凑清单输出一串“未涉及”。
