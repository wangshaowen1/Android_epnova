# Android Figma XML 快速规则

本文件用于 Figma/截图到 Android XML 的首轮实现或检查。只有目标节点信息不足、整页重写、复杂资源导出或用户要求完整精确还原时，才读取 `.agents/skills/android-figma-xml-ui/SKILL.md`。

## 首轮动作

1. 从 Figma URL 解析 `fileKey` 和 `node-id`。
2. 有 `node-id` 时只读取目标节点；没有 `node-id` 时先定位目标节点，不直接分析整个文件。
3. 用 `rg -n` 定位目标 XML、Fragment/Activity/ViewModel 和同类 XML。
4. 按目标节点关键值精确查资源：文本色语义、间距、圆角、目标 drawable 名、颜色值、描边值。
5. 先完成目标区域，再做相关项自检：XML 解析、直接资源存在、BindingAdapter 存在、触达行 token/字体/drawable 命名。

## 首轮不要做

- 不读整个 Figma 文件。
- 不递归扫描整个 `res`。
- 不读取所有 style/theme/attrs/colors/dimens，只查本次值相关项。
- 不清理目标文件以外的历史 XML 问题。
- 不跑 Gradle，除非用户要求或当前任务必须验证构建。

## Android 映射

- 页面 XML 默认 `<layout>`，页面 ViewModel 变量名 `layout`，item 变量名 `item`。
- 点击优先用项目已有 click binding 或 `setOnSingleClickListener`。
- Figma 数值优先映射项目资源：`style/theme/attr/color/dimen/drawable/selector/BindingAdapter`。
- `4dp/8dp/12dp/16dp/24dp` 间距优先映射 `spacing_xs/s/m/l/xl`。
- `4dp/8dp/12dp/16dp` 圆角优先映射 `radius_s/m/l/xl`。
- `Text1` 到 `Text5` 优先映射 `?attr/textColor1` 到 `?attr/textColor5`。
- 不写本地没有的字体兜底，例如 `sans-serif-medium`、`sans-serif`、`PingFang SC`。
- 普通 `letterSpacing` / `lineHeight` 不落 XML；只有特殊视觉需求且无项目 style 对应时才局部保留。

## Drawable

- 新增 drawable 前先按候选名、颜色值、圆角值、描边值查等价资源。
- 不透明纯色背景按值命名，例如 `bg_242435_r16.xml`；透明色背景按项目透明百分比口径命名，例如 `bg_08_ffffff_r20.xml`；不要用页面语义名。
- 描边背景先查项目已有同类命名，沿用 `bg_<填充色>_stroke_<描边色>_rxx` 或 `bg_<填充色>_stroke_<透明百分比>_<描边色>_rxx` 等现有形式。
- 胶囊类背景统一 `r50`。
- 图片、复杂渐变、组合光效或业务语义强的资源才允许语义名。

## 输出

- 说明处理的目标页面/组件/区域。
- 说明已做的静态检查。
- 只列本次无法映射或需要人工确认的点。
