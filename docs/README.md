---
title: EpNova 项目文档
type: overview
status: active
last_verified: 2026-08-14
---

# EpNova 项目文档

## 项目职责

本仓库是 EpNova Android 客户端，承载 AI 剧集创作、作品管理与播放、账号、支付等业务，并维护供 Codex、Claude Code 等工具共同使用的开发规则。包名、业务能力、依赖和运行行为始终以当前分支代码为准。

## 系统地图

| 领域 | 职责 | 稳定锚点 | 详细文档 |
| --- | --- | --- | --- |
| AI 开发入口 | 定义红线、任务档位、读取优先级和输出边界 | [`AGENTS.md`](../AGENTS.md) | [AI Skill 与规则治理](architecture/ai-skill-governance.md) |
| Android 规则路由 | 按任务范围路由到 quick rules、场景规则和 Skill | [`.agents/README.md`](../.agents/README.md) | [AI Skill 与规则治理](architecture/ai-skill-governance.md) |
| App 实现 | 提供当前分支的 Base、网络、列表、分页、路由和 UI 能力 | `app/` | [App 分层架构](architecture/app-layering.md) |
| 媒体选择 | 通过 Maven 接入 Matisse，并统一系统 Photo Picker 路由、主题与文案 | `modules.gradle`、`app/src/main/java/com/kuaigeglobal/epnova/common/picker/AppImagePicker.kt` | [Matisse 媒体选择器接入契约](contracts/matisse-media-picker.md) |
| 专项业务资料 | 保存复杂功能的分析、流程与实施判断 | [`.agents/project/`](../.agents/project/) | 仅按目标业务读取对应文档 |

## 核心约束

- 用户本次明确要求和当前代码同类实现优先于项目说明。
- 红线只在 [`AGENTS.md`](../AGENTS.md) 定义，其他文档只引用。
- Skill 只沉淀 AI 容易重复犯错的项目约束、真实代码索引或高代价决策点。
- 快速局部任务不升级到完整规则或 Skill；标准任务只加载命中的最小集合。

## 有效文档

| 文档 | 用途 | 状态 | 适用范围 |
| --- | --- | --- | --- |
| [AI Skill 与规则治理](architecture/ai-skill-governance.md) | 解释规则分层、Skill 准入、触发颗粒度和关键取舍 | active | 维护 `AGENTS.md`、`.agents` 规则和 Skill 时 |
| [App 分层架构](architecture/app-layering.md) | 指导共享代码归属、依赖和渐进治理 | active | Android `app` 及本地库模块 |
| [Matisse 媒体选择器接入契约](contracts/matisse-media-picker.md) | 说明依赖、选择器路由、主题优先级与本地化边界 | active | 所有媒体选择调用方 |

## 推荐阅读路径

- 新开发者：先读 [`AGENTS.md`](../AGENTS.md)，需要理解设计原因时再读 [AI Skill 与规则治理](architecture/ai-skill-governance.md)。
- 修改共享代码归属、跨层依赖或 `app/base`：只读 [App 分层架构](architecture/app-layering.md)。
- 接入或升级媒体选择器：只读 [Matisse 媒体选择器接入契约](contracts/matisse-media-picker.md)。
- 任务型 AI：先按 `AGENTS.md` 判断档位，再由 [`.agents/android-codegen-quick-rules.md`](../.agents/android-codegen-quick-rules.md) 路由；不要默认加载架构文档。
- 规则维护者：阅读 [AI Skill 与规则治理](architecture/ai-skill-governance.md) 后，再修改对应的唯一事实源。
