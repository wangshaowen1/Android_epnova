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
| `android-databinding-ui` | 页面稳定状态、互斥区域与 ViewModel 驱动的 XML DataBinding |
| `android-paging-state-ui` | Paging3 分页与空/错/载状态 UI |
| `android-tab-viewpager` | 多 Tab + ViewPager2 Fragment 容器、同步与生命周期 |
| `android-dialog` | 中心/底部弹窗、参数传递、结果回调 |
| `android-navigation-events` | AppRouter、scheme/deep link、FlowEventBus、登录态 |
| `android-figma-xml-ui` | Figma/截图到 XML 的完整精确还原 |
| `write-project-docs` | 项目导览、架构/功能文档、文档审计与收敛 |

每个 skill 文件夹的构成:`SKILL.md`(主体,所有工具都读)、`references/`(项目搜索索引/模板,命中且正文指向时按需读)、`agents/openai.yaml`(**仅 Codex/OpenAI 用的 agent 配置;Claude Code 与 Android Studio 跳过,不读**)。

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
- skill 只收录 AI 容易重复犯错的项目约束、可直接定位的真实实现索引，或出错代价高且仅靠通用知识无法判断的决策点。
- 通用语言规范、行业既定事实和“跟随最佳实践”之类无项目判别力的内容不做 skill；能由当前代码同类实现直接得出的写法也不重复展开。
- 触发边界写在 `SKILL.md` 的 `description`；正文只保留执行时会改变实现选择的规则，详细项目索引按需放入 `references/`。
- 每个 `references/` 文件必须由同目录 `SKILL.md` 直接链接并注明读取条件，避免二级跳转和默认全量读取。
- 修改规则或 Skill 后按系统运行结构校验：Windows 使用 `powershell -ExecutionPolicy Bypass -File .agents/scripts/validate-agent-docs.ps1`，Linux/macOS 使用 `sh .agents/scripts/validate-agent-docs.sh`。两个系统脚本检查相同的 `SKILL.md` BOM/frontmatter、元数据、reference 直链、注册表与本地 Markdown 链接；调整校验项时必须同步更新。
- 任何文档与当前分支代码不一致时,以当前代码同类实现为准。
- 不写死复制来源项目的项目名、包名、scheme、语言目录或 SDK 名称。
