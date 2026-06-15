---
name: workflow-principles
description: agent 工作流的取舍原则——轻量、单一来源、渐进披露;已评估并否决重型 spec 框架
metadata:
  type: reference
---

本工程的 agent 工作流(`.agents/` + `AGENTS.md` / `CLAUDE.md`)刻意保持轻量,目标是"AI 帮写代码",不是流程仪式。

- **单一来源**:红线禁令只在 `AGENTS.md`《关键禁令》;规则按单一职责分文件,不重复。
- **渐进披露**:按档位 / 场景按需加载,薄入口 + 细则外链(对应 Anthropic Skills 官方原则)。
- **结构由真实失败倒逼**,不为"完整性 / 对称性"预先堆砌规则或抽象层。
- **取舍标准**:一条规则 / 框架 / 工具能减少的真实错误,是否大于它消耗的注意力;指不出它防住哪个**已发生**的失败,就不加。

**已评估并否决(2026-06):** OpenSpec 等重型 spec-driven 框架(propose→apply→archive)。理由:仪式与轻量档位哲学冲突,对 Figma / UI 驱动的 Android 任务边际价值低,增加注意力税。仅当出现"大功能反复返工"的真实失败时才重新考虑。

**记忆机制为纯手动**(无 hook、无外部 CLI):曾搬来的 PreCompact 自动 hook 因本环境无 `claude` CLI 且属非核心,已删除。

**为什么**:规则多 / 冗余 / 逻辑冲突会让 AI 耗时慢、幻觉、token 浪费、注意力稀释。
