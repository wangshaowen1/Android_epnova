# 项目规则与调度入口（AGENTS）

Gradle 规则：默认不运行 Gradle，不申请 Gradle/网络权限做验证；只做静态检查并说明检查范围，除非用户明确要求运行 Gradle。

本文件只承载**红线、档位与路由**，不展开细则。完整规则按需读取：

- 快速规则（标准任务首读）：`.agents/android-codegen-quick-rules.md`
- 快速检查（局部确认首读）：`.agents/android-check-quick.md`
- Figma 快速规则：`.agents/android-figma-xml-quick.md`
- 项目基础/目录/Base 类：`.agents/android-codegen-project-rules.md`
- 架构/API/异步：`.agents/android-codegen-architecture-rules.md`
- XML/资源/命名：`.agents/android-codegen-xml-resource-rules.md`
- 场景路由与高风险：`.agents/android-codegen-scenarios.md`
- 完整 Checklist：`.agents/android-codegen-checklist.md`
- 场景 skill：`.agents/skills/*/SKILL.md`（每个 skill 只读 `SKILL.md` 与按需 `references/`；`agents/openai.yaml` 仅 Codex 用，Claude/Android Studio 跳过不读）
- 项目记忆：`.agents/memory/MEMORY.md`（语义/情节记忆索引，Codex 与 Claude Code 共享）。
  会话结束前，若本轮产生值得留存的判断或决策，主动写入对应记忆文件：
  语义记忆（稳定约定）更新 `.agents/memory/semantic/*.md`；
  情节记忆（决策/踩坑）追加至 `.agents/memory/episodic/decisions.md`；
  已在仓库文档中记录的内容不重复存。

优先级：用户本次明确要求 > 当前代码同类实现 > `AGENTS.md` 红线与档位 > `.agents/*` 规则与 skill 细则 > 通用建议。
当 `AGENTS.md`、`.agents/*` 或外部建议与当前代码不一致时，先以当前代码同类实现为准，并在输出中说明取舍。

## 回复与执行

- 始终使用中文，表达简洁直接。
- 修改代码前先读现有实现，先用 `rg -n` 定位再读片段，不凭通用模板硬套。
- 在 Windows PowerShell 读取中文文件前，先设置 UTF-8 输出，并使用 `Get-Content -Encoding UTF8`；若终端出现乱码，先排查编码，不把乱码当成文件内容或文件损坏。
- 默认最小改动：只改用户本次指向的地方，不顺手改同名旧入口、其它页面或历史布局；不做无关重构，不回退用户已有改动。
- 对“快速局部修改”，若预计新增超过 1 个辅助类或改动超过 3 个文件，先停下来确认是否扩大范围。
- 但用户明确要求实现一个完整业务闭环时，例如“接口 + ViewModel + 入口接入 + 弹窗/XML + 请求参数传递”，可继续执行；执行前用一句话说明预计触达范围，仍保持最小改动。
- 不确定的风险点直接标明，不用猜测填平。
- 当前仓库是 Epnova Android 工程，主包、namespace、applicationId 当前均为 `com.kuaigeglobal.epnova`；遇到复制来源项目的旧项目名、旧包名或旧业务词时，必须回到当前分支搜索确认。

## 执行档位

先判断任务属于哪一档，能用轻量路径完成时不要升级：

- 快速局部修改：逻辑搬迁、事件绑定、ViewModel 状态同步、修引用、静态检查、小范围 XML 调整。只读目标文件、直接相关代码和必要资源，不读完整 `.agents` 规则文档，不读 skill。即使上下文里有 Figma/XML，也按本档处理，不启用完整 Figma 流程。
- 标准代码生成：新增页面、接口、列表、弹窗、异步任务等中等范围任务。先读 `.agents/android-codegen-quick-rules.md`，再按需读命中场景的必要 skill，不读无关 skill。
- Figma/UI 生成或还原：仅当用户明确要求按 Figma/截图实现或校准可见 UI 时启用。先读目标 Figma 节点和 `.agents/android-figma-xml-quick.md`；只读目标节点与同类 XML，不读整个 Figma 文件，不全量扫 `res`。
- 代码审查：按 review 姿态输出问题优先；只读取和审查目标相关的规则。
- 文档维护：保持“AGENTS 调度入口、`.agents` 规则细则、场景路由、Checklist、skills”的分层，每个文件单一职责。

## 项目基线

- 主工程为 `app`；`config.gradle` / `app/build.gradle` / `AndroidManifest.xml` 中当前包名为 `com.kuaigeglobal.epnova`，应用名为 `EpNova`。
- Manifest 当前 scheme 为 `epnovascheme`，OneLink host 为 `epnova.onelink.me`；路由改动前回查 `AndroidManifest.xml`、`AppRouter` 和同类入口。
- 技术栈：Kotlin、MVVM + Repository、XML + DataBinding/ViewBinding，基于 `nhsd.basic`；当前分支已有 Room、Paging3、SSE 与支付/登录/推送/统计接入点。
- 页面 ViewModel 变量名默认 `layout`，列表 item 变量名默认 `item`。
- Activity/Fragment 优先继承项目已有 `BaseViewModelActivity` / `BaseViewModelFragment`；ViewModel 优先继承 `AppViewModel` / `AppRepositoryViewModel`。
- 新增文案走当前仓库实际维护的 `values*/strings.xml`；不无需求新增语言目录。

## 关键禁令（红线）

> 全项目红线禁令的**唯一来源**。其他文档不重复列举，只在解释场景时引用本节。本节总在上下文中（Codex 读 AGENTS.md、Claude Code 经 CLAUDE.md import）。

- 不要把来源项目的旧包名、旧业务路径、旧 scheme 或历史示例当作当前项目事实；只有当前分支真实存在时才能引用。
- 不新增 Hilt、`AppContainer`、`ViewModelFactory`、`di` 包（`nhsd.basic` 已负责 `binding`、`mRealVM`、`mRepo`、`mBundle` 注入）。
- 不为“标准 Clean Architecture”新增 `domain`、`UseCase`、`AppResult`、`AppError` 或完整 MVI；默认不把接口实体包装成 Domain/UiModel。
- 不把本项目当成纯 ViewBinding 小项目；页面 XML 普遍使用 `<layout>` 和 BindingAdapter。
- 不新增多 Gradle Module，不修改 `ApiResponse.java`，除非用户明确要求工程结构或全局网络层重构。
- Activity/Fragment 不直接请求网络或访问数据库；Adapter 不写业务请求；第三方 SDK 调用必须隔离在 `core/manager/` 或平台包，不散落到业务页面。
- 接口实体不加 `isSelected`、`isExpanded`、`isEditing`、`localProgress`、`displayText` 等 UI 状态字段；列表本地状态用 `ItemXxxVM` 或新增 `XxxUiModel`。
- 不硬编码 token、secret、api key、私钥，不在日志输出 token、手机号、邮箱、用户 ID、订单号、支付参数、授权 code，不打印完整 request/response 到线上日志。
- 数据库不使用 `fallbackToDestructiveMigration`，不生成破坏性迁移，除非用户明确允许清空本地数据。
- 除非用户明确要求，不查看或编辑 `OssManager.kt`。

## 输出

- 快速局部修改：只说明改了什么、检查了什么、是否有风险。
- 标准代码生成：说明新增/修改文件、复用内容、验证结果和需要确认的点。
- 只在实际触及时说明 API、Repository、UiModel、Room、长任务、多语言、SDK 差异等；不为凑清单输出一串“未涉及”。
