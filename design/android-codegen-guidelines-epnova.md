# Epnova Android Kotlin 代码生成规范

本文档用于指导在当前 Epnova 项目中生成 Android Kotlin 代码。它基于项目现有代码调整了通用 MVVM 说明，优先保持当前工程风格一致，避免引入不必要的新架构。

## 适用范围

- 主业务包名：`com.kuaigeglobal.epnova`
- 主业务工程：`app`
- 现有本地库模块：`apisecurity`、`datepicker`、`magicindicator`、`matisse`。新增业务代码仍优先放在 `app`，不要新增 Gradle Module。
- 现有架构：MVVM + Repository，基于 `nhsd.basic`
- UI 技术：XML + DataBinding/ViewBinding，当前项目已启用 DataBinding 和 ViewBinding
- 代码语言：Kotlin 为主，保留少量历史 Java
- 网络：Retrofit + `ApiResponse<T>`
- 数据库：Room，入口为 `core/db/AppDatabase.kt`
- 协程：优先使用项目已有 `launch`、`request`、`requestWithLoading`、`launchPolling`

## 与通用模板的关键差异

1. 不要把本项目当成纯 ViewBinding 小项目处理。页面 XML 已普遍使用 `<layout>`、`layout` 变量、`item` 变量和自定义 `BindingAdapter`。
2. 不要新增 `AppContainer`、`ViewModelFactory` 或 `di` 包。当前 `nhsd.basic` 已负责 `binding`、`mRealVM`、`mRepo`、`mBundle` 等注入。
3. 不要新增 Hilt 相关代码。禁止生成 `@Inject`、`@HiltViewModel`、`@AndroidEntryPoint`、`@Module`、`@InstallIn`、`@Provides`、`@Binds`。
4. 不要为了“标准 Clean Architecture”新增 `domain`、`UseCase`、`AppResult`、`AppError`。现阶段应沿用项目的 `ApiResponse<T>`、`response(...)` 和 `ApiException` 处理链。
5. 不要默认把接口实体再包装成 Domain Model / UiModel。项目中 `core/network/apis/entity` 下的模型已被 UI、ItemVM 和业务逻辑直接使用。只有页面显示字段明显复杂、需要组合多个来源或防止接口字段污染 UI 时，才新增轻量 UiModel/Mapper。

## 推荐目录规则

新增功能优先放在 `feature/<业务域>/` 下，和现有页面保持同一风格。

常用目录：

- `base/`：项目基础 ViewModel、ItemVM、RecyclerViewVM 等，不随意扩展。
- `common/`：通用 adapter、扩展函数、工具类、权限、LiveData 等。
- `core/network/apis/ApiService.kt`：Retrofit API 定义。
- `core/network/apis/request/`：请求体。
- `core/network/apis/response/`：接口响应模型，偏聚合数据。
- `core/network/apis/entity/`：接口实体模型，当前项目中也会被页面直接使用。
- `core/db/dao/`：Room Dao。
- `core/db/entity/`：Room Entity。
- `core/db/mapper/`：数据库模型转换。
- `core/data/CommonRepository.kt`：通用仓库基类。
- `core/manager/`：第三方 SDK、广告、支付、统计、登录等平台能力隔离。
- `router/`：页面路由和跳转。
- `widget/`：自定义 View、Dialog、状态组件。

不要新增多 Gradle Module，除非用户明确要求重构工程结构。

## Base 类使用规则

页面必须贴合项目已有基类：

- Activity 直接继承 `com.nhsd.basic.BaseViewModelActivity<VB, VM>`。
- Fragment 直接继承 `com.nhsd.basic.BaseViewModelFragment<VB, VM>`。
- 普通 ViewModel 继承 `AppViewModel`。
- 需要仓库的 ViewModel 继承 `AppRepositoryViewModel<T>`。
- `AppViewModel.id()` 和 `AppRepositoryViewModel.id()` 已固定返回 `BR.layout`，不要重复实现。
- 列表 item VM 继承 `AppItemViewModel<T>`，其 DataBinding 变量名固定为 `item`。
- 中间弹窗优先使用 `BaseVMDialogFragment<VB, VM>` 或 `BaseDialogFragment<VB>`。
- 底部弹窗优先使用 `BaseBottomPushVMDialogFragment<VB, VM>` 或 `BaseBottomPushDialogFragment<VB>`。

