# Codex 项目记忆

本文件只做调度入口，不承载完整 Android/Figma 细则。完整规则按需读取：

- 总规范：`design/android-codegen-guidelines.md`
- 快速规则：`design/android-codegen-quick-rules.md`
- 快速检查：`design/android-check-quick.md`
- 场景路由：`design/android-codegen-scenarios.md`
- 检查清单：`design/android-codegen-checklist.md`
- Figma 快速规则：`design/android-figma-xml-quick.md`
- 场景 skill：`design/skills/*/SKILL.md`

优先级：用户本次明确要求 > 当前代码同类实现 > `AGENTS.md` 快速约束 > `design/*` 规则与 skill 细则 > 通用建议。
当 `AGENTS.md`、`design/*` 或外部建议与当前代码不一致时，先以当前代码同类实现为准，并在输出中说明取舍。

## 回复与执行

- 始终使用中文，表达简洁直接。
- 修改代码前先读现有实现，不凭通用模板硬套。
- 默认最小改动，不做无关重构，不回退用户已有改动。
- 不确定的风险点直接标明，不用猜测填平。

## 执行档位

先判断任务属于哪一档，能用轻量路径完成时不要升级：

- 快速局部修改：逻辑搬迁、事件绑定、ViewModel 状态同步、修引用、静态检查、小范围 XML 调整。只读目标文件、直接相关代码和必要资源，不读完整 design 文档，不读 skill。
- 标准代码生成：新增页面、接口、列表、弹窗、异步任务等中等范围任务。先读 `design/android-codegen-quick-rules.md`，再按需读命中场景的必要 skill，不读无关 skill。
- Figma/UI 生成或还原：只有用户明确要求按 Figma/截图实现或校准可见 UI 时启用。只读取目标 Figma 节点、目标 XML、同类 XML 和必要资源索引，不读取整个 Figma 文件，不全量扫描 `res`。
- 代码审查：按 review 姿态输出问题优先；只读取和审查目标相关的规则。
- 文档维护：保持“AGENTS 调度入口、design 总规范、场景路由与高风险补充、Checklist、skills 细则”的分层。

## 快速执行约束

- 默认先用 `rg -n` 定位符号、id、资源名和调用点；命中行不足以判断时，再读取附近片段。
- Windows PowerShell 读取含中文文件时，先设置 UTF-8 输出或显式指定编码，避免控制台编码导致中文乱码；优先使用 `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)`、`$OutputEncoding = [System.Text.UTF8Encoding]::new($false)` 和 `Get-Content -Encoding UTF8`。不要把终端乱码误判为文件格式损坏。
- 用户明确要求“加注释”“说明原因”或“写清楚边界”时，注释是验收项；涉及业务口径、特殊兼容、局部开关、反直觉实现、非通用算法时，必须在代码旁说明为什么这么做和作用边界，不要只靠方法名表达。
- 不要为了确认小改动而 `Get-Content` 整个大文件；除非文件很短、结构未知或正在做整页重写。
- 同类实现搜索优先限定到当前 feature、目标 layout 和直接资源目录；只有找不到模式或发现冲突时才扩大到 `app/src/main`。
- 资源检查优先用明确路径或精确资源名查询；不要默认递归扫描整个 `res`。
- 用户说“这里”“截图这里”“这个区域”时，只改当前截图/当前入口对应实现，不同步改同名旧入口、其它页面或历史布局，除非用户明确要求。
- 小范围 UI 替换优先复用项目现有写法；简单 RecyclerView 列表默认使用 `QuickAdapter + AppItemViewModel + item layout`，不要手写 `ListAdapter`、`DiffUtil`、`ItemDecoration` 或额外宽度计算，除非当前代码已有同类模式或用户明确要求。
- 不为“一行展示 N 个”擅自增加固定等分宽度、强制宽度计算或布局算法；除非设计明确要求等分，否则 item 宽度按内容、现有 style/dimen 和原设计样式自适应。
- 默认不运行 Gradle、不申请 Gradle 权限、不用 Gradle 做验证；构建由用户自行执行。验证只做静态检查，并说明静态检查范围。
- 小改如果需要新增超过 1 个辅助类、修改超过 3 个文件，先停下来复核是否范围扩大；没有用户确认不要继续扩大。
- 局部接口接入快路径：用户已给接口截图或字段说明，且只是给现有页面/列表/点击流程补接口时，不等待外部接口工具，不升级到完整 API/分页/UI 规范；只读目标 `ApiConstants`、`ApiService`、request/entity、目标 VM/Repo/Fragment/Adapter/XML 和必要同类写法。
- 局部接口接入优先复用项目已有 `UserTokenBody`、`PagedBody`、`ApiResponse`、`PagedList`、`PagingSource`、`request/requestWithLoading` 链路；只有字段确实无法覆盖或接口语义需要隔离时才新增专用 Body/模型。
- ViewModel 简单接口请求可沿用附近 `request(...)`、`requestWithLoading(...)` 或 `launch { response(...) }`；涉及状态回滚、`savingIds`、loading/progress 清理或自定义异常提示时，使用 `launch(block = ..., onError = ..., onComplete = ...)` + `response(...)`。不要在 `launch` 内手写 `try/catch/finally` 包网络请求，除非有明确特殊原因。
- 小改检查先完成最小闭环：目标代码是否接上、XML 是否可解析、直接资源是否存在、旧逻辑是否残留。可选视觉细节放到结论里的“额外发现”，不要拖慢主检查。
- 用户明确说“不走 Gradle”“直接检查”时，只做静态检查并说明范围。
- “检查一下”类任务优先按 `design/android-check-quick.md` 执行，只有发现问题或用户要求完整审查时才读完整 Checklist。

