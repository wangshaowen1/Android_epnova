---
name: android-figma-xml-ui
description: "Android Figma/截图到 XML UI 还原规范。仅在本次实际按 Figma/截图生成或校准 Android XML 可见 UI，且 android-figma-xml-quick.md 无法覆盖时使用。用户只是提供截图说明 bug、检查差异、逻辑搬迁、事件绑定、ViewModel 状态同步、修引用或静态检查时，不触发完整 Figma UI skill。"
---

# Android Figma XML UI

首轮实现或检查优先读取 `.agents/android-figma-xml-quick.md`。只有目标节点信息不足、整页重写、复杂资源导出或用户要求完整精确还原时，才读取本文档。

## 性能优先原则

Figma UI 任务也要先走最小闭环，再按缺口扩大范围。不要为了“精确模式”默认全量读取 Figma 文件、全量扫描 `res` 或全量清理历史 XML。

- 有 Figma `node-id` 时只读取该节点；没有 `node-id` 时才读取页面级结构并尽快定位目标节点。
- 只在目标节点信息不足、组件变量无法判断、资源缺失或用户明确要求整页还原时，才扩大读取深度或读取更多节点。
- 本地资源查询先按候选值精确搜索：同类 XML、`dimens.xml`、`attrs.xml`、`colors.xml`、目标 drawable 名称、颜色值、圆角值、描边值。只有新增 drawable 或候选缺失时，才扫描整个 `drawable*`。
- token 检查只覆盖本次新增/修改的 XML 行和新增资源；不要顺手重构整个历史布局。
- 完成前检查使用“相关项检查”，不要求每次执行完整 checklist。页面级重写才做完整 UI 自检。

## 首轮最小动作模板

Figma 到 XML 的首轮动作最多覆盖以下内容；只有发现缺口才继续扩大：

1. 解析用户提供的 Figma URL，优先拿 `fileKey` 和 `node-id`。
2. 有 `node-id` 时只读取目标节点；没有 `node-id` 时先定位目标节点，不直接分析整个文件。
3. 用 `rg -n` 定位目标 XML、Fragment/Activity/ViewModel 和同类 XML，不整文件读取大文件。
4. 按目标节点出现的关键值精确查资源：文字色语义、间距、圆角、目标 drawable 名、颜色值、描边值。
5. 先完成目标区域实现，再做相关项自检：XML 解析、直接资源存在、BindingAdapter 存在、触达行 token/字体/drawable 命名。

首轮不要做：

- 不读整个 Figma 文件。
- 不递归扫描整个 `res`。
- 不读取所有 style/theme/attrs/colors/dimens，只查本次值相关项。
- 不清理目标文件以外的历史 XML 问题。
- 不跑 Gradle，除非用户要求或当前任务必须验证构建。

## 执行顺序

生成或审查 Figma/截图还原 UI 时，按这个顺序处理：

1. 定位范围：从用户提供的 Figma URL、`node-id`、截图或目标 XML 判断本次只实现哪个页面、组件或区域。
2. 读取设计源：优先读取目标 Figma 节点的布局、字体、颜色、圆角、阴影和资源；没有设计源时说明依据来自截图/标注。
3. 建立最小资源索引：读取目标 XML、同类 XML，以及 `styles.xml`、`attrs.xml`、`colors.xml`、`dimens.xml` 中和本次值相关的部分。
4. 映射项目规范：能映射到项目 style/theme/attr/dimen/drawable 的，不手写局部值。
5. 局部精确值兜底：项目没有对应资源时，才在当前 XML 或新资源中使用设计稿精确值，并说明无法映射的点。
6. 相关项自检：只检查本次变更区域的结构、间距、字号、颜色、圆角、图片、文案伸缩和资源引用；完整页面重写才做整页检查。

项目资源索引见 [project-examples.md](references/project-examples.md)。只有不清楚项目常用 style/token/资源示例时才读取，不要每次 Figma 任务默认读取。

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

### token 映射必须优先于裸值

执行 Figma 精确还原时，先判断设计值是否能精确映射到项目 token。能映射则必须使用 token；不能映射时才保留局部精确值。

| 设计值 / 语义 | XML 写法 |
| --- | --- |
| `4dp` 间距 | `@dimen/spacing_xs` |
| `8dp` 间距 | `@dimen/spacing_s` |
| `12dp` 间距 | `@dimen/spacing_m` |
| `16dp` 间距 | `@dimen/spacing_l` |
| `24dp` 间距 | `@dimen/spacing_xl` |
| `4dp` 圆角 | `@dimen/radius_s` |
| `8dp` 圆角 | `@dimen/radius_m` |
| `12dp` 圆角 | `@dimen/radius_l` |
| `16dp` 圆角 | `@dimen/radius_xl` |
| `Text1` 文本色 | `?attr/textColor1` |
| `Text2` 文本色 | `?attr/textColor2` |
| `Text3` 文本色 | `?attr/textColor3` |
| `Text4` 文本色 | `?attr/textColor4` |
| `Text5` 文本色 | `?attr/textColor5` |

硬性要求：