页面中直接使用基类注入对象：

- `binding`：页面 Binding。
- `mRealVM`：当前页面 ViewModel。
- `mRepo`：`AppRepositoryViewModel<T>` 对应 Repository。
- `mBundle`：参数 Bundle。

## UI 与 XML 规则

1. XML 默认使用 `<layout>` 根节点。
2. 页面 ViewModel 变量名必须是 `layout`。
3. 列表 item 变量名必须是 `item`。
4. 页面初始化优先放在 Fragment 的 `onPrepareView()` 中，并拆成 `initView()`、`initListener()`、`initObservers()` 或现有相近命名。
5. Activity 可以在 `onCreate()` 做启动初始化，但网络和业务动作仍应委托给 ViewModel。
6. 点击事件优先使用 `setOnSingleClickListener` 或 XML 中 `layout.clickHandler::onClick`，不要直接使用裸 `setOnClickListener`。
7. XML 可以写简单可读的 DataBinding 表达式，复杂业务判断放到 ViewModel 方法或 BindingAdapter。
8. 图片加载优先使用项目已有 `loadUri`、`loadHQThumbnailUri`、`loadThumbnailUri`、`loadVideoUrl` 等 BindingAdapter。
9. 文案必须走字符串资源，新增 key 只放到以下文件并检查重复：
   - `app/src/main/res/values/strings.xml`
   - `app/src/main/res/values-zh/strings.xml`
   - `app/src/main/res/values-th/strings.xml`
   - `app/src/main/res/values-in/strings.xml`

涉及 Figma、截图还原、页面/弹窗/卡片等可见 UI 时，按项目的“Figma UI 精确实现规范”执行，优先读取设计源并精确还原尺寸、颜色、字体、间距、圆角、阴影和资源。

### Figma UI 精确实现补充

