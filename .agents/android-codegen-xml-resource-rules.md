# Android XML 与资源规则

快速入口见 `android-codegen-quick-rules.md`。只有涉及 XML/UI、资源 token、drawable 命名或 Figma 值映射时读取本文件。

## XML 与 UI

- 新增页面 DataBinding XML 默认使用 `<layout>` 根节点，页面 ViewModel 变量名默认 `layout`。
- 新增列表 item DataBinding XML 默认变量名 `item`。
- 简单业务弹窗不为了 DataBinding 表达式新增 `dialog`、接口实体等变量；优先在 Dialog 内直接更新视图和点击事件。若存在多处派生展示状态、样式分支、选中态、参数规整或复用状态，使用 VM 弹窗基类，XML 只绑定 `layout`。VM 可以暴露源对象和少量选择状态；简单颜色、背景、显隐用 XML 三元表达式表达，不要把每个样式值都拆成 Observable，也不要在 XML `<data>` 里单独声明接口实体或 Dialog 变量来承载展示。
- 编辑历史 XML、include、纯 ViewBinding 或无绑定 layout 时，跟随当前文件和同类文件，不强行改结构。
- 页面初始化优先放在 Fragment 的 `onPrepareView()` 中，并拆成 `initView()`、`initListener()`、`initObservers()` 或现有相近命名。
- 与 `onPrepareView()` 成对的手动回调清理默认放在 Fragment 的 `onDestroy()`，例如 ViewPager2 callback、AdapterDataObserver、键盘监听和事件监听；不要默认生成 `onDestroyView()` 清理，除非正在修改的同类页面或基类已经这样做。
- Activity 可以在 `onCreate()` 做启动初始化，但网络和业务动作仍应委托给 ViewModel。
- 保存、提交、支付、生成、跳转、登录等用户高频动作优先使用 `setOnSingleClickListener` 或 XML 中 `layout.clickHandler::onClick`。简单关闭、toggle、播放器点击、adapter retry、widget 内部回调、Java 旧控件或同类代码已采用的简单监听，可以沿用 `setOnClickListener`。
- XML 可以写简单可读的 DataBinding 表达式，复杂业务判断放到 ViewModel 方法或 BindingAdapter。
- 图片加载优先使用项目已有 `loadUri`、`loadHQThumbnailUri`、`loadThumbnailUri`、`loadVideoUrl` 等 BindingAdapter。
- 文案必须走字符串资源；新增 key 前先检查重复，并同步当前仓库已有且业务要求覆盖的 `values*/strings.xml`。

## 执行边界

- 快速局部修改：只读取目标文件、直接相关代码和必要资源；检查只覆盖触达文件。
- 标准代码生成：先读 `android-codegen-quick-rules.md` 和命中场景 skill；不读取未命中的 skill。
- Figma UI 生成或还原：只在用户明确要求按 Figma/截图实现或校准可见 UI 时启用。优先读取目标 Figma 节点和目标 XML，不读取整个 Figma 文件。
- 资源查询采用按需策略：先按候选名称、颜色值、圆角、描边或同类 XML 精确查询；只有新增 drawable、资源缺失或命名冲突时，才扩大到 `drawable*` 目录扫描。
- token 检查只覆盖本次新增或修改的 XML 行和直接相关资源；不要顺手清理整个历史文件，除非用户明确要求。
- 用户明确要求“不走 Gradle”“直接检查”时，只做 XML 解析、资源引用存在性、关键 `rg` 检查等静态验证。

## Resource Token

