# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

## 核心基础文件

- `app/src/main/java/<主业务包路径>/common/adapter/QuickAdapter.kt`
- `app/src/main/java/<主业务包路径>/base/item/AppItemViewModel.kt`
- `app/src/main/java/<主业务包路径>/base/RecyclerViewVM.kt`
- `app/src/main/java/<主业务包路径>/common/ext/ItemViewModelExtensions.kt`

要点：

- `QuickAdapter<T : QuickItemViewModel>` 内部通过 `QuickViewHolder.bind(item)` 绑定 DataBinding。
- `AppItemViewModel<T>.id()` 固定返回 `BR.item`，所以 item XML 变量名必须是 `item`。
- `RecyclerViewVM.Builder().setQuickAdapter(...)` 适合已有 XML 绑定 `RecyclerViewVM` 的静态/配置型列表。
- `ItemViewModelExtensions.kt` 里基于 position 的 `getVm(...)` 已标记废弃，新代码不要主动采用 position 缓存。

## QuickAdapter + submitList 示例

普通奖励页列表：

- `app/src/main/java/<主业务包路径>/feature/rewards/RewardsViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/rewards/ItemCheckInDayVM.kt`
- `app/src/main/java/<主业务包路径>/feature/rewards/ItemEarnRewardVM.kt`
- `app/src/main/res/layout/item_check_in_day.xml`
- `app/src/main/res/layout/item_earn_reward.xml`

可参考写法：

- ViewModel 持有 `QuickAdapter<ItemCheckInDayVM>` 和 `QuickAdapter<ItemEarnRewardVM>`。
- 接口成功后 `taskList.map { ItemCheckInDayVM(it) }`，再 `submitList(...)`。
- `ItemEarnRewardVM` 暴露 `onWatchAction`、`onLockedAction`、`onCheckInAction`，由 ViewModel 注入具体动作。
- item XML 变量名为 `item`，按钮使用 `singleClick="@{(v) -> item.onWatch()}"` 或类似写法。

注意：

- `ItemEarnRewardVM` 的点击方法只分发动作，不直接请求接口。
- 如果列表接口需要 loading/empty/error，状态仍放在页面 ViewModel 中，不放在 Adapter。

## 搜索结果列表示例

- `app/src/main/java/<主业务包路径>/feature/home/search/SearchViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/home/search/ItemSearchResultVm.kt`
- `app/src/main/res/layout/item_search_result.xml`

可参考写法：

- `resultAdapter = QuickAdapter<ItemSearchResultVm>(R.layout.item_search_result, emptyList())`。
- 接口成功后把 `WorkVOInfo` 映射成 `ItemSearchResultVm`。
- ViewModel 给 item VM 注入 `onItemClick.value`，item XML 用 `singleClick="@{(v)->item.onItemClick.invoke(v)}"` 转发点击。
- XML 文本、图片绑定直接使用 `item.data.xxx`，复杂展示逻辑放到 item VM 或 BindingAdapter。

## 多类型列表示例

资产管理混排：

- `app/src/main/java/<主业务包路径>/feature/creator/assets/AssetManagementAdapter.kt`
- `app/src/main/java/<主业务包路径>/feature/creator/assets/AssetManagementViewModel.kt`

故事板混排：

- `app/src/main/java/<主业务包路径>/feature/creator/storyboard/StoryboardDesignAdapter.kt`
- `app/src/main/java/<主业务包路径>/feature/creator/storyboard/StoryboardDesignViewModel.kt`

可参考写法：

- Adapter 继承 `BaseMultiItemAdapter<AppItemViewModel<*>>`。
- `addItemType(type, MultiItemAdapterListener(layout))` 注册布局。
- `onItemViewType { position, list -> ... }` 根据 item VM 类型返回 viewType。
- `MultiItemAdapterListener.onBind(...)` 只调用 `holder.bind(item)`。
- ViewModel 组装 `MutableList<AppItemViewModel<*>>`，最后 `adapter.submitList(items)`。
- 本地编辑型混排列表可以使用 `adapter.add(...)`、`adapter.remove(...)`，例如添加角色/场景/分镜。

注意：

- 多类型列表是为了真实混排，不是普通列表的默认方案。
- 当 item VM 持有编辑状态或长任务时，替换列表前需要参考现有代码清理旧 item 状态，例如 `adapter.items.forEach { if (it is ItemAssetVM<*>) it.clear() }`。

## RecyclerViewVM 示例

- `app/src/main/java/<主业务包路径>/feature/creator/set/VideoSetViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/creator/set/allstyle/CreationStyleChooseViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/play/more/EpisodeChooseViewModel.kt`

适合场景：

- 固定选项列表。
- 简单横向/网格配置项。
- XML 已经绑定 `RecyclerViewVM`，并通过 BindingAdapter 设置 RecyclerView。

搜索建议：

```bash
rg -n "QuickAdapter<|submitList\\(|setQuickAdapter\\(" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
rg -n "BaseMultiItemAdapter<AppItemViewModel|addItemType\\(|onItemViewType" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
rg -n "name=\"item\"|singleClick=\"@\\{.*item" app/src/main/res/layout
```