- Figma 设计访问优先使用可用 MCP 工具，或使用 curl 调用 Figma REST API；不要用 WebFetch 读取 Figma URL。
- Android XML 实现时，先查 `styles.xml`、`attrs.xml`、`colors.xml`、`dimens.xml`、已有 drawable/selector 和 BindingAdapter。
- 优先映射到项目已有 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector`，不要使用 CSS/SCSS 变量等 Web 语境；如果设计 token 类似 Web/CSS token，需要翻译成 Android 项目资源。
- 字体尽量使用本地已有 `@style/Text.Bold`、`@style/Text.SemiBold`、`@style/Text.Medium`、`@style/Text.Regular`、`Text28/Text20/Text18/Text16/Text14` 等 style，不要重复手写 `fontFamily`、`textStyle`。
- 识别到 Figma 标注 `Text1`、`Text2`、`Text3`、`Text4`、`Text5` 等文本层级时，优先映射为项目实际主题色 `?attr/textColor1`、`?attr/textColor2`、`?attr/textColor3`、`?attr/textColor4`、`?attr/textColor5`。
- 涉及 TextView、Button、EditText、标签等文案控件时，宽高尽量不要写死；优先使用 `wrap_content`、`0dp` 约束、`minWidth/minHeight`、`maxWidth`、`maxLines`、`ellipsize`、padding/margin 来适配不同语言和文案长度。
- 仅当项目没有对应 style/theme/attr/resource 时，才使用局部精确值，并在输出中说明无法映射的原因。

## ViewModel 规则

1. ViewModel 负责页面状态、用户事件处理、调用 Repository、调度协程和处理页面业务。
2. ViewModel 不直接依赖 Activity、Fragment、View。
3. 简单网络请求可以沿用现有项目风格，通过 `mRepo.api.xxx(...)` 配合 `response(...)`、`request(...)` 或 `requestWithLoading(...)` 调用。
4. 当同一业务存在多处复用、包含 IO 切换、数据库组合、缓存策略或多接口组合时，在对应 feature 下新增 Repository 方法，由 ViewModel 调用 `mRepo.xxx(...)`。
5. 不直接创建 `ApiService` 或 `AppDatabase`，统一通过 `mRepo.api` 和 `mRepo.appDatabase`。
6. 页面状态优先使用 `MutableLiveData`、`ObservableField`、`ObservableBoolean`、`ObservableInt`，和现有 XML DataBinding 风格保持一致。
7. 已有 Paging、SSE、播放器状态等 Flow 场景可以使用 `StateFlow` / `Flow`。
8. Paging 使用 `Pager(...).flow.cachedIn(viewModelScope)`。
9. 轮询、SSE、下载、支付监听等长任务必须保存 `Job` 或 handler，并在 `onCleared()` 中取消。
10. 避免为了“不可变暴露”大规模改造已有 `MutableLiveData` 公有字段。新增复杂状态可以采用私有 `MutableLiveData` + 公开 `LiveData`，但以不破坏项目一致性为准。

## Repository 规则

1. 通用仓库继承 `AppRepository` 或 `CommonRepository`。
2. 功能仓库放在对应 feature 下，例如 `feature/series/SeriesRepository.kt`。
3. Repository 可以调用 `api`、`appDatabase`、MMKV、SharedPreferences、DataStore 或文件缓存。
4. Repository 返回类型优先保持 `ApiResponse<T>` 或项目已有业务模型，不为单个新功能单独引入 `AppResult<T>`。
5. 需要 IO 切换时，Repository 方法可以使用 `withContext(Dispatchers.IO)`。
6. API 错误处理优先交给 `response(...)`、`request(...)`、`requestWithLoading(...)`，不要在 Repository 中随意吞异常。
7. 只有当错误文案、重试策略、缓存降级等属于数据层职责时，才在 Repository 中封装。

## API 与模型规则

1. Retrofit 方法统一写在 `core/network/apis/ApiService.kt`。
2. API 路径常量写在 `ApiConstants.kt`。
3. 请求体写在 `core/network/apis/request/`。
4. 响应聚合模型写在 `core/network/apis/response/`。
5. 接口实体写在 `core/network/apis/entity/`。
6. Room Entity 写在 `core/db/entity/`。
7. Room Dao 写在 `core/db/dao/`，并在 `AppDatabase` 中注册。
8. 数据库模型和接口/UI 模型有差异时，在 `core/db/mapper/` 或 feature 内新增 mapper。
9. 不修改 `ApiResponse.java`，除非用户明确要求做全局网络层改造。

## 异步与错误处理

优先使用项目已有扩展：

- 无 loading 的网络请求：`request({ mRepo.api.xxx(body) }) { ... }`
- 用户触发且需要 loading 的请求：`requestWithLoading({ mRepo.api.xxx(body) }) { ... }`
- 自定义协程：`launch { ... }`
- 带 loading 的自定义协程：`launchWithLoading { ... }`
- 轮询：`launchPolling(...)`

错误处理原则：

- 接口失败默认通过 `response(...)` 的 error 分支或 `requestWithLoading(showToastError = true)` 处理。
- 需要自定义失败 UI 时使用 `onError` 回调。
- `CancellationException` 必须放行，不要吞掉协程取消。
- UI 只展示用户能理解的 Toast/Dialog/状态页，不展示原始堆栈。

## RecyclerView 与列表

1. 普通 DataBinding 列表优先使用 `QuickAdapter<ItemXxxVM>` + `AppItemViewModel<T>`。
2. item layout 变量固定为 `item`。
3. 简单列表更新使用 `submitList(newItems)`。
4. 只有强制全量刷新时才使用 adapter 的 `refresh()`。
5. 多类型列表可以使用 `BaseMultiItemAdapter<AppItemViewModel<*>>`，参考 `AssetManagementAdapter`、`StoryboardDesignAdapter`。
6. 分页列表使用 Paging3，按现有 `PagingSource`、`RemoteMediator`、`PagingDataAdapter` 风格实现。
7. Adapter 只负责渲染和转发点击事件，不直接请求网络。

## UseCase 与 MVI

默认不生成 UseCase，也不生成完整 MVI。

可以新增 UseCase 的条件：

- 同一业务逻辑被多个 ViewModel 复用。
- 需要组合多个 Repository。
- 规则计算明显复杂，放在 ViewModel 会膨胀。
- 需要独立测试核心业务逻辑。

可以新增轻量 UiState 的条件：

- 页面同时存在 loading、empty、error、content、editing、selecting 等多种状态。
- 页面事件很多，多个异步结果会交叉影响 UI。
- Flow/SSE/Paging/轮询混合后，单独 LiveData 难以维护。

不要生成 Store、Reducer、Intent、Effect 全套结构，除非用户明确要求。

## 第三方 SDK 隔离

广告、支付、推送、统计、登录、归因等能力必须隔离在 `core/manager/` 或对应 `core/google/`、`core/facebook/` 包下。

业务页面不要直接散落第三方 SDK 调用。已有模式：

- 广告平台接口：`core/manager/ad/IRewardedAdPlatform.kt`
- Unity 广告实现：`core/manager/ad/UnityRewardedAdPlatform.kt`
- 奖励广告统一入口：`core/manager/RewardedAdManager.kt`
- Google Billing：`core/google/billing/BillingManager.kt`
- 统计：`core/manager/EventManager.kt`

## 命名与代码风格

命名细则已抽成项目内 skill：[epnova-android-naming](skills/epnova-android-naming/SKILL.md)。生成或审查命名时优先使用该 skill，本节保留关键摘要和索引。

- Kotlin 缩进 4 空格。
- 优先安全调用和 Elvis 表达式，避免 `!!`。
- 单行长度尽量控制在 120 字符左右。
- 类名使用 `XxxActivity`、`XxxFragment`、`XxxViewModel`、`XxxRepository`、`XxxDialog`、`ItemXxxVM`。
- 常量放到 `companion object`，使用 upper snake case。
- Boolean 命名以 `is`、`has`、`can`、`should` 开头。
- 事件回调以 `onXxx` 命名。
- Intent extra、Bundle key、MMKV key、SharedPreferences key 必须抽成常量。
- 需要注释时使用中文注释，解释原因，不重复代码本身。
- 不检查、不修改 `OssManager.kt`，除非用户明确要求。

### 包名

- 包名全部小写，按业务域组织：`feature/series`、`feature/recharge`、`feature/creator/assets`。
- 新业务优先放在已有相近 feature 下，不为单页面新增过深目录。
- 数据子包可使用 `data`、`request`、`response`、`entity`、`mapper`。
- 不复制历史拼写错误包名。新增包避免 `manger` 这类误拼，除非必须兼容已有路径。

### Kotlin/Java 文件与类

- 文件名与主类名一致。
- Activity：`XxxActivity`，例如 `SeriesActivity`。
- Fragment：`XxxFragment`，例如 `HomeFragment`。
- ViewModel：`XxxViewModel`，例如 `SeriesViewModel`。
- Repository：`XxxRepository`，例如 `CreationRepository`。
- Dialog：`XxxDialog`、`XxxBottomSheet`，已有基类以 Dialog 命名时优先沿用 `XxxDialog`。
- Adapter：`XxxAdapter`，多类型列表也使用业务名，例如 `AssetManagementAdapter`。
- 列表 item VM：新代码统一使用 `ItemXxxVM`。已有历史文件出现 `ItemXxxVm` 时不强制重命名。
- Controller/Manager/Helper：仅在确实承担协调、平台封装、工具职责时使用，避免把普通业务逻辑都命名为 Manager。
- Java 只用于维护历史 Java 类或必须继承 Java 实现的场景；新增业务默认 Kotlin。

### 变量与属性

- 普通变量、函数、私有成员使用 lowerCamelCase。
- 私有属性不加 `m` 前缀；已有基类字段如 `mRealVM`、`mRepo`、`mBundle` 按框架保持原样。
- `LiveData`/`MutableLiveData` 名称使用业务含义，不加 `LiveData` 后缀，例如 `workInfo`、`loginTaskData`。
- `StateFlow`/`MutableStateFlow` 名称使用业务含义，必要时可加 `Flow` 表示连续流，例如 `feedFlow`。
- Adapter 变量使用 `xxxAdapter`，例如 `homeAdapter`、`publishedPlatformAdapter`。
- Job 变量使用 `xxxJob`，例如 `pollingJob`、`purchaseJob`。
- Handler/Processor/Manager 变量使用业务名，避免 `handler1`、`manager2`。
- Boolean 命名以 `is`、`has`、`can`、`should` 开头，例如 `isLoading`、`hasMore`、`canSubmit`、`shouldRefresh`。
- 列表集合使用复数或 `xxxList`，例如 `episodes`、`selectedIds`、`roleImageList`。

### 函数

- 函数名以动词或动宾结构开头：`loadData()`、`fetchUserInfo()`、`submitFeedback()`、`updateLoadState()`。
- 点击、回调、监听使用 `onXxx`：`onBackPressed()`、`onPaySuccess()`、`onItemSelected()`。
- 初始化函数按职责命名：`initView()`、`initListener()`、`initObservers()`、`initData()`。
- 网络/数据获取按语义区分：
  - `fetchXxx()`：刷新远端或主动请求。
  - `loadXxx()`：加载页面所需数据，可包含缓存。
  - `getXxx()`：轻量读取或已有项目同名接口。
  - `queryXxx()`：查询类接口。
  - `submitXxx()` / `saveXxx()` / `deleteXxx()`：用户提交类动作。
- 不使用无意义名称：`doSomething()`、`handleData()`、`process()`。除非作用域极小且含义明确。

### 常量与 Key

- 常量放到 `companion object`，使用 upper snake case。
- Intent extra / Bundle key：`KEY_XXX` 或复用 `Constants.Extra.XXX`。
- Request code：`REQUEST_CODE_XXX`。
- 默认值：`DEFAULT_XXX`。
- 分页大小：`PAGE_SIZE`、`INITIAL_LOAD_SIZE`。
- 时间间隔：`CLICK_INTERVAL_TIME`、`POLLING_INTERVAL_MS`。
- MMKV、SharedPreferences、缓存 key 必须抽成常量，禁止散落硬编码字符串。
- 对外协议字段、接口枚举值、事件 type 用具名常量，不直接写裸数字。

### API、请求体和模型

- Retrofit 方法名使用 lowerCamelCase，尽量贴近业务动作：`getUserHomeInfo()`、`seriesFollow()`、`createOrder()`。
- API 常量写在 `ApiConstants.kt`，命名使用 upper snake case，并与后端接口语义一致。
- 请求体命名为 `XxxBody`，例如 `WorkIdBody`、`SeriesSetPriceBody`。
- 分页请求体可命名为 `XxxPagedBody`。
- 接口聚合响应命名为 `XxxData`，例如 `HomeBookShelfData`、`LoginTaskData`。
- 接口实体命名为 `XxxInfo`，例如 `WorkInfo`、`EpisodeInfo`、`PaymentInfo`。
- Room Entity 命名为 `XxxEntity`，例如 `WorkInfoEntity`。
- Mapper 函数使用 `toXxx()`，例如 `toDomain()`、`toEntity()`、`toUiModel()`。
- 只有 UI 需要独立显示模型时才新增 `XxxUiModel`。

### UI 资源

- layout 文件：
  - Activity：`activity_xxx.xml`
  - Fragment：`fragment_xxx.xml`
  - Dialog：`dialog_xxx.xml`
  - RecyclerView item：`item_xxx.xml`
  - include 片段：`layout_xxx.xml` 或 `include_xxx.xml`
- drawable 文件优先沿用项目前缀：
  - 图标：`icon_xxx.xml` 或 `ic_xxx.xml`
  - 图片：`img_xxx.webp/png`
  - 纯色背景：`bg_<透明值>_<颜色值>_<圆角位置><圆角值>.xml`
  - 选择器：`selector_xxx.xml`、`cb_xxx.xml`
  - 渐变：`gradient_<透明值1>_<颜色值1>_<透明值2>_<颜色值2>_<方向>_<圆角位置><圆角值>.xml`
  - 圆形/圆角：`oval_xxx.xml`、`shape_xxx.xml`
  - 进度条：`progress_xxx.xml`
- drawable 细则：
  - 透明值使用两位小写 16 进制，不带 `#`，例如 `00`、`20`、`50`、`80`、`ff`。
  - 颜色值使用六位小写 16 进制，不带 `#`，例如 `ffffff`、`000000`、`121212`。
  - 圆角值只写 dp 数字，不带 `dp`，例如 `r8`、`r12`、`tr16`。
  - 胶囊类背景一律使用 `r50`，例如 `bg_ff_ffffff_r50.xml`。
  - `r<值>` 表示四角圆角，`tr<值>` 表示顶部圆角，`br<值>` 表示底部圆角。
  - 通用纯色背景优先用值命名，例如 `bg_20_ffffff_r8.xml`、`bg_80_121212_r16.xml`、`bg_ff_f5f5f5_tr24.xml`。
  - 新增通用渐变必须先写两个色值，再写方向和圆角，例如 `gradient_ff_fff1ea_ff_ffffff_top_r16.xml`、`gradient_ff_ff6159_ff_d91f1f_left_r16.xml`、`gradient_80_121212_80_1c1c2d_top_tr16.xml`。
  - 渐变方向可用 `top`、`bottom`、`left`、`right`、`ltop`、`rtop`、`lbottom`、`rbottom`；历史资源里已有方向前置命名时不强制重命名，新资源使用色值前置顺序。
  - 界面特有或功能语义强的资源可以用 `shape_bg_<功能>_<状态或角色>.xml`，例如 `shape_bg_publish_task.xml`、`shape_bg_story_upload.xml`。
  - 不使用 `bg_gray.xml`、`shape_1.xml`、`gradient_button.xml` 这类不可搜索的泛名。
