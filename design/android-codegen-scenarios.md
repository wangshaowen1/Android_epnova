# Android 场景路由与高风险补充

本文档只做场景路由和高风险补充，不承载完整实现细则。标准任务先读 [android-codegen-quick-rules.md](android-codegen-quick-rules.md)，局部检查先读 [android-check-quick.md](android-check-quick.md)。只有 quick rules 无法判断场景、任务跨模块、涉及 Room/SDK/多语言等高风险能力，或用户明确要求完整场景审查时，才读取本文件。

文档、skill 或旧模板与当前代码同类实现不一致时，先以当前代码同类实现为准；安全、敏感信息、用户明确禁令和项目明确禁止项仍然不能因为旧代码存在而扩大使用。

## 读取原则

- 能通过目标文件、同类实现和 quick rules 完成的小改，不读取本文件。
- 命中单一场景时，只读取对应 skill，不连锁读取其他 skill。
- 场景之间有交叉时，先按本次实际修改范围选择最小必要 skill。
- Figma、列表、弹窗、分页、异步、命名等详细规则在对应 skill 中维护，不在本文件重复展开。
- 完整 Checklist 只用于整页生成、复杂改动、用户要求全量审查或发布前审查。

## 组合场景最小读取

组合任务按实际触达范围读取，不因为标题里出现相关词就连锁读取全部 skill。

| 组合场景 | 推荐读取顺序 | 边界 |
| --- | --- | --- |
| 新页面 + API | `android-codegen-quick-rules.md` -> `android-feature-scaffold` -> `android-api-request` | 只有接口链路实际新增/修改时读 API skill。 |
| 新页面 + 普通列表 | `android-codegen-quick-rules.md` -> `android-feature-scaffold` -> `android-databinding-list` | 列表非分页时不要读分页 skill。 |
| 新页面 + 分页 | `android-codegen-quick-rules.md` -> `android-feature-scaffold` -> `android-paging-state-ui` | 不为普通分页默认新增 `RemoteMediator`。 |
| API + 长任务 | `android-codegen-quick-rules.md` -> `android-api-request` -> 必要时 `android-async-task` | 一次性请求不触发异步长任务 skill；轮询、SSE、下载才触发。 |
| Figma 普通区域 | 目标 Figma 节点 -> `android-figma-xml-quick.md` | 信息足够时不读完整 Figma skill，不全量扫 `res`。 |
| Figma 弹窗 | 目标 Figma 节点 -> `android-figma-xml-quick.md` -> `android-dialog` | 只有完整精确还原、复杂资产或信息不足时再读 `android-figma-xml-ui`。 |
| 弹窗 + API | `android-codegen-quick-rules.md` -> `android-dialog` -> 必要时 `android-api-request` | 只改文案、Toast、简单字段时不读弹窗 skill。 |
| 分页 + item 操作 | `android-codegen-quick-rules.md` -> `android-paging-state-ui` | 复杂点击/多选/跨页刷新按分页 skill 内事件规则处理，不转普通列表 skill。 |
| 导航 + 全局刷新 | `android-codegen-quick-rules.md` -> `android-navigation-events` | 单个明确 Activity 跳转只查目标 `start/getIntent/newInstance`。 |
| 作品发布改造 | `work-publish-flow.md` -> `work-publish-logic.md` -> `work-publish-current-code-analysis.md` -> 按实际触达 skill | 旧发行/签约字段只能作代码现状参考，不作为新发布流程最终业务口径。 |

## 场景路由

### 页面骨架

新增 Activity/Fragment/ViewModel/XML 骨架、判断 Base 类、包路径、`start(...)` / `newInstance(...)` / `mBundle` 模式时，读取 [android-feature-scaffold](skills/android-feature-scaffold/SKILL.md)。

只改已有页面的局部逻辑、Toast、字段绑定、点击事件、状态同步或资源引用时，不读取页面骨架 skill。

### API 与一次性接口

新增或修改 `ApiConstants`、`ApiService`、request/response/entity、Repository 方法、ViewModel 请求链路或错误处理时，读取 [android-api-request](skills/android-api-request/SKILL.md)。

用户已经给出接口字段，且只是给现有页面补一次性接口调用时，优先按 quick rules 查目标 `ApiService`、request/entity、VM/Repo 和附近同类写法；不自动升级到完整 API skill。

### 异步长任务

涉及轮询、SSE、下载、视频合成、生成任务状态检查、Job 生命周期、重复启动保护、取消逻辑或长任务 UI 时，读取 [android-async-task](skills/android-async-task/SKILL.md)。

普通 loading/error 文案、一次性接口请求、简单按钮状态，不触发异步长任务 skill。

### 非分页列表

新增、重构或审查普通 RecyclerView/DataBinding 列表、Adapter、ItemVM、item layout、列表更新和点击转发时，读取 [android-databinding-list](skills/android-databinding-list/SKILL.md)。

只排查列表 bug、只改 item 文案/样式、只调 BottomSpace/Decoration/间距或刷新逻辑的一小段时，优先读目标代码和 quick check。

### 分页与状态 UI

涉及 Paging3、PagingSource、PagingDataAdapter、LoadState/footer、分页刷新/重试、分页 empty/error/loading、SmartRefreshLayout 分页链路或 Room 缓存分页时，读取 [android-paging-state-ui](skills/android-paging-state-ui/SKILL.md)。

