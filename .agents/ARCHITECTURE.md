# Agent 工作流架构说明

> 给**开发者 / 维护者**:解释这套 AI 代码生成工作流怎么组织、相比历史改了什么、为什么这么改。
> 日常使用看根目录 `AGENTS.md` 和本目录 `README.md` 即可,本文是设计背景。

## 一句话

一套**跨工具(Codex + Claude Code + Android Studio Gemini)、轻量、渐进披露**的 Android AI 代码生成工作流:薄入口 `AGENTS.md` 只承载红线 + 档位 + 路由,规则细则 / skill / 记忆按需加载,目录与格式对齐各家官方推荐。

核心理念:**我们要 AI 帮写代码,不是搭流程仪式。** 规则越多、越冗余、越冲突,只会让 AI 更慢、更易幻觉、更费 token、注意力更稀释。

## 当前架构

```
根目录
├── AGENTS.md      薄调度入口(~67 行):Gradle 默认 · 红线禁令(唯一来源) · 执行档位 · 路由清单 · 输出
│                  ← Codex / Cursor / Copilot / Gemini CLI 等原生读取
├── CLAUDE.md      Claude Code 入口:@import AGENTS.md(官方模式)+ 记忆分层说明
│                  ← Claude Code 不原生读 AGENTS.md,经此复用同一份规则
└── .agents/       ← 对齐 Android Studio Quail 1+ 官方 skills 路径
    ├── README.md                 目录导航(只回答"哪个文件管什么")
    ├── ARCHITECTURE.md           本文
    ├── android-codegen-*.md      规则细则(单一职责,按场景/档位按需读)
    │     quick-rules / check-quick / figma-xml-quick / project-rules /
    │     architecture-rules / xml-resource-rules / scenarios / checklist
    ├── skills/<name>/            场景 skill(命中场景才加载 = 渐进披露)
    │     ├── SKILL.md            主体(name+description 决定何时加载;所有工具读)
    │     ├── references/         项目示例(命中后按需读)
    │     └── agents/openai.yaml  仅 Codex 用;Claude / Android Studio 跳过不读
    └── memory/                   CoALA 记忆分层(团队 + Codex + Claude 共享)
          ├── semantic/           稳定约定:workflow-principles、design-tokens
          ├── episodic/           决策日志:decisions.md(按需手动追加)
          └── user-preferences.md 协作偏好
```

| 层 | 职责 | 加载时机 |
| --- | --- | --- |
| `AGENTS.md` / `CLAUDE.md` | 红线、档位、路由、行为准则 | **每次会话常驻** |
| `.agents/android-codegen-*.md` | 规则细则,每个文件单一职责 | 命中档位 / 场景才读 |
| `.agents/skills/*/SKILL.md` | 单一场景的完整操作手册 | 命中该场景才读 |
| `.agents/memory/` | 跨会话的稳定约定与历史决策 | 召回时读 |

执行分**档位**(progressive disclosure):快速局部修改 → 标准代码生成 → Figma/UI → 代码审查 → 文档维护。能用轻量路径完成就不升级,小改不读完整规范、不读 skill。

## 设计原则

- **单一来源**:每条规则只有一个家;红线禁令只在 `AGENTS.md`《关键禁令》,其它文档只引用不重复。
- **渐进披露**:薄入口 + 细则外链;按需加载,不把全部规则灌进上下文。
- **结构由真实失败倒逼**:不为"完整性 / 对称性"预先堆砌规则或抽象层。
- **注意力是预算**:加任何规则 / 框架前,先问"它减少的真实错误是否大于消耗的注意力";指不出它防住哪个**已发生**的失败,就不加。
- **跨工具单文件源**:规则只写一份(`AGENTS.md`),各工具按各自机制复用,杜绝多份漂移。

## 历史对比与改进

这套工作流是在旧本地化资料基础上迁移到 `.agents/` 的结果,本轮做了系统性本地化:

| 维度 | 历史状态 | 现在 | 为什么改 |
| --- | --- | --- | --- |
| 颜色 attr | 新资料里混入了语义 token 假设,旧资料里有当前项目真实数字 token | 以当前 `attrs.xml` / `styles.xml` 为准:`themeColor1-3`、`textColor1-5`、`bgColor1-6`、`fillColor1-4`、`funcColor*` | agent 文档必须服务当前代码,不能为了通用命名体系生成不存在的资源 |
| 字号 style | 新资料里混入通用语义 style 假设 | 以当前 `styles.xml` 为准:`Text28/20/18/16/14/13/12/11/10` + `Text.Bold/SemiBold/Medium/Regular` | UI 还原时优先复用真实 style,避免 XML 引用不存在的样式 |
| 目录名 | 旧目录名暗示设计稿,实则装规则/skill/记忆/业务资料 | `.agents/` 承载规则、skill、memory,专项资料迁入 `.agents/project/` | 对齐 Android Studio 官方 skills 路径,并让目录名符合 agent 工作流用途 |
| 红线禁令 | 在多个文件里重复,且容易和当前代码事实漂移 | 收敛到 `AGENTS.md` **唯一来源**;其它文档只引用不重复 | 重复 = 改一处漏多处会漂移;冲突 = AI 生成编译不过或用回废弃名 |
| 入口文档 | 有冗余的 `guidelines.md`(第二份索引);`AGENTS.md` 臃肿(~95 行) | 删 `guidelines.md`;`AGENTS.md` 瘦身为薄入口(~67 行) | 官方建议 AGENTS.md 越短越好;常驻文档越短,AI 解析越快、注意力越集中 |
| 规则文件职责 | 内容交叉重复 | 每个文件单一职责,互相引用不重复 | 让 AI(和人)一眼知道去哪查,减少冲突 |
| 记忆 | 带有通用占位和错误 token 事实 | CoALA 分层(semantic / episodic);记录 Epnova 当前事实 | 学界分类框架（CoALA）；操作纪律明确：semantic 可编辑去重，episodic 只追加不改写 |
| 记忆写入 | 自动 hook 易带来不可控成本和工具依赖 | **移除 hook,改纯手动** | 记忆只沉淀稳定约定或明确踩坑,不让后台流程替用户消费资源 |
| 语言 | 中英混排 | 统一中文 | 团队一致,降低理解成本 |
| 跨工具 | 只有 `AGENTS.md`,Claude Code 读不到 | `CLAUDE.md` 经 `@import AGENTS.md` 复用 | Claude Code 不原生读 AGENTS.md(官方确认),import 是官方推荐的零分叉做法 |

## 官方 / 行业对标

| 组件 | 对标标准 | 状态 |
| --- | --- | --- |
| `AGENTS.md` | Linux Foundation 跨工具开放标准(Codex/Cursor/Copilot/Gemini/Aider 等) | ✅ |
| `CLAUDE.md @import` | Anthropic 官方跨工具复用模式 | ✅ |
| `.agents/skills/` | Android Studio Quail 1+ 官方 skills 路径 | ✅ 精确命中 |
| `SKILL.md`(name+description+渐进披露) | Agent Skills 开放标准 / Anthropic Skills | ✅ |
| `references/` `agents/` 子目录 | Skills 标准可选资源目录(`agents/openai.yaml` 为 Codex 附加) | ✅ |
| 记忆 semantic / episodic 分层 | CoALA 记忆分类（学界分类框架，非工业界标准） | ✅ |
| 颜色 / 主题 token | 以当前 Android `attrs.xml`、`styles.xml`、`colors.xml` 为准 | ✅ 已本地化 |

## 给开发者的日常用法

1. 打开 Claude Code / Codex,直接说任务——`AGENTS.md` 已自动加载红线 + 档位 + 路由。
2. 小改直接做;中等任务它会自己按场景读对应 skill;Figma 还原才进 Figma 流程。
3. 想沉淀经验,会话结尾说一句"本轮有没有需要更新的 memory?",由 AI 判断是否写入 `.agents/memory/`(仓库已记录的不重复存,只存前瞻性判断)。
4. 维护规则时守住:**红线只进 `AGENTS.md`;每个文件单一职责;能不加就不加。**
