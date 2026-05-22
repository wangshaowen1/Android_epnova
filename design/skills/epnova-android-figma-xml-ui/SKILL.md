---
name: epnova-android-figma-xml-ui
description: "Epnova Android Figma/截图到 XML UI 还原规范。用户提供 Figma 链接、Figma 节点、设计稿、截图、标注图、切图资源，或要求按 Figma 实现、截图还原、像素级还原、精确实现 Android XML 页面/组件/弹窗/卡片/列表/item/状态页时使用。重点约束项目 style/theme/attr 优先、Text style 映射、?attr/textColor1/textColor2/bgColor/themeColor、资源命名、drawable 命名、图片导出、文案控件不写死宽高、多语言适配、XML DataBinding/ViewBinding、BindingAdapter。"
---

# Epnova Android Figma XML UI

## 执行顺序

生成或审查 Figma/截图还原 UI 时，按这个顺序处理：

1. 读取设计源：有 Figma 节点/链接时优先用可用 MCP 工具读取节点、布局、字体、颜色、圆角、阴影和资源；没有设计源时说明依据来自截图/标注。
2. 查项目资源：先看 `styles.xml`、`attrs.xml`、`colors.xml`、`dimens.xml`、已有 `drawable/selector`、`BindingAdapters.kt` 和同类 XML。
3. 映射项目规范：能映射到项目 style/theme/attr/dimen/drawable 的，不手写局部值。
4. 局部精确值兜底：项目没有对应资源时，才在当前 XML 或新资源中使用设计稿精确值，并说明无法映射的点。
5. 自检：检查首屏结构、间距、字号、颜色、圆角、图片、文案伸缩和多语言风险。

项目资源索引见 [project-examples.md](references/project-examples.md)。

## XML 基础

- 页面 XML 默认使用 `<layout>`，页面 ViewModel 变量固定为 `layout`；item XML 变量固定为 `item`。
- 可见 UI 仍按项目 MVVM：Activity/Fragment 初始化 View、监听点击、观察 ViewModel；复杂业务表达式不要写进 XML。
- 图片加载优先使用项目 BindingAdapter：`loadUri`、`loadThumbnailUri`、`loadHQThumbnailUri`、`loadVideoUrl`。
- 点击优先使用项目已有 click binding 或 `setOnSingleClickListener`，不要在 Adapter 里写业务请求。
- Figma 的 Auto Layout 映射到 Android 时，优先用 `ConstraintLayout`、`LinearLayout`、`RecyclerView`、`0dp` 约束、`wrap_content`、`padding/margin`，不要用一堆绝对位置堆出来。

## Style / Theme / Attr 映射

先复用主题和文本 style，再考虑新增资源。

- 主题色：`?attr/themeColor1`、`?attr/themeColor2`、`?attr/themeColor3`。
- 文本色：`?attr/textColor1`、`?attr/textColor2`、`?attr/textColor3`、`?attr/textColor4`、`?attr/textColor5`。
- 背景色：`?attr/bgColor1`、`?attr/bgColor2`、`?attr/bgColor3`、`?attr/bgColor4`、`?attr/bgColor5`。
- 功能色：优先查 `?attr/funcColorSuccess`、`?attr/funcColorWarning`、`?attr/funcColorError`、`?attr/funcColorLink`。
- 常规填充：优先查 `?attr/fillColor1`、`?attr/fillColor2`、`?attr/fillColor3`、`?attr/fillColor4`。
- 间距/圆角：优先复用 `@dimen/spacing_xs`、`spacing_s`、`spacing_m`、`spacing_l`、`spacing_xl`、`radius_s`、`radius_m`、`radius_l`、`radius_xl`。

Figma 标注为 `Text1`、`Text2`、`Text3`、`Text4`、`Text5` 时，优先映射到 `?attr/textColor1` 到 `?attr/textColor5`，不要直接把标注名当资源名。

## 文本与文案

