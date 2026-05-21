---
name: epnova-android-feature-scaffold
description: "Epnova Android 页面骨架生成规范。用户提到 scaffold、feature scaffold、页面骨架、新页面、新建页面、生成页面、Activity、Fragment、ViewModel、XML 布局、Toolbar、参数传递、start method、newInstance、mBundle、BaseViewModelActivity、BaseViewModelFragment，或要求新增/审查 Android 功能页面基础结构时使用。"
---

# Epnova Android 页面骨架

## 触发条件

创建或审查 Epnova Android 功能页面基础结构时使用本 skill：

- 用户提到 scaffold、feature scaffold、page scaffold、页面骨架、新页面、新建页面、生成页面、新 Activity、新 Fragment、ViewModel、XML 布局、Toolbar、参数传递或基础页面。
- 生成或审查 `Activity + ViewModel + XML`、`Fragment + ViewModel + XML`、`ToolbarViewModel`、`companion object start(...)`、`newInstance(...)`、`mBundle` 参数读取或页面初始化方法。
- 新增可见 Android 页面、Tab 页面、嵌套 Fragment、表单页、详情页、选择页或简单设置/账号页面。
- 判断应使用的 Base 类、包路径、XML layout 变量、点击绑定或页面初始化模式。

如果页面是分页列表，同时使用 `epnova-android-paging-state-ui`。如果任务主要是命名，同时使用 `epnova-android-naming`。

## 核心规则

生成能满足当前功能的最小项目原生 MVVM 页面骨架。

使用当前 Epnova 项目模式，不使用通用 Android 模板：

- Activity 使用 `BaseViewModelActivity<VB, VM>`。
- Fragment 使用 `BaseViewModelFragment<VB, VM>`。
- 页面需要 `mRepo`、API、数据库、`mBundle` 或项目仓库能力时，使用 `AppRepositoryViewModel<AppRepository>`。
- 只有极小页面且不需要仓库或参数时，才使用 `AppViewModel`。
- XML 使用 `<layout>` 根节点，页面变量名为 `layout`。
- 页面需要标准标题/返回栏时，Toolbar 使用 `ToolbarViewModel` 和 `@layout/view_model_toolbar`。

普通页面骨架不要引入 Hilt、AppContainer、ViewModelFactory、AppResult/AppError、UseCase、完整 MVI 或新的 Base 类。

## 生成前判断

1. 先在相同 `feature/<domain>` 包下搜索附近已有页面。
2. 明确选择 Activity、Fragment 或 Dialog：
   - Activity：独立全屏入口、外部启动、需要返回结果或作为导航目标。
   - Fragment：Tab 页、子页面、ViewPager 页面或由现有 Activity 承载的页面。
   - Dialog：弹窗 UI，使用项目弹窗专项规则，不使用本页面骨架规则。
3. 判断页面是否需要仓库访问。需要时使用 `AppRepositoryViewModel<AppRepository>` 或已有 feature Repository。
4. 只新增当前确实需要的文件。除非功能已经需要，否则不要创建 Repository、UseCase、Adapter、UiState 或 mapper。
5. 包名、类名、layout、drawable、string 和 View id 使用 `epnova-android-naming`。

## 文件放置

新业务页面放在最接近的现有包下：

```text
app/src/main/java/com/kuaigeglobal/epnova/feature/<domain>/
```

Typical files:

- Activity 页面：`XxxActivity.kt`、`XxxViewModel.kt`、`activity_xxx.xml`。
- Fragment 页面：`XxxFragment.kt`、`XxxViewModel.kt`、`fragment_xxx.xml`。
- 只有需要列表时才增加：`XxxAdapter.kt`、`ItemXxxVM.kt`、`item_xxx.xml`、`XxxEvent.kt`。
- 只有现有 `AppRepository` 不足时才增加 feature Repository：`XxxRepository.kt`。

字符串资源放入项目允许的 string 文件，不要在 XML 或 Kotlin 中硬编码面向用户的文案。

## Activity 模式

独立页面和需要返回结果的页面使用 Activity。

