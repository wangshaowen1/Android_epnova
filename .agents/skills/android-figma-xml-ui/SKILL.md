---
name: android-figma-xml-ui
description: "Android Figma/截图到 XML UI 还原规范。仅在本次实际按 Figma/截图生成或校准 Android XML 可见 UI，且 android-figma-xml-quick.md 无法覆盖时使用。用户只是提供截图说明 bug、检查差异、逻辑搬迁、事件绑定、ViewModel 状态同步、修引用或静态检查时，不触发完整 Figma UI skill。"
---

# Android Figma XML UI

首轮实现和检查先读 `.agents/android-figma-xml-quick.md`。只有目标信息不足、整页重写、复杂资产导出或用户要求完整精确还原时才进入本文。

## 读取边界

- 有 `node-id` 时只读目标节点；没有时先定位目标区域，再按缺口扩大。
- 本地资源先按候选值、候选名和同类 XML 精确搜索；新增资源或候选不足时才扩大 `drawable*` 扫描。
- token 与视觉检查只覆盖本次触达区域；页面级重写才做整页检查。
- 不默认读取整个 Figma 文件、全部 style/theme 或整个 `res`。

## 落地顺序

1. 明确页面、组件或区域边界，并读取对应设计节点/截图标注。
2. 读取目标 XML、直接代码和一个最近的同类 XML。
3. 按本次设计值精确查询 style、attr、color、dimen 和 drawable。
4. 能映射当前项目 token 就复用；无等价资源时保留局部精确值并说明。
5. 实现后检查层级、约束、间距、文本、图片裁剪、状态和直接资源引用。

颜色、间距、圆角、字体、行高/字距和 drawable 命名只由 `.agents/android-codegen-xml-resource-rules.md` 维护；本文不复制映射表。需要定位当前资源入口时再读 [项目 UI 索引](references/project-examples.md)。

## Android 结构

- 保留 Auto Layout 的方向、gap、padding、对齐和伸缩关系；使用 `ConstraintLayout`、`LinearLayout`、`RecyclerView`、`0dp` 约束与 `wrap_content` 表达，不堆绝对坐标。
- 页面 `layout`、列表 `item`、BindingAdapter、点击和 MVVM 边界跟随目标文件；不为简单展示新增透传状态或业务逻辑。
- 文案来自设计/需求并走 string；标注和开发备注不能写进 UI。
- 长文案、多语言和系统字号要验证可伸缩宽度、`maxLines`、`ellipsize`、滚动或换行，不能因截图中文字短而固定文案控件宽度。
- 字体文件缺失时沿用项目字体并说明，不能写系统字体名伪装成精确还原。

## 设计资产与位图密度

- 明确的图标、照片、插画和背景使用设计导出资源或项目已有资产；缺资产时说明，不用占位图冒充完成。
- 矢量且效果等价时优先复用/导出 Vector；多色复杂图、照片和插画使用 PNG/WebP。
- 位图目录必须与实际导出倍率匹配：1x/2x/3x 分别按当前项目密度策略落目录；只有确有多套导出才维护多个同名文件。
- 不把 1x 位图放进 `drawable-nodpi` 再靠 ImageView 放大；这会产生模糊且绕过 density 缩放。
- 保存前检查文件真实格式、像素尺寸、透明通道和资源目录，不能只相信扩展名或 Figma 节点名。

## 检查

- 只加载了目标节点和相关资源，缺失信息已说明。
- 结构、约束、文本伸缩和状态符合设计，token 复用符合 XML 资源规则。
- 图片使用真实资产，位图倍率、目录、格式、尺寸和 alpha 匹配。
- XML 可解析，直接资源和 BindingAdapter 存在；没有把业务判断或无意义透传状态塞进 XML/VM。
