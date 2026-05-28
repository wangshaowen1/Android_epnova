# Android Kotlin 代码生成总规范

本文档是完整规范索引页，不作为大多数任务的首读文件。标准任务优先读取 [android-codegen-quick-rules.md](android-codegen-quick-rules.md)；“检查一下 / 不走 Gradle / 局部确认”优先读取 [android-check-quick.md](android-check-quick.md)。

只有 quick rules 无法覆盖、任务跨模块、涉及高风险能力或用户要求完整规范时，才读取本文件和下列细则。

## 文档分层

1. 快速规则：[android-codegen-quick-rules.md](android-codegen-quick-rules.md)
2. 快速检查：[android-check-quick.md](android-check-quick.md)
3. Figma 快速规则：[android-figma-xml-quick.md](android-figma-xml-quick.md)
4. 项目基础：[android-codegen-project-rules.md](android-codegen-project-rules.md)
5. 架构/API/异步：[android-codegen-architecture-rules.md](android-codegen-architecture-rules.md)
6. XML/资源/命名：[android-codegen-xml-resource-rules.md](android-codegen-xml-resource-rules.md)
7. 场景路由与高风险补充：[android-codegen-scenarios.md](android-codegen-scenarios.md)
8. 完整 Checklist：[android-codegen-checklist.md](android-codegen-checklist.md)
9. 场景 skill：`design/skills/*/SKILL.md`

## 使用优先级

1. 用户本次明确要求。
2. 当前项目中同类功能的现有实现。
3. 项目根目录 `AGENTS.md` 的快速约束。
4. Quick rules 或 quick check。
5. 命中场景的最小必要 skill。
6. 本索引、场景路由与高风险补充和完整 Checklist。
7. Android 官方通用建议。

`design/` 文档用于减少重复搜索和提醒项目惯例，不替代读取当前代码。文档、旧通用模板或外部建议与当前代码不一致时，先以当前代码同类实现为准，并在输出中说明取舍；安全、敏感信息、用户明确禁令和项目明确禁止项仍然不能因为旧代码存在而扩大使用。

## 读取策略

- 快速局部修改：读目标代码和 `android-check-quick.md`，不读完整规范。
- 标准代码生成：读 `android-codegen-quick-rules.md`，再按本次实际修改范围读取命中场景的最小必要 skill。
- Figma/UI 生成：读目标 Figma 节点和 `android-figma-xml-quick.md`；需要完整细则时读 `skills/android-figma-xml-ui/SKILL.md`。
- 项目结构、目录、禁令不确定：读 `android-codegen-project-rules.md`。
- ViewModel、Repository、API、异步任务不确定：读 `android-codegen-architecture-rules.md`。
- XML token、drawable、命名不确定：读 `android-codegen-xml-resource-rules.md`。
- 完整审查、复杂风险输出或用户明确要求全量审查：读 `android-codegen-checklist.md`。

## 核心约束摘要

- 默认最小范围执行，先 `rg -n` 定位，再按需读取局部片段。
- 不要为小改动全量读取规范、全量扫描资源或全量分析 Figma。
- 不新增 Hilt、`AppContainer`、`ViewModelFactory`、`di` 包。
- 不为普通任务新增 Clean Architecture、UseCase、AppResult/AppError、完整 MVI。
- Activity/Fragment 不直接请求网络或访问数据库；Adapter 不写业务请求。
- XML 优先使用项目 `style/theme/attr/color/dimen/drawable`，只检查本次触达片段。
- 新增 drawable 先查等价资源；纯色/圆角/描边背景按值命名。
- 除非用户明确要求，不查看或编辑 `OssManager.kt`。
