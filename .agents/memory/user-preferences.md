---
name: user-preferences
description: 团队协作偏好 — 语言、设计还原期望、命名约定要求
metadata:
  type: user
---

**语言**：用户使用简体中文沟通，中文提问时用中文回复，表达简洁直接。

**设计还原**：用户自行做最终布局微调（如标题左对齐改居中）。不要过度设计布局——给出结构正确的基础版本，留给用户打磨。

**颜色 / 资源值**：用户要求值与设计稿（Figma）严格一致。**不要臆测或猜测 hex 色值**——一律从真实 Figma 节点获取；臆造值会被驳回。

**命名约定**：资源命名先跟随当前项目真实资源体系。当前 `attrs.xml` 仍使用 `themeColor1`、`textColor1`、`bgColor1`、`fillColor1` 等数字 token，不能凭通用语义命名生成不存在的 `colorBrand` / `textColorPrimary` / `bgPrimary`。

> 此为团队默认偏好；若当前代码后续完成 token 重命名，再同步更新本文件和 XML 规则。
