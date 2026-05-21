---
name: epnova-android-naming
description: "Epnova Android Kotlin 命名规范。用户提到命名、命名规则、resource naming、资源命名、drawable naming、id naming、string key、class naming、package naming，或需要生成/审查包名、类名、ViewModel、Repository、API 模型、资源文件、View id、状态、事件、常量、函数、layout、drawable、string、dimen、attr、Retrofit API、请求体、响应/实体模型、Adapter、key 时使用。"
---

# Epnova Android 命名规范

## 触发条件

当任务包含 Epnova Android 项目的命名决策或命名审查时使用本 skill：

- 用户提到 naming、命名、命名规则、资源命名、drawable 命名、id 命名、string key、包名、类名、方法名、字段名或常量名。
- 生成或审查包名、类名、函数名、属性名、ViewModel、Repository、Adapter、item VM、UiState、事件、effect、常量、Intent extra、Bundle key、MMKV key、SharedPreferences key。
- 新增或审查 Android 资源名：layout、drawable、selector、gradient、string、dimen、attr、style、color、View id 和 BindingAdapter 属性名。
- 新增或审查 Retrofit API 名、request body 名、response model 名、entity 名、Room entity 名、DAO 名、mapper 名或 UiModel 名。
- 按 Figma 或 XML 实现 UI 时，需要新增资源名或 View id，并要求与项目命名一致。

## 核心规则

优先遵循当前 Epnova 项目风格。已有本地模式时，不引入通用 Android 模板式命名。

默认使用 Kotlin。只有编辑历史 Java 文件时，才沿用 Java 相关命名。

## 包名

- 包名使用小写。
- 新业务代码放到最接近的现有 `feature/<domain>` 包下。
- 只有子包职责明确时，才使用 `data`、`request`、`response`、`entity`、`mapper`。
- 避免为单个页面创建过深的包层级。
- 除非兼容历史代码必须如此，否则不要把 `manger` 这类历史拼写错误复制到新包名中。

## 类名

- Activity：`XxxActivity`
- Fragment：`XxxFragment`
- ViewModel：`XxxViewModel`
- Repository：`XxxRepository`
- Dialog：`XxxDialog`
- Adapter：`XxxAdapter`
- Item ViewModel：`ItemXxxVM`
- UI 状态：`XxxUiState`
- 事件：`XxxEvent`
- Effect：`XxxEffect`，仅在明确需要时使用
- 枚举：`XxxEnum`，或使用已有业务命名，例如 `MainBulletFrameEnum`

优先使用具体业务名：

- 推荐：`SeriesViewModel`、`AssetManagementRepository`、`ItemRechargeCreditsVM`
- 避免：`CommonViewModel2`、`DataManager`、`ListItem`、`MyAdapter`

只有用于协调、平台隔离或工具职责时，才使用 `Manager`、`Controller`、`Helper`。

## 变量与属性

- 变量、属性和函数使用 lowerCamelCase。
- 新代码不要给私有字段添加 `m` 前缀。框架已有字段如 `mRealVM`、`mRepo`、`mBundle` 保持不变。
- `LiveData` 按业务含义命名，不按类型命名：`workInfo`、`loginTaskData`、`episodeInfo`。
- Flow 按业务含义命名；只有有助于表达连续流时才加 `Flow`：`feedFlow`、`currentWaterfallId`。
- Adapter 变量：`xxxAdapter`。
- Job 变量：`xxxJob`。
- Boolean 命名以 `is`、`has`、`can` 或 `should` 开头。
- 集合命名使用复数或 `xxxList`：`episodes`、`selectedIds`、`roleImageList`。

## 函数

- 函数名以动词开头：`loadData()`、`fetchUserInfo()`、`submitFeedback()`、`updateLoadState()`。
- 回调使用 `onXxx`：`onPaySuccess()`、`onItemSelected()`。
- 初始化使用项目常见命名：`initView()`、`initListener()`、`initObservers()`、`initData()`。
- 主动远程刷新使用 `fetchXxx()`。
- 页面数据加载使用 `loadXxx()`，可以包含缓存。
- 轻量读取或沿用已有 API 命名时使用 `getXxx()`。
- 查询类接口使用 `queryXxx()`。
- 用户触发的变更操作使用 `submitXxx()`、`saveXxx()`、`deleteXxx()`。

除非局部作用域非常小且含义明确，否则避免 `doSomething()`、`handleData()`、`process()` 这类泛名。

## 常量与 Key

- 常量默认放在 `companion object`，除非已有共享 `Constants` 位置。
- 使用 upper snake case。
- Intent/Bundle key：`KEY_XXX` 或 `Constants.Extra.XXX`。
- 请求码：`REQUEST_CODE_XXX`。
- 默认值：`DEFAULT_XXX`。
- 分页大小：`PAGE_SIZE`、`INITIAL_LOAD_SIZE`。
- 时间间隔：`CLICK_INTERVAL_TIME`、`POLLING_INTERVAL_MS`。
- MMKV、SharedPreferences、缓存 key、协议字段、事件类型和接口枚举值都要抽成具名常量。

## API 与模型