- color 资源：
  - 通用透明度颜色沿用 `color_XX_xxxxxx`。
  - 业务语义颜色使用 `func_success_dark`、`text_color_3_dark` 这类语义命名。
- dimen 资源：
  - 通用间距优先复用 `spacing_xs/s/m/l`、`radius_s/m/l`。
  - 业务独有尺寸使用 `业务名_属性`，例如 `main_bottom_nav_height`。
- string key 使用语义名，不用页面位置名，例如 `feedback_submit_success` 优于 `text_button_1`。

### View id

- 当前项目 XML id 多使用 lowerCamelCase，新代码保持一致。
- 常用 View id 前缀：
  - `tv`：TextView
  - `iv`：ImageView
  - `btn`：按钮或可点击入口
  - `rv` / `recyclerView`：RecyclerView
  - `srl`：SmartRefreshLayout
  - `lsv`：LoadStateViewHolder
  - `et`：EditText
  - `ll` / `cl` / `fl`：LinearLayout / ConstraintLayout / FrameLayout
  - `vp`：ViewPager/ViewPager2
  - `tab`：自定义 tab 点击层
  - `rb`：RadioButton
- 同一页面内命名表达业务含义，例如 `btnSearch`、`ivCover`、`tvTitle`，不要使用 `view1`、`text2`。

