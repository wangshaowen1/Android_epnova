# Design 文档索引

本目录用于存放本项目后续生成的索引、规范文档和设计相关说明。只有需要查询项目规范、设计资料或历史扫描信息时，从本目录查找；普通代码修改仍以项目根目录 `AGENTS.md`、目标代码和当前同类实现为先。

## 根目录规范文档

- `android-codegen-guidelines.md`：Android AI 代码生成总入口，说明文档分层、读取优先级、读取策略和核心约束摘要。
- `android-codegen-quick-rules.md`：高频 Android 代码生成快速规则，覆盖项目基线、查找方式、DataBinding 列表、XML 资源、Drawable、禁止项和最小验证。
- `android-check-quick.md`：局部检查快速规则，适用于“不走 Gradle”“检查一下”等场景，覆盖 Kotlin/逻辑、XML/资源、Figma/XML 的轻量检查路径。
- `android-codegen-project-rules.md`：项目基础规则，覆盖项目识别、技术基线、禁止项、目录规则和 Base 类使用方式。
- `android-codegen-architecture-rules.md`：架构与业务规则，覆盖 ViewModel、Repository、API 与模型、异步错误处理、业务注释、UseCase/MVI、安全与日志。
- `android-codegen-xml-resource-rules.md`：XML 与资源规则，覆盖 UI XML 写法、Figma 执行边界、Resource Token 映射、资源命名和 drawable 命名。
- `android-codegen-scenarios.md`：场景路由与高风险补充，只用于判断应读取哪个 skill，以及 Room、SDK/版本差异、多语言等高风险场景；不承载完整列表、弹窗、Figma、Paging、异步或命名细则。
- `android-codegen-checklist.md`：完整代码生成与复杂审查 Checklist，仅在整页生成、复杂改动、用户要求全量审查或发布前审查时使用。
- `android-figma-xml-quick.md`：Figma 到 Android XML 的快速规则，适用于拿到 Figma 链接、节点或截图后快速实现/走查 UI。
- `code-interface-directory.md`：代码接口目录维护说明，用于指导重新扫描当前分支。
- `code-interface-scan-summary.md`：当前项目代码接口扫描摘要，属于历史快照；用于快速了解项目结构，实际判断以当前分支搜索结果和文件内容为准。

## 版本设计资料

- `1.8.0/README.md`：快出片 V1.8.0 Android 原型整理，记录 Android 需要实现、展示或对接的页面、状态、弹窗、字幕、充值、埋点等需求。
- `1.8.0/screenshot-index.md`：V1.8.0 Android 原型截图索引，按做短剧 Tab、创作状态、作品详情横条、分镜成功弹窗、算力充值、我的/设置/帮助、字幕修正归档截图。
- `1.8.0/assets/`：V1.8.0 Android 原型截图资源，仅用于 UI 结构、布局和视觉参考。

## 项目 Skills

- `skills/android-feature-scaffold/`：页面骨架规范，用于新增或审查 Activity、Fragment、ViewModel、XML、Toolbar、参数传递和 start/newInstance 结构。
- `skills/android-api-request/`：接口请求规范，用于 Retrofit、ApiService、ApiConstants、请求体、响应模型、Repository、ViewModel 请求和错误处理。
- `skills/android-async-task/`：异步长任务规范，用于轮询、SSE、生成任务、下载任务、视频合成、Job 管理、重试和状态 UI。
- `skills/android-databinding-list/`：非分页 DataBinding 列表规范，用于 QuickAdapter、AppItemViewModel、item XML、多类型列表和点击事件转发。
- `skills/android-paging-state-ui/`：Paging3 分页与状态 UI 规范，用于 PagingSource、PagingDataAdapter、LoadState、空态/错态/加载态、刷新和重试。
- `skills/android-dialog/`：弹窗实现规范，用于中心弹窗、底部弹窗、BaseDialogFragment、BaseVMDialogFragment、参数传递、按钮事件和结果回调。
- `skills/android-navigation-events/`：导航与事件规范，用于 AppRouter、AppNavigator、FlowEventBus、Events、scheme/deep link、主 Tab 跳转和登录态变化。
- `skills/android-figma-xml-ui/`：Figma/截图到 XML UI 还原规范，用于按设计稿实现 Android 页面、组件、弹窗、卡片、列表和状态页。
- `skills/android-naming/`：命名规范，用于包名、类名、ViewModel、Repository、API 模型、资源文件、View id、常量、函数、layout、drawable、string、dimen 和 attr。

`skills/*/references/` 存放项目示例和搜索依据，`skills/*/agents/` 存放 agent 配置；通常由对应 skill 按需读取，不作为人工优先入口。

## AI 使用说明

- AI 调度入口在项目根目录 `AGENTS.md`，负责说明执行流程、文档读取策略和 skill 路由。
- 标准 Android 代码生成优先读取 `android-codegen-quick-rules.md`，再按本次实际修改范围读取命中场景的最小必要 skill；只有 quick rules 无法覆盖、任务跨模块、涉及 Room/SDK/多语言等高风险能力、场景路由不明确或用户要求完整规范时，才读取 `android-codegen-guidelines.md` 和 `android-codegen-scenarios.md`。
- “检查一下 / 不走 Gradle / 局部确认”优先读取 `android-check-quick.md`；只有发现问题、任务跨模块或用户要求完整审查时，才读取 `android-codegen-checklist.md`。
- 只有本次明确按 Figma/截图实现或校准可见 UI 时，才优先读取 `android-figma-xml-quick.md`；整页重写、复杂资源导出、目标节点信息不足或用户要求完整精确还原时，才读取 `skills/android-figma-xml-ui/`。普通 XML 小改、逻辑搬迁、修引用和静态检查走目标代码与 `android-check-quick.md`。
- 只有用户明确提到 V1.8.0、20260522、截图编号/归档页面，或任务明显来自该归档需求时，才读取 `1.8.0/README.md`。需要视觉核对时先读 `1.8.0/screenshot-index.md` 定位目标截图，只有目标截图确实相关时才打开具体图片。
- 项目内可复用 skill 位于 `design/skills/`，用于页面骨架、API 请求、命名、分页、普通列表、弹窗、导航事件、Figma XML UI、异步任务等高频场景。
- `design/` 文档用于索引项目惯例和降低重复搜索成本，不覆盖用户当前要求和当前代码中的同类真实实现；当文档与当前代码冲突时，先按当前代码同类实现处理，并保留安全和项目禁令。
- 规则文件不得写死复制来源项目的项目名、包名、scheme、语言目录或 SDK 名称；这些信息一律从当前分支读取。
