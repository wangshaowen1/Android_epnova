# Android 快速规则

标准 Android 小改和常规生成任务的首读。**单一职责:查找方式 + 最小验证 + 路由指针。**
红线禁令见 `AGENTS.md`《关键禁令》;项目基线/目录/Base 类见 `android-codegen-project-rules.md`;完整场景路由见 `android-codegen-scenarios.md`。本文件不重复这些内容。

## 执行范围

- 快速局部修改:只读目标文件、直接相关代码和必要资源;不读完整规范,不读无关 skill。
- 标准代码生成:先读本文件,再按本次实际新增或修改的领域读取最小必要 skill。
- Figma/UI 还原:先读目标 Figma 节点和 `android-figma-xml-quick.md`。
- 用户明确说“不走 Gradle”时,只做静态检查(流程见 `android-check-quick.md`)。

## 查找方式

- 先用 `rg -n` 定位符号、id、资源名、字符串 key 和调用点;不要先整文件读取。
- 只在命中行不足以判断时读取附近片段。
- 同类实现搜索先限定当前 feature、目标 layout、直接资源目录;找不到再扩大范围。
- 资源检查先按明确路径、候选资源名、颜色值、圆角值、描边值精确查询;不要默认扫描整个 `res`。
- 文档和当前代码不一致时,当前代码同类实现优先。
- Windows PowerShell 读取含中文文件易乱码:用 `[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)`、`$OutputEncoding = [System.Text.UTF8Encoding]::new($false)` 和 `Get-Content -Encoding UTF8`;不要把终端乱码误判为文件损坏。

## 场景路由(命中才读,不连锁)

只按本次实际触达范围读取对应 skill,不因为相关词出现在上下文里就连锁读取。完整路由表与组合场景见 `android-codegen-scenarios.md`。最常用:

- 新页面骨架 → `skills/android-feature-scaffold`
- 接口链路新增/改 → `skills/android-api-request`
- 轮询/SSE/下载等长任务 → `skills/android-async-task`
- 非分页列表 → `skills/android-databinding-list`;分页 → `skills/android-paging-state-ui`
- 弹窗 → `skills/android-dialog`;导航/事件 → `skills/android-navigation-events`
- XML token/drawable 命名 → `android-codegen-xml-resource-rules.md`,命名仍不定再读 `skills/android-naming`
## 局部接口快路径

用户已给接口截图/字段表/明确路径,且只是给现有页面、列表或点击流程补一个局部接口时,走快路径,不升级到完整 API/分页 skill,也不等外部接口工具:

- 只读目标 `ApiConstants`、`ApiService`、request/entity、目标 VM/Repo/Fragment/Adapter/XML 和必要同类写法。
- 优先复用项目已有 `UserTokenBody`、`PagedBody`、`ApiResponse`、`PagedList`、`PagingSource`、`request/requestWithLoading` 链路;字段确实无法覆盖或语义需隔离时才新增专用 Body/模型。
- 错误处理与 `launch(block/onError/onComplete)` 细则见 `android-codegen-architecture-rules.md`《异步与错误处理》。

## 最小验证

- Kotlin/逻辑小改:用 `rg` 确认新旧调用点、旧逻辑残留和关键引用。
- XML 小改:解析目标 XML,检查直接 `@drawable/@string/@color/@dimen/@style` 引用是否存在。
- Figma/XML 小改:只检查本次触达行的 token、字体、行高、字距、drawable 命名。
- 未运行 Gradle 时,在结果中说明“未跑 Gradle,按用户要求只做静态检查”。

## 输出

按任务规模说明,只保留与本次相关的信息,不为凑清单输出一串“未涉及”。输出模板见 `android-check-quick.md`。

- 快速局部修改:改了什么、静态检查了什么、有无风险。
- 标准代码生成:新增/修改文件、复用了哪些 base 类/Repository/BindingAdapter/style/drawable/skill、验证结果、需要确认的点。
- 代码审查:按严重程度列文件、行号、问题、影响、建议;无问题则说明并列剩余风险。
