# Memory Index

Epnova Android 工程的智能体记忆索引，遵循 CoALA 记忆分层（procedural / semantic / episodic）。
记忆只记录跨会话稳定事实、明确决策和踩坑；已经在规则文档中权威定义的内容不重复存。

## 语义记忆 semantic/（稳定事实与约定，按条目更新、去重纠错优先）

- [workflow-principles.md](semantic/workflow-principles.md) — agent 工作流取舍原则（轻量 / 单一来源 / 渐进披露；已否决 OpenSpec）
- [design-tokens.md](semantic/design-tokens.md) — 当前项目颜色 / 主题 token 与 Figma 映射约定

> 技术栈、Base 类、目录、代码范式等**项目约定属于规则层**，权威定义见 `../android-codegen-project-rules.md`、`../android-codegen-architecture-rules.md`；memory 不再平行维护一份，避免漂移。

## 情节记忆 episodic/（版本决策与经验，只追加）

- [decisions.md](episodic/decisions.md) — 版本变更决策、踩坑与验证结论（按需手动追加）

## 用户 / 协作偏好

- [user-preferences.md](user-preferences.md) — 协作语言、设计还原期望、命名约定要求

> 程序记忆（procedural）= `.agents/skills/<name>/SKILL.md`，由 skill 路由按需加载，不在此索引重复列出。