```kotlin
class XxxActivity : BaseViewModelActivity<ActivityXxxBinding, XxxViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context?, workId: String?) {
            if (context == null) return
            val starter = Intent(context, XxxActivity::class.java)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initListener()
        dataUpdate()
    }

    private fun initView() {
    }

    private fun initListener() {
    }

    private fun dataUpdate() {
    }
}
```

如果需要返回结果，参考附近选择页补充 `getIntent(...)` 和 `setResult(...)`。网络和业务逻辑仍放在 ViewModel。

## Fragment 模式

Tab 页、子页面以及由其他页面承载的页面使用 Fragment。

```kotlin
class XxxFragment : BaseViewModelFragment<FragmentXxxBinding, XxxViewModel>() {

    companion object {
        fun newInstance(workId: String?): XxxFragment {
            return XxxFragment().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.WORK_ID, workId)
                }
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initListener()
        dataUpdate()
    }

    private fun initView() {
    }

    private fun initListener() {
    }

    private fun dataUpdate() {
    }
}
```

Fragment 收集协程数据时，优先使用 `viewLifecycleOwner.lifecycleScope` 和 `viewLifecycleOwner.repeatOnLifecycle(...)`，除非附近代码已有更强的局部模式。

## ViewModel 模式

标准 Toolbar 使用 `ToolbarViewModel`，参数从 `mBundle` 读取。

```kotlin
class XxxViewModel : AppRepositoryViewModel<AppRepository>() {

    val toolbar = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.xxx_title.getResString()
    )

    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }

    override fun onCreate() {
        super.onCreate()
        loadData()
    }

    private fun loadData() {
    }
}
```

使用与附近代码一致的项目状态类型：`MutableLiveData`、`ObservableField`、`ObservableBoolean`、`ObservableInt`。只有功能已经需要分页、SSE、轮询或连续流时，才使用 `Flow`。

Long-running jobs, polling, SSE handlers, downloads, and media tasks must be stored and cancelled in `onCleared()`.

## XML 模式

Page XML should use DataBinding:

```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <variable
            name="layout"
            type="com.kuaigeglobal.epnova.feature.xxx.XxxViewModel" />
    </data>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <include
            android:id="@+id/toolbar"
            layout="@layout/view_model_toolbar"
            app:toolbar="@{layout.toolbar}" />

    </LinearLayout>
</layout>
```

优先使用项目已有 style 和 attr：`@style/Text14`、`@style/Text16`、`@style/Button.Bold`、`?attr/textColor1`、`?attr/textColor4`、`@dimen/spacing_l` 和已有 drawable 资源。

Click handling:

- Kotlin 命令式绑定优先使用 `setOnSingleClickListener`。
- 页面通过 `onClick(view, data)` 集中处理点击时，XML 使用 `singleClick="@{layout.clickHandler::onClick}"`。
- 普通用户操作不要直接使用裸 `setOnClickListener`。

## 观察与事件

Activity 可以使用 `observe(this)`。

Fragment 新代码优先使用 view 生命周期：

```kotlin
mRealVM.xxxData.observe(viewLifecycleOwner) { data ->
}
```

`observeEvents { ... }` 和 `FlowEventBus` 仅用于跨页面应用事件。父子页面通信如果可以用直接回调或附近已有 shared ViewModel 处理，不要使用事件总线。

## 审查清单

页面骨架完成前检查：

- 文件位于最接近的现有 `feature/<domain>` 包下。
- 类名和 XML 命名符合 `epnova-android-naming`。
- Activity extends `BaseViewModelActivity<VB, VM>` or Fragment extends `BaseViewModelFragment<VB, VM>`.
- XML root is `<layout>` and page variable is `layout`.
- Standard toolbar uses `ToolbarViewModel` and `view_model_toolbar`.
- Arguments use `Constants.Extra.*` and are read from `mBundle` in ViewModel when practical.
- Activity/Fragment 只初始化 View、监听、观察者、导航、Toast/Dialog/loading，不直接请求网络或访问数据库。
- ViewModel owns page state and request calls.
- 没有在缺乏真实需要时新增 Base 类、Hilt、AppContainer、ViewModelFactory、UseCase、AppResult/AppError 或完整 MVI。
- Long tasks are cancelled in `onCleared()`.