- Retrofit 方法名使用 lowerCamelCase，并匹配业务动作：`getUserHomeInfo()`、`seriesFollow()`、`createOrder()`。
- `ApiConstants.kt` 中的 API 常量使用 upper snake case。
- 请求体：`XxxBody`，例如 `WorkIdBody`。
- 分页请求体：`XxxPagedBody`。
- 聚合响应模型：`XxxData`，例如 `HomeBookShelfData`。
- API 实体：`XxxInfo`，例如 `WorkInfo`、`EpisodeInfo`、`PaymentInfo`。
- Room 实体：`XxxEntity`。
- Mapper 函数：`toXxx()`，例如 `toDomain()`、`toEntity()`、`toUiModel()`。
- 只有 UI 需要单独展示模型时，才新增 `XxxUiModel`。

## 资源文件

Layout 文件：

- Activity：`activity_xxx.xml`
- Fragment：`fragment_xxx.xml`
- Dialog：`dialog_xxx.xml`
- RecyclerView item：`item_xxx.xml`
- Include fragment：`layout_xxx.xml` 或 `include_xxx.xml`

Drawable 文件：

- 图标：`icon_xxx.xml` 或 `ic_xxx.xml`
- 图片：`img_xxx.webp` 或 `img_xxx.png`
- 纯色背景：`bg_<alpha>_<color>_<corner><radius>.xml`
- Selector：`selector_xxx.xml` 或 `cb_xxx.xml`
- 渐变：`gradient_<alpha1>_<color1>_<alpha2>_<color2>_<direction>_<corner><radius>.xml`
- Oval/shape：`oval_xxx.xml` 或 `shape_xxx.xml`
- Progress：`progress_xxx.xml`

Drawable token 规则：

- `<alpha>` 使用两位小写 16 进制，不带 `#`：`00`、`10`、`20`、`50`、`80`、`ff`。
- `<color>` 使用六位小写 16 进制，不带 `#`：`ffffff`、`000000`、`121212`。
- `<radius>` 只写 dp 数值，不带 `dp`：`r8`、`r12`、`tr16`。
- 胶囊类背景一律使用 `r50`，例如 `bg_ff_ffffff_r50.xml`。
- 四角圆角使用 `r<radius>`。
- 仅顶部圆角使用 `tr<radius>`。
- 仅底部圆角使用 `br<radius>`。
- 通用纯色背景优先使用精确值命名，不使用语义名：
  - `bg_20_ffffff_r8.xml`
  - `bg_80_121212_r16.xml`
  - `bg_ff_f5f5f5_tr24.xml`
- 新增渐变资源先写两个色值，再写方向和圆角：
  - `gradient_ff_fff1ea_ff_ffffff_top_r16.xml`
  - `gradient_ff_ff6159_ff_d91f1f_left_r16.xml`
  - `gradient_80_121212_80_1c1c2d_top_tr16.xml`
- 支持的方向标记：`top`、`bottom`、`left`、`right`、`ltop`、`rtop`、`lbottom`、`rbottom`。编辑历史文件时保留已有方向前置命名；新增文件使用色值前置顺序。
- 如果 drawable 明显属于某个界面或功能，可以用 `shape_bg_<feature>_<state_or_role>.xml` 表达业务语义，不必强行编码所有数值，例如 `shape_bg_publish_task.xml`、`shape_bg_story_upload.xml`、`shape_bg_work_popularity.xml`。
- 能用色值或业务角色命名时，不使用 `bg_gray.xml`、`shape_1.xml`、`gradient_button.xml` 这类泛名。

其他资源：

- 优先复用通用 dimen，例如 `spacing_xs`、`spacing_s`、`spacing_m`、`spacing_l`、`radius_s`、`radius_m`、`radius_l`。
- 业务专用 dimen 使用 `business_property`，例如 `main_bottom_nav_height`。
- String key 使用语义名，例如 `feedback_submit_success`。
- 避免 `text_button_1` 这类位置型命名。

## View ID

View id 使用 lowerCamelCase，并与现有 XML 风格保持一致。

常用前缀：

- `tv`: TextView
- `iv`: ImageView
- `btn`: button or clickable entrance
- `rv` or `recyclerView`: RecyclerView
- `srl`: SmartRefreshLayout
- `lsv`: LoadStateViewHolder
- `et`: EditText
- `ll`, `cl`, `fl`: LinearLayout, ConstraintLayout, FrameLayout
- `vp`: ViewPager or ViewPager2
- `tab`: custom tab click layer
- `rb`: RadioButton

优先表达业务含义：`btnSearch`、`ivCover`、`tvTitle`。

避免 `view1`、`text2`、`button3` 这类位置型命名。

## 审查清单

生成代码结束前检查：

- 命名符合附近已有 feature 模式。
- 没有为了命名一致性引入新的 Hilt、`AppContainer`、`ViewModelFactory`、`AppResult` 或 `AppError` 名称。
- API、请求体、响应、实体和 mapper 名称位于预期包下。
- XML layout 变量保持页面用 `layout`、列表 item 用 `item`。
- 新 string、drawable、layout 和 id 名称有语义、可搜索。
