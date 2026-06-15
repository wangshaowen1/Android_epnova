# CLAUDE.md

> Claude Code 专属入口。团队共享规范全部在 `AGENTS.md`（Codex / Cursor / Copilot / Gemini CLI 等也读它）。
> Claude Code 不原生读 `AGENTS.md`，故此文件通过官方 `@import` 复用它，避免维护两份规则。
> 新增团队规范一律写进 `AGENTS.md`；只有 Claude 专属能力（skill 触发、记忆分层）才写本文件。

@AGENTS.md

## 记忆分层（CoALA：procedural / semantic / episodic）

- **程序记忆（怎么做）**：`.agents/skills/<name>/SKILL.md`，由 skill 路由按需加载。
- **语义记忆（稳定事实与约定）**：`.agents/memory/semantic/*.md`。
- **情节记忆（版本决策 / 踩坑 / 验证结论，追加式）**：`.agents/memory/episodic/decisions.md`。
- **索引**：`.agents/memory/MEMORY.md`。

写记忆规则：语义记忆按条目更新，去重、纠错优先于新增；情节记忆只追加，不删除、不改写历史条目。修改任何记忆文件前先 Read。

## 记忆写入

记忆由对话中按需判断后写入，**不依赖任何 hook 或外部 CLI**：

- 用户说“本轮有没有需要更新的 memory？”时，判断是否有值得留的内容并写入对应文件。
- 仓库、commits、`.agents/` 文档已记录的不重复存；只存不在任何文件里的前瞻性判断。
- 团队级决策写 `.agents/memory/`（随仓库走，Codex 共享）。

## 跨工具一致性

`CLAUDE.md` 与 `AGENTS.md` 不得出现规则分叉。本文件只承载 Claude 专属能力，规则正文以 `@AGENTS.md` 导入为唯一来源。