- 当设计值与项目已有 `@dimen`、`?attr`、`@color`、`@style`、`@drawable` 完全匹配时，必须使用项目资源引用，不要在 XML 中直接写裸值。
- 精确模式不等于全部写死数值；优先级是“先精确匹配项目 token，再用局部精确值兜底”。
- 只有项目没有对应资源，或设计值与现有 token 不一致时，才允许在当前 XML 或新增资源中写设计稿原始值。
- 常见间距必须优先映射：`4dp -> @dimen/spacing_xs`、`8dp -> @dimen/spacing_s`、`12dp -> @dimen/spacing_m`、`16dp -> @dimen/spacing_l`、`24dp -> @dimen/spacing_xl`。
- 常见圆角必须优先映射：`4dp -> @dimen/radius_s`、`8dp -> @dimen/radius_m`、`12dp -> @dimen/radius_l`、`16dp -> @dimen/radius_xl`。
- 识别到 Figma 文本层级、样式名或语义为 `Text1`、`Text2`、`Text3`、`Text4`、`Text5` 时，文本颜色必须优先使用 `?attr/textColor1`、`?attr/textColor2`、`?attr/textColor3`、`?attr/textColor4`、`?attr/textColor5`。
- 识别到主题色、背景色、填充色、功能色语义时，优先使用 `?attr/themeColor*`、`?attr/bgColor*`、`?attr/fillColor*`、`?attr/funcColor*`；不要直接写 `#FFFFFF`、`#121212`、`#F5FFFFFF` 等字面量，除非没有对应 token。
- 字号、字重优先使用项目 Text style；不要为了贴 Figma 重复手写 `android:fontFamily`、`android:textStyle`、`android:textSize`。
- 本地没有的字体不需要做系统字体兜底。禁止为了模拟字重而写 `android:fontFamily="sans-serif-medium"`、`sans-serif`、`PingFang SC` 等临时字体名。
- 默认不要在每个 TextView/Button 上手写 `android:letterSpacing`、`android:lineHeight`。只有设计稿明确要求特殊字距/行高，且项目 Text style 没有对应层级、实际 UI 不加会明显偏离时才允许局部添加。
- 新增或修改 XML 后，只检查本次触达片段中的裸 `dp`、裸 `#` 色值和手写字体属性；能映射项目资源的必须替换为项目资源引用，非必要的 `letterSpacing`、`lineHeight` 必须移除。

## 命名与资源

- Kotlin 缩进 4 空格。
- 优先安全调用和 Elvis 表达式，避免 `!!`。
- 单行长度尽量控制在 120 字符左右。
- 类名使用 `XxxActivity`、`XxxFragment`、`XxxViewModel`、`XxxRepository`、`XxxDialog`、`ItemXxxVM`。
- 常量放到 `companion object`，使用 upper snake case。
- Boolean 命名以 `is`、`has`、`can`、`should` 开头。
- 事件回调以 `onXxx` 命名。
- 跨类传递、复用、公开入口、Activity result、MMKV 和 SharedPreferences 的 key 必须抽成常量。
- 单个私有 Dialog/Fragment 的局部参数可使用当前类 `private const val KEY_XXX`；编辑历史直接字符串时不为规范单独重构，除非本次触达或存在拼写风险。
- Job 变量使用 `xxxJob`。
- layout 文件使用 `activity_xxx.xml`、`fragment_xxx.xml`、`dialog_xxx.xml`、`item_xxx.xml`。
- 不透明纯色背景使用 `bg_<颜色值>_<圆角位置><圆角值>.xml`，例如 `bg_242435_r16.xml`、`bg_ffffff_r8.xml`。
- 透明色背景使用 `bg_<透明百分比>_<颜色值>_<圆角位置><圆角值>.xml`，例如 `bg_08_ffffff_r20.xml`；透明百分比沿用项目已有口径，不按十六进制 alpha 强行换算。
- 胶囊类背景一律使用 `r50`。
- 描边背景先查项目已有同类命名，常见形式包括 `bg_<填充色>_stroke_<描边色>_<圆角>.xml`、`bg_<填充色>_stroke_<透明百分比>_<描边色>_<圆角>.xml`。
- 通用渐变沿用当前项目已有方向和值命名，新增前先查同类 `gradient_*` / `bg_gradient_*` 资源，不强制重命名历史风格。
- 界面特有或功能语义强的资源可以用 `shape_bg_<功能>_<状态或角色>.xml`。
- 只有新增命名、资源命名冲突、命名审查或用户明确要求时才读取 `android-naming` skill；普通局部改动按本节摘要和现有同类命名处理。