- `android:layout_margin*="12dp"`、`android:padding*="12dp"` 这类值必须先改为 `@dimen/spacing_m`；其他表内值同理。
- `android:textColor="#..."` 如果对应 Figma 的 Text1/Text2/Text3/Text4/Text5 或项目主题语义，必须改为 `?attr/textColor*`。
- `android:backgroundTint="#..."`、shape solid/stroke/gradient 颜色如果对应项目主题、背景、填充或功能色，必须使用 `?attr`、`@color` 或复用已有 drawable。
- 仅当设计稿出现非项目 token 值，例如 `15dp`、`30dp`、特殊透明度色值，且项目没有等价资源时，才允许写裸值；完成说明中要列出无法映射原因。

## 文本与文案

- 字体权重优先使用 `@style/Text.Bold`、`@style/Text.SemiBold`、`@style/Text.Medium`、`@style/Text.Regular`。
- 字号层级优先使用 `@style/Text28`、`Text20`、`Text18`、`Text16`、`Text14`、`Text13`、`Text12`、`Text11`、`Text10`。
- 不要重复手写 `android:fontFamily`、`android:textStyle`、`android:letterSpacing`、`android:lineHeight`。
- 本地没有的字体不需要做兜底。不要在 XML 中写 `android:fontFamily="sans-serif-medium"`、`sans-serif`、`PingFang SC` 等系统/设计字体名来模拟 Figma；项目没有对应字体或 Text style 时，沿用项目默认字体，并在输出中说明字体无法精确映射。
- Figma 返回的普通行高和字距不要机械落到 XML；优先让项目 Text style 和系统默认排版处理。只有标题、按钮、品牌字或强视觉对齐处确实需要特殊值时，才局部添加 `letterSpacing` / `lineHeight`。
- 文案必须走 string 资源，新增 key 同步当前分支已有且业务要求覆盖的 `values*/strings.xml`。
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

新增资源先查是否已有可复用文件。确实需要新增时，遵守项目命名 skill：`android-naming`。

- 不透明纯色背景：`bg_颜色值_r/tr/br_radius值.xml`，例如 `bg_242435_r16.xml`、`bg_ffffff_r8.xml`。
- 透明色背景：`bg_透明百分比_颜色值_r/tr/br_radius值.xml`，例如 `bg_08_ffffff_r20.xml`；透明百分比沿用项目已有口径。
- 胶囊类背景统一 `r50`，例如 `bg_ffffff_r50.xml` 或 `bg_20_ffffff_r50.xml`。
- 描边背景先查项目已有同类命名，常见形式包括 `bg_<填充色>_stroke_<描边色>_rxx`、`bg_<填充色>_stroke_<透明百分比>_<描边色>_rxx`。
- 通用渐变沿用当前项目已有 `gradient_*` / `bg_gradient_*` 方向和值命名，新增前先查同类资源。
- 界面特有或功能语义强的资源：允许 `shape_bg_功能_xx.xml`，例如 `shape_bg_publish_task.xml`。
- 图标优先 `ic_xxx.xml` 或 `icon_xxx.xml`；图片优先 `img_xxx.webp/png`。
- selector 使用 `selector_xxx.xml`、`cb_xxx.xml`；按钮背景沿用项目已有 `btn_xxx.xml` 风格。
- 不新增 `bg_gray.xml`、`shape_1.xml`、`gradient_button.xml` 这类不可搜索泛名。

硬性要求：

- 新增 drawable 前先按候选文件名、颜色值、圆角值、描边值精确查询本地是否已有等价资源；有等价资源必须复用，不要新增。只有精确查询不足以判断时，才扩大到 `drawable*` 目录扫描。
- 纯色背景、透明色背景、纯色圆角、纯色描边背景不允许用页面语义命名，例如不要新增 `bg_creation_hot_recommend.xml`、`bg_xxx_card.xml` 这类文件；必须按颜色值/透明百分比/圆角/描边值命名，或复用已有 `bg_242435_r16.xml`、`bg_08_ffffff_r20.xml`、`fill_color1_r12.xml` 等资源。
- 只有复杂设计资产、图片、特殊渐变、组合光效或业务语义强且无法用值命名清晰表达的资源，才允许使用 `shape_bg_<功能>_<状态或角色>.xml` 这类语义名。

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

按本次变更范围执行，不做无关全量检查：

- 目标 Figma/截图信息是否足够；不足的部分是否说明。
- 本次触达片段的字体、文本色、背景色、间距、圆角是否优先映射项目资源。
- 本次新增/修改的 XML 行中，可映射的 `4dp/8dp/12dp/16dp/24dp` 间距和 `4dp/8dp/12dp/16dp` 圆角是否已使用 `@dimen/spacing_*` / `@dimen/radius_*`。
- 本次触达片段里的 `Text1` 等文本层级是否映射到 `?attr/textColor1` 等主题色。
- 本次新增/修改的 XML 行是否避免了非必要的裸 `dp`、裸 `#`、`android:fontFamily`、`android:textStyle`、`android:letterSpacing`、`android:lineHeight`。
- 本次新增 drawable 是否已按候选值查过等价资源，并符合值命名规则。
- 本次触达的图片/图标是否使用设计资产或项目已有资产。
- 目标 XML 是否符合 DataBinding/ViewBinding、`layout`/`item` 变量和已有 BindingAdapter 习惯。