- 字体权重优先使用 `@style/Text.Bold`、`@style/Text.SemiBold`、`@style/Text.Medium`、`@style/Text.Regular`。
- 字号层级优先使用 `@style/Text28`、`Text20`、`Text18`、`Text16`、`Text14`、`Text13`、`Text12`、`Text11`、`Text10`。
- 不要重复手写 `android:fontFamily`、`android:textStyle`，除非设计稿字体在项目 style 中没有对应项。
- 文案必须走 string 资源，新增 key 同步 `values`、`values-zh`、`values-th`、`values-in` 的 `strings.xml`。
- TextView、Button、EditText、标签等文案控件尽量不要写死宽高；优先使用 `wrap_content`、`0dp` 约束、`minWidth/minHeight`、`maxWidth`、`maxLines`、`ellipsize`、padding/margin。
- 按钮如果视觉上固定高度但文案可变，允许固定高度或 `minHeight`，宽度优先 `wrap_content` / `0dp` + 约束，避免多语言溢出。

示例：

```xml
<TextView
    android:id="@+id/tvTitle"
    style="@style/Text16"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:ellipsize="end"
    android:maxLines="1"
    android:text="@string/series_title"
    android:textColor="?attr/textColor1" />
```

## Drawable 与资源命名

新增资源先查是否已有可复用文件。确实需要新增时，遵守项目命名 skill：`epnova-android-naming`。

- 通用纯色背景：`bg_透明值_颜色值_r/tr/br_radius值.xml`，例如 `bg_20_ffffff_r8.xml`。
- 胶囊类背景统一 `r50`，例如 `bg_ff_ffffff_r50.xml`。
- 通用渐变：`gradient_透明值1_颜色值1_透明值2_颜色值2_渐变方向_r/tr/br_radius.xml`。
- 界面特有或功能语义强的资源：允许 `shape_bg_功能_xx.xml`，例如 `shape_bg_publish_task.xml`。
- 图标优先 `ic_xxx.xml` 或 `icon_xxx.xml`；图片优先 `img_xxx.webp/png`。
- selector 使用 `selector_xxx.xml`、`cb_xxx.xml`；按钮背景沿用项目已有 `btn_xxx.xml` 风格。
- 不新增 `bg_gray.xml`、`shape_1.xml`、`gradient_button.xml` 这类不可搜索泛名。

## 设计资产

- 明确的图标、图片、头像、插画、背景必须使用设计稿导出资源或项目已有资产。
- 有 Figma 节点资源时优先通过 MCP 工具下载 SVG/PNG，并保存到符合命名规则的位置。
- 不用临时占位图替代明确资产；缺资产时在输出中说明。
- 矢量图标能复用项目已有 drawable 时优先复用；多色复杂图、照片、插画优先使用 PNG/WebP。

## 不要做

- 不用 CSS/SCSS/Web token 术语实现 Android XML；遇到类似 CSS token 的设计变量，要翻译成项目 `style/theme/attr/color/dimen/drawable`。
- 不把设计说明、标注文案、开发备注写进页面 UI。
- 不为了像素值完全一致而绕开项目 style/theme，除非项目没有对应资源。
- 不在 XML 中写复杂业务判断；复杂逻辑放 ViewModel 或 BindingAdapter。
- 不因为截图里文字短就写死文案控件宽度，多语言会放大这个问题。

## 完成前检查

- 是否已经读取 Figma/截图，并说明无法读取的部分。
- 字体、文本色、背景色、间距、圆角是否优先映射项目资源。
- `Text1` 等文本层级是否映射到 `?attr/textColor1` 等主题色。
- 文案是否走 string 资源，文本控件是否避免固定宽高。
- 新增 drawable 是否符合 `bg_透明值_颜色值_rxx` / `gradient_...` / `shape_bg_功能_xx`。
- 图片是否使用真实设计资产或项目已有资产。
- XML 是否符合 DataBinding/ViewBinding、`layout`/`item` 变量和已有 BindingAdapter 习惯。
