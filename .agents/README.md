# .agents 目录（Agent 工作流）

本目录是 Epnova Android 工程的 agent 规则细则、场景 skill、项目资料和记忆的存放处。**它只是导航索引,不承载调度逻辑。**

调度入口、读取优先级、执行档位和**红线禁令**统一在根目录 `AGENTS.md`(Claude Code 经 `CLAUDE.md` import 读同一份)。本文件只回答"哪个文件管什么",不重复规则正文。

> 想了解整套架构的设计、历史演进与改动理由,见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 规则细则(按职责单一,按需读取)

| 文件 | 单一职责 |
| --- | --- |
| `android-codegen-quick-rules.md` | 标准代码生成任务首读:查找方式、最小验证、场景路由指针 |
| `android-check-quick.md` | “检查一下 / 不走 Gradle / 局部确认”首读:静态检查流程 + 输出模板 |
| `android-figma-xml-quick.md` | Figma/截图到 XML 的首轮实现流程 |
| `android-codegen-project-rules.md` | 项目识别、技术基线、目录结构、Base 类使用 |
| `android-codegen-architecture-rules.md` | ViewModel、Repository、API 与模型、异步错误处理、业务注释 |
| `android-codegen-xml-resource-rules.md` | XML/UI 写法、Resource Token 映射、资源与 drawable 命名 |
| `android-codegen-scenarios.md` | 场景→skill 路由表、Room/SDK/多语言等高风险补充 |
| `android-codegen-checklist.md` | 完整审查 Checklist(仅整页生成/复杂改动/发布前用) |
| `code-interface-directory.md` | 代码接口目录的重新扫描说明 |
| `code-interface-scan-summary.md` | 历史扫描摘要；只作导航线索，使用前必须以当前代码复核 |

## 场景 Skills(`skills/<name>/SKILL.md`,命中场景才加载)

| skill | 用途 |
| --- | --- |
| `android-feature-scaffold` | 页面骨架:Activity/Fragment/ViewModel/XML/参数传递 |
| `android-api-request` | 接口请求:Retrofit、ApiService、请求体、响应模型、Repository |
| `android-async-task` | 异步长任务:轮询、SSE、下载、Job 管理、状态 UI |
| `android-databinding-list` | 非分页 DataBinding 列表:QuickAdapter、ItemVM、多类型 |
| `android-paging-state-ui` | Paging3 分页与空/错/载状态 UI |
| `android-dialog` | 中心/底部弹窗、参数传递、结果回调 |
| `android-navigation-events` | AppRouter、scheme/deep link、FlowEventBus、登录态 |
| `android-figma-xml-ui` | Figma/截图到 XML 的完整精确还原 |
| `android-naming` | 包名、类名、资源、id、常量、layout、drawable 命名 |

每个 skill 文件夹的构成:`SKILL.md`(主体,所有工具都读)、`references/`(项目示例,命中后按需读)、`agents/openai.yaml`(**仅 Codex/OpenAI 用的触发摘要;不承载生成细则;Claude Code 与 Android Studio 跳过,不读**)。如果 `agents/openai.yaml` 与 `SKILL.md` 不一致，以 `SKILL.md` 为准。

## 项目记忆(`memory/`)

- `memory/MEMORY.md` — 记忆索引(语义/情节/用户偏好,CoALA 分层)
- 详见 `memory/MEMORY.md`;Codex 与 Claude Code 共享。

## 项目资料(`project/`)

- `project/work-publish-*.md` — 作品发布流程、状态逻辑和当前代码分析。
- `project/play/` — Top Earners / Creator 播放链路分析与实施记录。
- `project/ads/` — 广告平台相关回滚和风险文档。
- `project/asset-management-api-refactor-analysis.md` — 资产管理接口改造分析。

只有任务明确命中相关业务域时才读取这些专项文档；普通代码修改仍以当前代码和同类实现为准。

## 维护原则

- 每个文件单一职责,规则只有一个家;红线禁令只在 `AGENTS.md`。
- 任何文档与当前分支代码不一致时,以当前代码同类实现为准。
- 不写死复制来源项目的项目名、包名、scheme、语言目录或 SDK 名称。
