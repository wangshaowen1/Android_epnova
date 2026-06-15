---
name: design-tokens
description: Epnova 当前颜色/主题 token 的两层结构与 Figma 映射约定
metadata:
  type: reference
---

Epnova 当前颜色采用两层结构:`attrs.xml` 主题属性 + `colors.xml` 实际色值。

当前分支经 `attrs.xml`、`styles.xml`、`colors.xml` 核实，仍使用数字层级 token；agent 不能凭通用语义命名改写成不存在的资源。

- attr 命名的**权威清单**以 `app/src/main/res/values/attrs.xml` 为准：主题色 `themeColor1-3`，文字色 `textColor1-5`，背景 `bgColor1-6`，填充 `fillColor1-4`，功能色 `funcColorSuccess/Warning/Failure/Link/Special`。
- `AppTheme.Base` 当前绑定暗色实际值：`theme_color_1_dark=#FF2E55`、`theme_color_2_dark=#78A2FF`、`theme_color_3_dark=#337B9FFC`，`text_color_1_dark=#F5FFFFFF` 到 `text_color_5_dark=#4DFFFFFF`，`bg_color_1_dark=#0D0D0D` 到 `bg_color_6_dark=#F6F6F6`，`fill_color_1_dark=#0FFFFFFF` 到 `fill_color_4_dark=#4DFFFFFF`。
- Figma→token 的**映射规则**见 `.agents/android-codegen-xml-resource-rules.md`《Resource Token》,本文件不重复。
- 新增 UI 优先引用当前真实 `?attr` / `@style` / `@dimen` / `@drawable`；不要直接硬编码 hex。设计稿有项目未覆盖的精确值时，按规则局部兜底并说明原因。
- `textColorPrimary`、`colorBrand`、`bgPrimary`、`fillPrimary`、`outlineDefault` 等语义 token 当前不在 `attrs.xml` 中，不得在 XML 中直接生成。

**为什么记在 memory**:这是跨会话容易被通用模板覆盖的项目事实(当前数字 token + 不可硬编码 + 不可臆测色值),便于 Codex/Claude 召回;具体规则仍由规则层与当前资源文件承载,避免漂移。
