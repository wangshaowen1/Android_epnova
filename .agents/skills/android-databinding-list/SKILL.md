---
name: android-databinding-list
description: "Android 非分页 DataBinding 列表实现规范。仅在本次实际新增、重构或审查非分页 RecyclerView/DataBinding 列表实现，包括 Adapter、ItemVM、item layout、列表数据更新和点击转发时使用。只排查列表 bug、只改 item 文案/样式或只改一小段刷新逻辑时，优先读目标代码和 android-check-quick.md，不触发完整列表 skill。"
---

# Android 非分页 DataBinding 列表

## 触发条件

当任务实际新增、重构或审查 Android 项目的普通非分页 RecyclerView 列表实现时使用本 skill：

- 用户明确要求新增、重构或审查非分页列表、普通列表、DataBinding 列表、QuickAdapter、AppItemViewModel、ItemXxxVM、BaseMultiItemAdapter、submitList、RecyclerViewVM、item XML、item 变量、点击事件转发、多类型列表、混排列表或静态选项列表。
- 生成或审查 `QuickAdapter<ItemXxxVM>`、`AppItemViewModel<T>`、`BaseMultiItemAdapter<AppItemViewModel<*>>`、item layout、列表 VM、列表点击事件、`submitList(...)` 更新。
- 页面列表来自一次性接口、内存数据、配置选项、搜索结果、详情页子列表或表单选项。

只排查列表 bug、只改 item 文案/样式、只改刷新逻辑的一小段、只调整 BottomSpace/Decoration/间距时，优先读目标代码和 `android-check-quick.md`，不使用完整列表 skill。

如果列表是分页加载、下拉刷新/上拉加载或 `PagingDataAdapter`，且本次实际修改分页链路，再读取 `android-paging-state-ui`。如果本次实际新增页面骨架，再读取 `android-feature-scaffold`。只有命名不确定、批量新增资源或用户要求命名审查时，才读取 `android-naming`。

## 核心规则

普通 DataBinding 列表优先使用项目现有组合：

- 单类型普通列表：`QuickAdapter<ItemXxxVM>(R.layout.item_xxx, emptyList())`。
- item VM：继承 `AppItemViewModel<T>`，命名为 `ItemXxxVM`。
- item XML：使用 `<layout>`，DataBinding 变量名固定为 `item`，类型优先绑定 `ItemXxxVM`。
- 列表更新：优先 `adapter.submitList(newItems)`。
- 自定义 Adapter 或 PagingDataAdapter 的 ViewHolder 优先继承 `BindingViewHolder<VB>`，并通过 `binding.getVm(...)` 获取或复用 item VM。
- 复杂列表、多选、批量操作、跨页面刷新或需要统一收敛的 item 点击/选择/操作，优先通过 `BaseItemEventsViewModel` 或当前 feature 已有事件 VM 转发；简单选择、弹窗列表、静态配置项、局部 item 操作可以使用 item VM callback、`onItemClick.value`、ViewModel 方法或同类 `setOnItemClickListener`。
- 多类型列表：使用 `BaseMultiItemAdapter<AppItemViewModel<*>>`，参考 `AssetManagementAdapter`、`StoryboardDesignAdapter`。
- 静态/配置型列表可以使用 `RecyclerViewVM().Builder().setQuickAdapter(...)`。

不要为了普通列表引入 `PagingSource`、`PagingDataAdapter`、`RemoteMediator`、Hilt、UseCase、完整 MVI 或新的 BaseAdapter。Adapter 只负责渲染和转发事件，不直接请求网络或访问数据库。

## 单类型列表

ViewModel 中持有 Adapter，符合当前项目风格：

```kotlin
val resultAdapter = QuickAdapter<ItemSearchResultVM>(R.layout.item_search_result, emptyList())

fun loadList() {
    request(
        apiBlock = { mRepo.api.xxx(body) },
        onSuccess = {
            resultAdapter.submitList(data?.map { item ->
                ItemSearchResultVM(item).apply {
                    onItemClick.value = {
                        // 页面跳转、弹窗或回调放在 ViewModel 中处理
                    }
                }
            } ?: emptyList())
        }
    )
}
```

item VM 保持轻量，持有展示状态和回调入口：

```kotlin
class ItemSearchResultVM(data: WorkVOInfo?) : AppItemViewModel<WorkVOInfo>(data) {
    var keyWord: String? = null
}
```

item XML 变量固定为 `item`：

```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <data>
        <variable
            name="item"
            type="com.kuaigeglobal.epnova.feature.xxx.ItemXxxVM" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        singleClick="@{(v) -> item.onItemClick.invoke(v)}"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        ...
    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
```

## 点击事件

