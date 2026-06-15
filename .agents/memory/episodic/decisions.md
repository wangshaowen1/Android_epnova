---
name: decisions
description: 版本决策、踩坑与验证结论（追加式情节记忆，按需手动维护）
metadata:
  type: project
---

> 情节记忆：记录「发生了什么」——版本决策、踩坑、验证结论。**只追加，不删除、不改写历史条目。**
> 由对话中按需判断后手动追加（用户说“本轮有没有需要更新的 memory？”时触发）。

## 条目格式

```
## v<版本或"本轮"> <变更主题>

### 变更了什么
<字段 / 文件> → <新增 / 删除 / 修改>：<一句话>

### 为什么
<具体症状：哪个版本、哪一处、出现了什么问题>
<根因分析 + 决策推导>

### 验证结论
<验证方式 + 结果，含具体数据> 或 PENDING（说明待验证项）
```

---

## 本轮 agent 工作流本地化迁移

### 变更了什么
旧本地化资料 → `.agents/` 新结构：业务专项文档迁入 `.agents/project/`，代码接口扫描摘要迁入 `.agents/code-interface-scan-summary.md`，场景 skill 示例迁入 `.agents/skills/*/references/`。

### 为什么
`.agents/` 来自新工作流结构，但包含通用占位和错误 token 假设；旧本地化资料保留了 Epnova 的真实业务资料、资源 token 和可复用组件线索。为避免 agent 继续按模板生成不存在的包名、色值或样式，规则和记忆必须以当前分支代码事实为准。

### 验证结论
PENDING：已静态核实 `com.kuaigeglobal.epnova`、`EpNova`、`epnovascheme`、`epnova.onelink.me`、`themeColor1/textColor1/bgColor1/fillColor1`、`Text28/Text16` 等当前事实；旧本地化目录删除需等待用户确认清单后执行。
