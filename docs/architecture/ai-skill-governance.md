---
title: AI Skill 与规则治理架构
type: architecture
status: active
last_verified: 2026-08-05
---

# AI Skill 与规则治理架构

## 背景与目的

本仓库的 AI 规则需要同时解决两个相反的问题：一方面，AI 必须知道当前项目独有的实现和高风险边界；另一方面，规则过多、重复或过于通用会消耗上下文，使真正重要的约束失去权重。

治理目标不是增加 Skill 数量，而是让 AI 用最少的读取成本命中足以正确完成任务的事实和决策。

## 范围与非目标

本文覆盖：

- `AGENTS.md`、`.agents` 规则文档与场景 Skill 的职责分层。
- Skill 的准入、触发颗粒度、正文和 references 边界。
- API、Tab/ViewPager2、Paging 等本轮关键路由决策。
- 团队维护这些规则时应保持的不变量。

本文不负责：

- 逐项解释所有 Android 类、页面、接口和资源。
- 替代各 Skill 的具体实现规则。
- 记录每次规则修改的版本流水账。
- 教 AI 通用 Kotlin、Android 或软件工程常识。

## 当前模型

规则采用渐进披露模型：

```text
AGENTS.md 红线与任务档位
  → android-codegen-quick-rules.md 一级路由
    → android-codegen-scenarios.md 组合场景
      → 命中的 SKILL.md
        → 必要时读取 references/
```

| 层级 | 负责 | 不负责 |
| --- | --- | --- |
| `AGENTS.md` | 常驻红线、执行档位、读取优先级和输出边界 | 展开场景实现细节 |
| `.agents/README.md` | 文件与 Skill 导航、维护原则 | 调度逻辑和重复规则正文 |
| quick rules | 标准任务的一级分流和局部快路径 | 完整场景说明 |
| scenarios | 多 Skill 组合和跨领域高风险补充 | 复制 Skill 正文 |
| `SKILL.md` description | 判断何时触发、何时不触发 | 展开实现步骤 |
| `SKILL.md` 正文 | 会改变项目实现选择的规则 | 重复触发说明和行业常识 |
| `references/` | 真实符号、能力边界和按需代码索引 | 默认加载的完整代码清单 |
| `agents/openai.yaml` | Codex 展示信息和明确的 `$skill-name` 调用入口 | 通用规则正文 |

## Skill 准入标准

新建或保留 Skill，至少应满足以下一项：

- AI 在真实任务中容易重复犯错。
- 当前项目存在通用知识无法推出的实现约束。
- 可以为 AI 提供准确、稳定的代码搜索入口。
- 决策涉及生命周期、并发、状态一致性、协议兼容等高代价风险。

下列内容不应独立成为 Skill：

- 驼峰命名、Boolean 前缀、缩进、行长等通用语言规范。
- “遵循最佳实践”“代码要清晰”等没有判别力的描述。
- 当前任务不存在的其他端、其他项目或未来架构推测。
- 可以从目标文件和附近同类实现直接得出的普通写法。

准入判断的核心问题是：这条规则能否阻止一个具体且现实的错误。如果不能，优先不写。

## 触发颗粒度

触发范围必须同时写清正向场景和排除场景，避免关键词误触发完整 Skill。

| 任务 | 读取策略 |
| --- | --- |
| 只改 Tab 文案、样式或一次 `currentItem` | 读取目标代码，不触发 Tab Skill |
| 新增或重构多 Tab + ViewPager2 Fragment 容器 | 读取 `android-tab-viewpager` |
| 多 Tab 且每个子页独立分页 | 读取 `android-tab-viewpager` 和 `android-paging-state-ui` |
| 已有完整接口链路，只补字段、参数或单一调用点 | 走 quick rules 局部接口快路径 |
| 新增接口、跨至少两层修改或调整错误语义 | 读取 `android-api-request` |
| 只改弹窗文案、Toast 或局部样式 | 读取目标弹窗，不触发完整 Dialog Skill |

多个 Skill 组合时，每个 Skill 只负责自己的状态所有权和风险边界，不因标题中出现相关词而连锁加载。

## 关键决策

### 删除通用命名 Skill

原 `android-naming` 大部分内容属于 AI 已掌握的行业常识，独立加载会增加 token，却不能显著降低错误率。因此删除该 Skill，并将真正影响项目实现的规则归还所属领域：

- drawable 数值、透明度和圆角命名归 XML 资源规则。
- `ItemXxxVM` 归 DataBinding 列表 Skill。
- `XxxBody`、`XxxInfo`、`XxxData` 归 API Skill。
- scheme、host、事件值和历史拼写的兼容约束归导航 Skill。

这不是取消命名约束，而是让约束在实际使用它的任务中出现。

### 新增 Tab/ViewPager2 Skill

Tab + ViewPager2 是已确认的 AI 失败场景：AI 知道 Android 官方 API，但无法仅凭通用知识判断当前项目 Adapter 和快捷绑定的真实能力。

当前项目事实包括：