优先让 ViewModel 组装 item VM 时注入回调。需要跨页面统一处理、多选状态、批量操作或分页列表操作时，再使用 `BaseItemEventsViewModel` 或当前 feature 已有事件 VM：

```kotlin
ItemEarnRewardVM(item).apply {
    onWatchAction = { preReportAd(this) }
    onLockedAction = { showToast(R.string.rewards_finish_previous_task) }
}
```

item VM 只暴露动作方法或 `onItemClick`：

```kotlin
fun onWatch() {
    if (isLocked.get()) onLockedAction?.invoke() else onWatchAction?.invoke()
}
```

不要在 Adapter 中写业务请求。item VM 也不要直接持有 Activity/Fragment/View；需要跳转、Toast、弹窗、接口请求时，优先通过 ViewModel 注入 callback。

## 数据更新

- 接口返回后把数据映射成 `ItemXxxVM`，再 `submitList(...)`。
- 空列表使用 `emptyList()`，不要传可变的 null。
- 只有强制整体刷新、且本地已有代码明确需要时，才使用 `adapter.refresh()` 或 `notifyDataSetChanged()`。
- 需要局部新增/删除时，可以使用 BaseQuickAdapter 的 `add(...)`、`remove(...)`，但要确保当前列表确实是本地编辑型列表。
- 不要复用同一个可变 `MutableList` 反复改完再提交，优先提交新列表，减少 Diff/刷新异常。

## 多类型列表

混排列表使用 `BaseMultiItemAdapter<AppItemViewModel<*>>`：

```kotlin
class XxxAdapter : BaseMultiItemAdapter<AppItemViewModel<*>>() {
    init {
        addItemType(1, MultiItemAdapterListener(R.layout.item_xxx))
        addItemType(2, MultiItemAdapterListener(R.layout.item_add_button))
        onItemViewType { position, list ->
            when (list[position]) {
                is ItemAddVM -> 2
                else -> 1
            }
        }
    }
}

class MultiItemAdapterListener(
    @LayoutRes private val layout: Int
) : BaseMultiItemAdapter.OnMultiItemAdapterListener<AppItemViewModel<*>, QuickViewHolder> {
    override fun onBind(holder: QuickViewHolder, position: Int, item: AppItemViewModel<*>?) {
        holder.bind(item)
    }

    override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder.create(layout, parent)
    }
}
```

多类型列表适合标题 + 内容 + 添加按钮、角色/场景/道具混排、故事板 + 添加项等场景。普通同构列表不要为了“可扩展”提前做成多类型。

## RecyclerViewVM 分支

静态选项、配置项、简单横向列表可以沿用 `RecyclerViewVM.Builder().setQuickAdapter(...)`：

```kotlin
val mStyleAdapterVm = RecyclerViewVM()
    .Builder()
    .setQuickAdapter(R.layout.item_creation_style, styleVms)
    .build()
```

该模式适合 XML 已绑定 `RecyclerViewVM` 的页面。不要为了一个普通页面强行引入 `RecyclerViewVM`，优先跟随附近代码。

## 与分页列表区分

以下情况不要使用本 skill 作为主流程，应使用 `android-paging-state-ui`：

- 接口返回 `PagedList<T>`，或页面有 page/pageSize。
- 需要上拉加载更多、分页 footer、`LoadStateViewHolder`、`SmartRefreshLayout.startRefreshWithAdapter(...)`。
- 使用 `PagingSource`、`Pager(...).flow.cachedIn(viewModelScope)`、`PagingDataAdapter`。

## 审查清单

普通列表完成前检查：

- 单类型列表使用 `QuickAdapter<ItemXxxVM>`，多类型列表才使用 `BaseMultiItemAdapter<AppItemViewModel<*>>`。
- item VM 继承 `AppItemViewModel<T>`，命名符合 `ItemXxxVM`。
- item XML 使用 `<layout>`，变量名是 `item`，类型是 `ItemXxxVM`。
- 自定义 Adapter / PagingDataAdapter 的 ViewHolder 使用 `BindingViewHolder`，并通过 `binding.getVm(...)` 复用 item VM。
- 列表更新优先使用 `submitList(newItems)`。
- Adapter 没有网络、数据库、跳转和复杂业务逻辑。
- 复杂列表、多选、批量操作、跨页面刷新或分页列表操作优先通过 `BaseItemEventsViewModel` 或同类事件 VM 转发；简单选择、弹窗列表、静态配置项、局部 item 操作可以使用 item VM callback、`onItemClick`、ViewModel 方法或同类 `setOnItemClickListener`。
- 用户可见文案使用 string 资源，文本样式和颜色优先使用项目 style/attr。
- 普通列表没有误引入 Paging3、RemoteMediator、UseCase、Hilt 或新 Base 类。