## 项目基线

- 主工程默认 `app`；实际 namespace、包名、flavor、依赖和 Manifest 以当前分支文件为准。
- 技术栈：Kotlin、MVVM + Repository、XML + DataBinding/ViewBinding、基于 `nhsd.basic`。
- 页面 ViewModel 变量名默认 `layout`，列表 item 变量名默认 `item`。
- Activity/Fragment 优先继承项目已有 `BaseViewModelActivity` / `BaseViewModelFragment`；ViewModel 优先继承项目已有 `AppViewModel` / `AppRepositoryViewModel`。
- 保存、提交、支付、生成、跳转、登录等用户高频动作优先沿用 `setOnSingleClickListener` 或 XML 中 `layout.clickHandler::onClick`；简单关闭、toggle、播放器点击、adapter retry、widget 内部回调可跟随当前代码使用 `setOnClickListener`。
- 新增文案走当前仓库实际维护的 `values*/strings.xml`；不要无需求新增语言目录。

## 关键禁令

- 不要新增 Hilt、`AppContainer`、`ViewModelFactory`、`di` 包。
- 不要为了“标准 Clean Architecture”新增 `domain`、`UseCase`、`AppResult`、`AppError` 或完整 MVI。
- 不要把本项目当成纯 ViewBinding 小项目处理；页面 XML 普遍使用 `<layout>` 和 BindingAdapter。
- Activity/Fragment 不直接请求网络或访问数据库；Adapter 不写业务请求。
- 不要硬编码 token、secret、api key、私钥，不要在日志输出敏感信息。
- 除非用户明确要求，不要查看或编辑 `OssManager.kt`。

## Figma/XML 快路径

- 有 Figma `node-id` 时只读取该节点；只有节点缺上下文、组件/变量无法判断或用户明确要求整页分析时才扩大范围。
- Figma 数值优先映射项目 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector` 和 BindingAdapter；首轮规则见 `design/android-figma-xml-quick.md`，完整细则见 `design/skills/android-figma-xml-ui/SKILL.md`。
- 逻辑搬迁、事件绑定、ViewModel 状态同步、修引用、静态检查，即使上下文里有 Figma/XML，也按快速局部修改处理，不启用完整 Figma 流程。

## 输出

- 快速局部修改：只说明改了什么、检查了什么、是否有风险。
- 标准代码生成：说明新增/修改文件、复用内容、验证结果和需要确认的点。
- 只有实际触及时，才说明 API、Repository、UiModel、Room、长任务、多语言、SDK 差异等条目。
- 不要为了满足清单而输出一串“未涉及”；只保留和本次任务有关的信息。