- `FragmentStateCacheAdapter.setFragments()` 只替换列表，不发出 Adapter 通知，并以 Fragment 实例 `hashCode()` 作为 itemId。
- Fragment 列表的安全默认是初始化时稳定设置一次，不能在 Tab 点击或数据刷新时重建。
- `NeedUpdateFragmentStateAdapter` 使用位置 itemId，只覆盖有限的同位置刷新和尾部增删，不是任意动态 Tab 的现成方案。
- `ViewPagerHelper` 和 `PillIndicatorView.attachToViewPager2()` 内部注册匿名 callback，没有对应的解绑入口。
- Tab 点击与页面切换回调若同时发起加载，容易产生重复请求和状态反复写入。

这些事实集中在 [`android-tab-viewpager`](../../.agents/skills/android-tab-viewpager/SKILL.md)，详细搜索入口放在其 [`project-index.md`](../../.agents/skills/android-tab-viewpager/references/project-index.md)。其他项目的业务页面、包名和业务词不进入 Epnova Skill。

### 拆分 Tab 容器与 Paging

Tab 容器和分页子页具有不同的状态所有权：

- Tab Skill 负责容器、Fragment 稳定性、选中同步和 callback 生命周期。
- Paging Skill 负责每个子 Fragment 的 Pager、PagingSource、Adapter 和 LoadState。

因此普通 ViewPager2 不需要加载 Paging Skill；只修改分页子页也不需要重复加载容器规则。多 Tab 分页场景组合读取两者。

### 收紧 API Skill

完整 API Skill 只在以下场景触发：新增接口；新增、修改或审查 `ApiService`、model、Repository、ViewModel 中至少两层；调整 `response/request/requestWithLoading` 的错误语义。

当前分支已有完整同类链路，且只补一个明确字段、参数或调用点时走局部快路径。这样保留跨层接口的完整约束，同时避免局部改动加载整份 API 手册。

## 不变量与失败行为

- 红线禁令只在 `AGENTS.md` 定义；发现副本时应删除副本并保留引用。
- Skill 与当前代码不一致时，以当前分支同类实现为事实，并更新 Skill 或明确差异。
- 从 Template 同步规则后必须重新确认 Epnova 的 namespace、包名、业务域、scheme 和真实依赖。
- references 中没有完整业务实现时，应说明能力边界，不用来源项目路径填补空白。
- 动态 Tab、长任务、数据库迁移、登录和支付等无法从当前事实确认时，必须暴露待确认点，不假设现有封装已支持。
- 一条规则同时出现在多个文件时，应确定唯一归属，其他位置只保留路由链接。

## 实现地图

| 关注点 | 事实或规则锚点 | 验证方式 |
| --- | --- | --- |
| 常驻红线与档位 | [`AGENTS.md`](../../AGENTS.md) | 检查其他文档没有复制红线正文 |
| 一级路由与接口快路径 | [`.agents/android-codegen-quick-rules.md`](../../.agents/android-codegen-quick-rules.md) | 用局部任务和标准任务分别检查命中集合 |
| 组合场景 | [`.agents/android-codegen-scenarios.md`](../../.agents/android-codegen-scenarios.md) | 检查组合顺序和 Skill 职责没有重叠 |
| Skill 清单与维护原则 | [`.agents/README.md`](../../.agents/README.md) | 检查新增 Skill 是否通过准入标准 |
| Tab/ViewPager2 | [`.agents/skills/android-tab-viewpager/`](../../.agents/skills/android-tab-viewpager/) | 对照 Adapter、指示器和 callback 的当前实现 |
| Paging 边界 | [`.agents/skills/android-paging-state-ui/`](../../.agents/skills/android-paging-state-ui/) | 确认容器不持有子页分页状态 |
| API 边界 | [`.agents/skills/android-api-request/`](../../.agents/skills/android-api-request/) | 对照 quick path 与跨层触发条件 |
| 结构校验 | [`.agents/scripts/validate-agent-docs.ps1`](../../.agents/scripts/validate-agent-docs.ps1) | 修改规则或 Skill 后运行，无错误再提交 |

## 维护与验证

维护 Skill 时至少检查：

- `description` 同时包含正向触发和明确排除项。
- 正文没有复制 `description` 的触发清单。
- `agents/openai.yaml` 的 `default_prompt` 明确引用 `$skill-name`。
- references 中的符号在当前分支真实存在，且没有机器绝对路径或来源项目业务路径。
- quick rules、scenarios 和 Skill 之间没有互相矛盾的触发条件。
- 运行 `powershell -ExecutionPolicy Bypass -File .agents/scripts/validate-agent-docs.ps1`，确认：
  - `SKILL.md` 从首字节 `---` 开始，不含会导致发现失败的 UTF-8 BOM。
  - frontmatter 的 name/description、目录名和 `agents/openai.yaml` 一致，`default_prompt` 显式引用 `$skill-name`。
  - 每个 reference 由同目录 `SKILL.md` 直接链接，Skill 注册表与目录一致。
  - 本地 Markdown 链接有效，没有机器绝对路径。

校验脚本只验证确定性结构，不判断规则是否有业务价值。新增或保留规则仍需回答“能否阻止已知错误或提供当前项目无法从通用知识推出的索引”。

Gradle 不用于验证文档结构；涉及真实代码行为时，只在用户明确要求后运行对应构建或测试。
