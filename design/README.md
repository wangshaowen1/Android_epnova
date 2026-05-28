# Design 文档索引

本目录用于存放本项目后续生成的索引、规范文档和设计相关说明。只有需要查询项目规范、设计资料或历史扫描信息时，从本目录查找；普通代码修改仍以项目根目录 `AGENTS.md`、目标代码和当前同类实现为先。

## 阅读优先级

1. 用户本次明确要求。
2. 当前代码同类实现。
3. 根目录 `AGENTS.md`。
4. 本目录的 quick rules / quick check / Figma quick。
5. 命中场景的最小必要 `skills/android-*`。
6. 完整总规范、场景路由、高风险补充和 Checklist。

文档与当前代码不一致时，先以当前代码同类实现为准；安全、敏感信息、项目禁令和用户明确要求不因历史代码存在而放宽。

## 任务档位

- 快速局部修改：只读目标文件、直接相关代码和必要资源；需要检查时优先读 `android-check-quick.md`，不默认读取完整规范或 skill。
- 标准代码生成：先读 `android-codegen-quick-rules.md`，再按实际触达范围读取命中的 `skills/android-*`。
- Figma/UI 生成或还原：先读目标 Figma 节点或截图信息，再读 `android-figma-xml-quick.md`；只有目标信息不足、整页重写、复杂资源导出或用户要求完整精确还原时，才读 `skills/android-figma-xml-ui/SKILL.md`。
- 完整审查或发布前检查：按实际范围裁剪读取 `android-codegen-checklist.md`，不要为小改动执行全量 Checklist。

## 常用任务路由表

| 任务 | 首读 | 必要 skill / 文档 | 不要默认读取 |
| --- | --- | --- | --- |
| 局部 Kotlin 逻辑修复 | 目标代码、同类实现、`android-check-quick.md` | 无 | 完整总规范、无关 skill |
| 局部 XML 小改 | 目标 XML、直接资源、`android-check-quick.md` | 无 | 完整 Figma skill、全量 `res` |
| 新页面 | `android-codegen-quick-rules.md` | `skills/android-feature-scaffold/SKILL.md` | API、列表、分页 skill，除非实际触达 |
| 新页面 + API | `android-codegen-quick-rules.md` | `android-feature-scaffold`、`android-api-request` | 完整 Checklist，除非用户要求 |
| 新页面 + 普通列表 | `android-codegen-quick-rules.md` | `android-feature-scaffold`、`android-databinding-list` | `android-paging-state-ui` |
| 新页面 + 分页 | `android-codegen-quick-rules.md` | `android-feature-scaffold`、`android-paging-state-ui` | 普通列表 skill |
| 局部一次性接口接入 | `android-codegen-quick-rules.md`、目标 API/VM/Repo | 必要时 `android-api-request` | 外部接口工具、分页 skill |
| 轮询 / SSE / 下载 | `android-codegen-quick-rules.md` | `android-async-task`，触达接口时再读 `android-api-request` | 把长任务当普通 API |
| Figma 页面/组件 | 目标 Figma 节点、`android-figma-xml-quick.md` | 必要时 `android-figma-xml-ui` | 整个 Figma 文件、全量 `res` |
| Figma 弹窗 | 目标 Figma 节点、`android-figma-xml-quick.md` | `android-dialog`，必要时 `android-figma-xml-ui` | 新页面骨架 skill |
| 命名审查 / 批量资源 | `android-codegen-quick-rules.md` | `android-naming`、必要时 XML 资源规则 | 为单个清晰命名读取完整命名 skill |
| 作品发布改造 | `project/work-publish-*.md`、当前代码 | 按实际触达 API / dialog / navigation / list skill | 旧发行逻辑作为最终依据 |

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

## 可执行脚本

