---
name: android-feature-scaffold
description: "Android 页面骨架生成规范。仅在本次实际新增页面、创建 Activity/Fragment/ViewModel/XML 骨架、重构页面基础结构，或判断 Base 类、包路径、start/newInstance、mBundle 模式时使用。修改已有页面的局部逻辑、局部 XML、字段绑定、Toast、点击事件或状态同步时不要触发。"
---

# Android 页面骨架

## 触发条件

创建或审查 Android 功能页面基础结构时使用本 skill：

- 用户明确要求 scaffold、feature scaffold、page scaffold、页面骨架、新页面、新建页面、生成页面、新 Activity、新 Fragment 或基础页面结构。
- 生成或审查 `Activity + ViewModel + XML`、`Fragment + ViewModel + XML`、`ToolbarViewModel`、`companion object start(...)`、`newInstance(...)`、`mBundle` 参数读取或页面初始化方法。
- 新增可见 Android 页面、Tab 页面、嵌套 Fragment、表单页、详情页、选择页或简单设置/账号页面。
- 判断应使用的 Base 类、包路径、XML layout 变量、点击绑定或页面初始化模式。

只修改已有页面的局部逻辑、局部 XML、字段绑定、Toast、点击事件、状态同步或资源引用时，不使用本 skill。

如果本次实际新增或修改分页链路，再读取 `android-paging-state-ui`。只有命名不确定、批量新增资源或用户要求命名审查时，才读取 `android-naming`。

## 核心规则

生成能满足当前功能的最小项目原生 MVVM 页面骨架。

使用当前项目模式，不使用通用 Android 模板：

- Activity 使用 `BaseViewModelActivity<VB, VM>`。
- Fragment 使用 `BaseViewModelFragment<VB, VM>`。
- 页面需要 `mRepo`、API、数据库或项目仓库能力时，使用 `AppRepositoryViewModel<AppRepository>` 或已有 feature Repository。
- 只需要页面状态、Toolbar、点击事件或 `mBundle` 参数读取时，优先使用 `AppViewModel`，并参考当前 feature 附近同类页面。
- XML 使用 `<layout>` 根节点，页面变量名为 `layout`。
- 页面需要标准标题/返回栏时，Toolbar 使用 `ToolbarViewModel` 和 `@layout/view_model_toolbar`。

普通页面骨架不要引入 Hilt、AppContainer、ViewModelFactory、AppResult/AppError、UseCase、完整 MVI 或新的 Base 类。

## 生成前判断

1. 先在相同 `feature/<domain>` 包下搜索附近已有页面。
2. 明确选择 Activity、Fragment 或 Dialog：
   - Activity：独立全屏入口、外部启动、需要返回结果或作为导航目标。
   - Fragment：Tab 页、子页面、ViewPager 页面或由现有 Activity 承载的页面。
   - Dialog：弹窗 UI，使用项目弹窗专项规则，不使用本页面骨架规则。
3. 判断页面是否需要仓库访问。需要时使用 `AppRepositoryViewModel<AppRepository>` 或已有 feature Repository；仅有 `mBundle` 参数不算仓库访问。
4. 只新增当前确实需要的文件。除非功能已经需要，否则不要创建 Repository、UseCase、Adapter、UiState 或 mapper。
5. 命名优先沿用当前 feature 和同类文件；只有命名冲突、不确定命名、批量新增资源或用户要求命名审查时，才读取 `android-naming`。

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

本项目 `onPrepareView()` 不是普通模板里的 `onViewCreated()`。在 `onPrepareView()` 中注册 ViewPager2 callback、AdapterDataObserver、键盘监听、事件监听等手动回调时，默认在 `onDestroy()` 中解除并清空引用，参考 `HomeFragment`、`WorksFragment`、`CreationTabFragment` 等同类写法；不要默认生成 `onDestroyView()`，除非正在修改的同类页面或基类已经这样做。

Fragment 收集协程数据时，可以使用 `viewLifecycleOwner.lifecycleScope` 和 `viewLifecycleOwner.repeatOnLifecycle(...)`，除非附近代码已有更强的局部模式。这个规则只约束 LiveData/Flow 观察，不改变 `onPrepareView()` 手动注册回调与 `onDestroy()` 的配对。

## ViewModel 模式

标准 Toolbar 使用 `ToolbarViewModel`，参数从 `mBundle` 读取。只读参数和维护本地页面状态时，可以使用 `AppViewModel`。

```kotlin
class XxxViewModel : AppViewModel() {

    val toolbar = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.xxx_title.getResString()
    )

    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
}
```

需要接口、数据库或仓库能力时，再使用 `AppRepositoryViewModel`：

```kotlin
class XxxViewModel : AppRepositoryViewModel<AppRepository>() {

    val toolbar = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.xxx_title.getResString()
    )

    override fun onCreate() {
        super.onCreate()
        loadData()
    }

    private fun loadData() {}
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

- 保存、提交、支付、生成、跳转、登录等用户高频动作，Kotlin 命令式绑定优先使用 `setOnSingleClickListener`。
- 页面通过 `onClick(view, data)` 集中处理点击时，XML 使用 `singleClick="@{layout.clickHandler::onClick}"`。
- 简单关闭、toggle、播放器点击、adapter retry、widget 内部回调、Java 旧控件或同类代码已采用的简单监听，可以沿用 `setOnClickListener`。

## 观察与事件

Activity 可以使用 `observe(this)`。

Fragment 观察 LiveData/Flow 时可以使用 `viewLifecycleOwner`：

```kotlin
mRealVM.xxxData.observe(viewLifecycleOwner) { data ->
}
```

不要因为使用 `viewLifecycleOwner` 观察数据，就把 `onPrepareView()` 中的手动注册清理挪到 `onDestroyView()`；本项目默认仍按 `onDestroy()` 清理。

`observeEvents { ... }` 和 `FlowEventBus` 仅用于跨页面应用事件。父子页面通信如果可以用直接回调或附近已有 shared ViewModel 处理，不要使用事件总线。

## 审查清单

页面骨架完成前检查：

- 文件位于最接近的现有 `feature/<domain>` 包下。
- 类名和 XML 命名符合 `android-naming`。
- Activity extends `BaseViewModelActivity<VB, VM>` or Fragment extends `BaseViewModelFragment<VB, VM>`.
- XML root is `<layout>` and page variable is `layout`.
- Standard toolbar uses `ToolbarViewModel` and `view_model_toolbar`.
- Cross-page arguments preferably use `Constants.Extra.*` or local `private const val KEY_XXX`; read from `mBundle` in ViewModel when practical. Do not refactor unrelated legacy keys just for style.
- Activity/Fragment 只初始化 View、监听、观察者、导航、Toast/Dialog/loading，不直接请求网络或访问数据库。
- ViewModel owns page state and request calls.
- 没有在缺乏真实需要时新增 Base 类、Hilt、AppContainer、ViewModelFactory、UseCase、AppResult/AppError 或完整 MVI。
- Long tasks are cancelled in `onCleared()`.
