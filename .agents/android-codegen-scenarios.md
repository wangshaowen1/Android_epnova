# Android 场景路由与高风险补充

标准任务先读 [快速规则](android-codegen-quick-rules.md)，局部检查先读 [快速检查](android-check-quick.md)。只有 quick rules 无法判断、任务跨场景，或涉及 Room、SDK、多语言等高风险能力时读取本文件。

## 读取原则

- 单一场景只读对应 skill；标题或上下文出现相关词不代表本次实际触达。
- 已明确目标文件、入口和局部改法时，直接读目标代码，不为“可能相关”连锁加载 skill。
- 组合任务按实际新增/修改的层选择最小集合；完整 Checklist 只用于整页、复杂改动或用户要求全量审查。

## 组合场景

| 组合 | 最小读取 | 边界 |
| --- | --- | --- |
| 新页面 + API | quick → `android-feature-scaffold` → 必要时 `android-api-request` | 已有接口只补单点走局部快路径。 |
| 新页面 + 普通列表 | quick → `android-feature-scaffold` → `android-databinding-list` | 非分页不读 Paging。 |
| 页面 + 多状态区域 | quick → `android-databinding-ui` | 手势、动画、播放器等即时过程保留命令式。 |
| 新页面 + 分页 | quick → `android-feature-scaffold` → `android-paging-state-ui` | 无离线/持久缓存需求不加 RemoteMediator。 |
| 多 Tab + 非分页子页 | quick → `android-tab-viewpager` | 只有新增完整页面骨架才加 scaffold。 |
| 多 Tab + 独立分页 | quick → `android-tab-viewpager` → `android-paging-state-ui` | 容器管 Fragment 稳定性；每个子页独立分页。 |
| API + 长任务 | quick → 必要时 `android-api-request` → `android-async-task` | 一次性请求不触发长任务。 |
| Figma 普通区域 | 目标节点 → `android-figma-xml-quick.md` | 信息足够时不读完整 Figma skill。 |
| Figma 弹窗 | 目标节点 → Figma quick → `android-dialog` | 复杂资产/整页精确还原才加完整 Figma skill。 |
| 弹窗 + API | quick → `android-dialog` → 必要时 API skill | 文案、字段、局部绑定不触发完整 Dialog。 |
| 分页 + item 操作 | quick → `android-paging-state-ui` | 不再加载普通列表 skill。 |
| 导航 + 全局刷新 | quick → `android-navigation-events` | 单个明确 start/newInstance 只查目标入口。 |

## 单场景路由

| 实际修改范围 | 读取 | 轻量路径 |
| --- | --- | --- |
| 新建/重构页面骨架、Base 类、start/newInstance/mBundle | [页面骨架](skills/android-feature-scaffold/SKILL.md) | 已有页面局部逻辑、绑定、点击不读。 |
| 新接口、跨 API/model/Repo/VM 至少两层、错误 helper 语义 | [API](skills/android-api-request/SKILL.md) | 完整链路只补字段/参数/调用点走 quick。 |
| 轮询、SSE、下载、任务状态与取消 | [异步](skills/android-async-task/SKILL.md) | 一次性请求或普通 loading 不读。 |
| 新增/重构非分页 RecyclerView + DataBinding 链路 | [普通列表](skills/android-databinding-list/SKILL.md) | 文案、样式、单点刷新只读目标代码。 |
| 稳定状态绑定、互斥区域、Observer 批量写 View | [页面 DataBinding](skills/android-databinding-ui/SKILL.md) | 即时手势/动画/播放器/单次赋值不读。 |
| PagingSource、PagingDataAdapter、LoadState、分页缓存 | [分页](skills/android-paging-state-ui/SKILL.md) | 普通列表不读；RemoteMediator 只在真实缓存需求下启用。 |
| Tab + ViewPager2 Fragment 容器和同步/生命周期 | [Tab/ViewPager2](skills/android-tab-viewpager/SKILL.md) | Banner、文案样式、单次 currentItem 不读。 |
| Dialog 基类、参数、结果、生命周期、IME、关闭 | [弹窗](skills/android-dialog/SKILL.md) | 已有弹窗文案、样式、局部绑定/回调不读。 |
| AppRouter、deep link、登录拦截、全局事件、主 Tab | [导航与事件](skills/android-navigation-events/SKILL.md) | 单个已知目标跳转或补参数只查目标代码。 |
| 完整 Figma/截图精确还原且 quick 不足 | [Figma XML](skills/android-figma-xml-ui/SKILL.md) | 逻辑搬迁、修引用、静态检查不触发。 |
| XML token、drawable 值命名、资源冲突 | [XML 资源规则](android-codegen-xml-resource-rules.md) | 局部 XML 只检查触达行和直接资源。 |

## 高风险补充

### Room

- Entity/Dao/Database/mapper 的真实位置以当前分支为准；新增或修改 Entity 时检查 `AppDatabase` version、历史 schema 和 migration。
- 结构、索引、主键变化必须有可验证迁移策略；破坏性迁移红线见 `AGENTS.md`。
- 无法确认当前版本或历史 schema 时停止猜测，在输出中列为人工确认项。

### SDK 与版本差异

- 先查当前依赖、Manifest、已有 Manager/平台包和合规初始化，不按来源项目或另一地区版本推断。
- 支付、登录、授权、奖励发放等最终状态以服务端校验为准。
- 多版本差异收敛在当前项目已有接口/策略层，不把第三方 SDK 类型扩散到业务调用方。

### 多语言

- 新文案只同步当前分支实际维护且需求覆盖的 `values*/strings.xml`，不因模板来源新建语言目录。
- 新增前查重复 key；长文案、按钮和标签验证伸缩、换行和省略。
- 无法提供准确翻译时保留已指定的默认文案，并明确人工确认项。

敏感信息、日志和 `OssManager` 禁令只以 `AGENTS.md` 为准，本文件不复制。