- `scripts/check_android_xml_tokens.ps1`：静态检查 Android XML 的裸色值、常见 spacing/radius token、字体兜底、局部字距/行高和简单 drawable 命名；小改优先用 `-ChangedLinesOnly`，避免历史 XML 问题干扰。
- `scripts/scan_code_interface.ps1`：按当前分支扫描主工程源码、API、Repository、feature 目录和语言资源；默认输出到终端，显式传 `-WriteSummary` 才写摘要文件，可用 `-ComparePath` 对比历史快照。
- `scripts/sync_design_docs.ps1`：在两个项目的 `design` 目录之间同步通用规范和脚本；默认排除需求文档目录 `project/**`，也不覆盖项目专属 `code-interface-scan-summary.md`。

脚本只作为辅助检查工具。发现问题后仍需结合当前代码同类实现、设计稿要求和对应 skill 判断是否修改。

## 专项业务文档

- `project/work-publish-flow.md`：作品发布流程说明，明确首次发布走 H5、非首次发布走 Android 原生，以及 Android 与 H5 的职责边界。
- `project/work-publish-logic.md`：作品发布状态、可发布集数计算、边界条件、弹窗和 Toast 文案。
- `project/work-publish-current-code-analysis.md`：作品发布相关当前代码现状，说明旧发行/签约逻辑与新发布流程的差距和建议改造顺序。

只有任务明确涉及作品发布、发布状态、发布入口、发布弹窗、作品/剧集发布角标或相关接口时，才读取这些专项文档。

## 版本设计资料

- `project/1.8.0/README.md`：快出片 V1.8.0 Android 原型整理，记录 Android 需要实现、展示或对接的页面、状态、弹窗、字幕、充值、埋点等需求。
- `project/1.8.0/screenshot-index.md`：V1.8.0 Android 原型截图索引，按做短剧 Tab、创作状态、作品详情横条、分镜成功弹窗、算力充值、我的/设置/帮助、字幕修正归档截图。
- `project/1.8.0/assets/`：V1.8.0 Android 原型截图资源，仅用于 UI 结构、布局和视觉参考。

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
- 当前项目 skill 路由统一使用 `android-*` 命名，例如 `android-api-request`、`android-dialog`、`android-figma-xml-ui`；旧的 `epnova-android-*` 前缀不再作为当前文档入口。
- 标准 Android 代码生成优先读取 `android-codegen-quick-rules.md`，再按本次实际修改范围读取命中场景的最小必要 skill；只有 quick rules 无法覆盖、任务跨模块、涉及 Room/SDK/多语言等高风险能力、场景路由不明确或用户要求完整规范时，才读取 `android-codegen-guidelines.md` 和 `android-codegen-scenarios.md`。
- “检查一下 / 不走 Gradle / 局部确认”优先读取 `android-check-quick.md`；只有发现问题、任务跨模块或用户要求完整审查时，才读取 `android-codegen-checklist.md`。
- 只有本次明确按 Figma/截图实现或校准可见 UI 时，才优先读取 `android-figma-xml-quick.md`；整页重写、复杂资源导出、目标节点信息不足或用户要求完整精确还原时，才读取 `skills/android-figma-xml-ui/`。普通 XML 小改、逻辑搬迁、修引用和静态检查走目标代码与 `android-check-quick.md`。
- 只有用户明确提到 V1.8.0、20260522、截图编号/归档页面，或任务明显来自该归档需求时，才读取 `project/1.8.0/README.md`。需要视觉核对时先读 `project/1.8.0/screenshot-index.md` 定位目标截图，只有目标截图确实相关时才打开具体图片。
- 只有任务明确涉及作品发布流程或发布状态时，才读取 `project/work-publish-*.md` 专项文档。
- 项目内可复用 skill 位于 `design/skills/`，用于页面骨架、API 请求、命名、分页、普通列表、弹窗、导航事件、Figma XML UI、异步任务等高频场景。
- `design/` 文档用于索引项目惯例和降低重复搜索成本，不覆盖用户当前要求和当前代码中的同类真实实现；当文档与当前代码冲突时，先按当前代码同类实现处理，并保留安全和项目禁令。
- 规则文件不得写死复制来源项目的项目名、包名、scheme、语言目录或 SDK 名称；这些信息一律从当前分支读取。
- 默认不运行 Gradle、不申请 Gradle 权限；除非用户明确要求或任务必须构建验证，否则只做静态检查并说明检查范围。