### 状态、事件和枚举

- 轻量页面状态命名为 `XxxUiState`。
- 一次性事件命名为 `XxxEvent`，例如 `MyWorksEvent`。
- 如确实需要效果事件，命名为 `XxxEffect`，但默认不生成。
- 枚举类命名为 `XxxEnum` 或直接使用业务名；已有代码使用 `MainBulletFrameEnum`，新代码可沿用同类风格。
- 状态值使用明确名称：`Loading`、`Content`、`Empty`、`Error`，不要使用 `State1`、`Type2`。

## 生成前检查清单

生成代码前先判断：

1. 这个功能已有相似页面或仓库吗？优先复用同类写法。
2. 新页面是否应该继承 `BaseViewModelActivity` / `BaseViewModelFragment`？
3. XML 是否需要 `<layout>`，变量名是否为 `layout` 或 `item`？
4. API 是否应加到 `ApiService.kt`，请求体是否放在 `request/`？
5. 是否真的需要新 Repository，还是 `AppRepository` 已足够？
6. 是否真的需要 UiModel、UseCase、UiState？
7. 是否有长任务需要在 `onCleared()` 取消？
8. 是否新增了字符串资源并同步多语言文件？
9. 是否误引入了 Hilt、AppContainer、ViewModelFactory、AppResult/AppError 等当前项目没有采用的结构？

## 推荐生成模式

简单页面：

- `FeatureActivity/Fragment`
- `FeatureViewModel : AppRepositoryViewModel<AppRepository>()`
- XML `<layout>` + `layout` 变量
- 必要时新增 request body / response model / entity

中等复杂页面：

- `FeatureActivity/Fragment`
- `FeatureViewModel : AppRepositoryViewModel<FeatureRepository>()`
- `FeatureRepository : AppRepository()` 或 `CommonRepository()`
- ItemVM + QuickAdapter 或 Paging3
- 必要时增加轻量 UiState

复杂异步页面：

- 保持 MVVM
- 明确拆分 Repository、状态对象、轮询/SSE handler、进度模拟器
- Job 和 handler 在 `onCleared()` 统一释放
- 只在复用或业务规则复杂时增加 UseCase