普通非分页列表状态 UI 不触发分页 skill。`RemoteMediator` 只在明确需要 Room 数据库缓存、离线缓存、本地优先展示或跨会话列表持久化时使用。

### 弹窗

新增、重构或审查 Dialog/DialogFragment、底部弹窗、中心弹窗、确认弹窗、输入弹窗、选择弹窗、支付弹窗、下载弹窗等弹窗结构时，读取 [android-dialog](skills/android-dialog/SKILL.md)。

只给已有弹窗补一个 Toast、文案、点击绑定或简单字段时，优先读目标弹窗和同类实现。

### 导航与事件

新增或修改 AppRouter/AppNavigator、scheme/deep link、push/H5 jumpUri、登录拦截、FlowEventBus/Events、主 tab 跳转或跨页面刷新时，读取 [android-navigation-events](skills/android-navigation-events/SKILL.md)。

单个明确目标跳转、已有 `start(...)` / `getIntent(...)` / `newInstance(...)` 补参数或局部点击跳转，优先查目标页面入口和同类调用。

### Figma/XML UI

用户明确要求按 Figma/截图实现或校准可见 UI 时，先读 [android-figma-xml-quick.md](android-figma-xml-quick.md)。只有目标节点信息不足、整页重写、复杂资源导出或用户要求完整精确还原时，才读取 [android-figma-xml-ui](skills/android-figma-xml-ui/SKILL.md)。

逻辑搬迁、事件绑定、ViewModel 状态同步、修引用、静态检查，即使上下文中有 Figma/XML，也不启用完整 Figma UI 流程。

### XML 资源与命名

涉及 XML token、Figma 值映射、drawable 命名、资源命名冲突或批量新增资源时，读取 [android-codegen-xml-resource-rules.md](android-codegen-xml-resource-rules.md)；命名仍不确定、批量新增资源或用户要求命名审查时，再读取 [android-naming](skills/android-naming/SKILL.md)。

普通局部 XML 小改只检查本次触达行的资源引用、token、字体、行高、字距和 drawable 命名。

## 高风险补充

以下内容保留在本文件，是因为它们跨多个 skill，且错误代价高。

### Room

Room 是数据库场景，不是普通 API 模型场景。

- Room Entity 写在 `core/db/entity/`。
- Room Dao 写在 `core/db/dao/`，并在 `AppDatabase` 中注册。
- 数据库模型和接口/UI 模型有差异时，在 `core/db/mapper/` 或 feature 内新增 mapper。
- 新增或修改 Room Entity 时，必须检查 `AppDatabase` version。
- 修改表结构、字段、索引、主键时，必须同步处理 Migration 或 AutoMigration。
- 禁止随意使用 `fallbackToDestructiveMigration`。
- 除非用户明确允许清空本地数据，否则不能生成破坏性迁移。
- 如果无法判断当前数据库版本和历史 schema，必须在输出中标明需要人工确认。

### SDK 与版本差异

广告、支付、推送、统计、登录、归因、设备标识等能力必须隔离在 `core/manager/` 或当前分支已有平台包下。

- 业务页面不要直接散落第三方 SDK 调用。
- 业务代码不要直接 import 第三方 SDK 类，除非该文件就是平台实现层。
- Manager 类应对业务暴露稳定接口，不把具体 SDK 类型扩散到页面和 ViewModel。
- 涉及支付、登录、授权、广告奖励发放时，不能只依赖客户端状态作为最终结果。
- 不在日志中输出订单号、支付参数、授权 code、token 或用户敏感信息。
- 不要默认把海外版 SDK 套到大陆版，也不要把大陆版 SDK 套到海外版。
- 接入新 SDK 前先查 `build.gradle`、Manifest、`core/manager/`、已有平台包和合规初始化流程。
- 若同一能力需要兼容多个版本，优先新增接口或策略层，并把平台差异收敛在实现类中。

### 多语言资源

新增文案必须走 string 资源，并按当前分支实际语言策略同步。

- 新增前先检查重复 key 和相近文案。
- 新 key 插入到相关业务分组附近。
- key 使用语义名，不用页面位置名，例如 `feedback_submit_success` 优于 `text_button_1`。
- 不把设计说明文案写进页面 UI，除非设计稿本身包含这些文案。
- 多语言文案长度可能差异很大，涉及 TextView、Button、EditText、标签时避免写死宽高。
- 如果当前分支只有默认 `values/strings.xml`，不要因为旧项目规则而强行创建额外语言目录。
- 如果产品要求海外/多语言版本，按已有或指定 locale 目录补齐。
- 如果无法提供准确翻译，可以先保留业务指定的默认语言占位，但输出中必须标明需要人工确认。

### 敏感信息与日志

- 不要硬编码 token、secret、api key、私钥。
- 不要在日志中输出 token、手机号、邮箱、用户 ID、订单号、支付参数、授权 code。
- 不查看或修改 `OssManager.kt`，除非用户明确要求。

## 不要在本文件扩展的内容

- 不重复维护 Figma token、spacing、textColor、font、lineHeight、drawable 命名细节；这些放在 Figma/XML quick 和 Figma/XML skill。
- 不重复维护列表 Adapter、ItemVM、PagingSource、LoadState、Dialog 基类、导航事件、异步 Job 的完整实现；这些放在对应 skill。
- 不把本文件当作默认必读 Checklist；完整检查使用 [android-codegen-checklist.md](android-codegen-checklist.md)。
