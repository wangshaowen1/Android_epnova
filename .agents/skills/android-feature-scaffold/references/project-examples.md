# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

这些示例用于对齐当前项目页面骨架写法，不要照搬业务逻辑。

## 带 Toolbar 的简单 Activity

文件：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/PhoneShowActivity.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/mine/PhoneShowViewModel.kt`
- `app/src/main/res/layout/activity_phone_show.xml`

可参考写法：

- `BaseViewModelActivity<ActivityPhoneShowBinding, PhoneShowViewModel>`.
- `companion object start(context)`.
- `ToolbarViewModel(navigationAction = { back() }, title = ...)`.
- XML `<layout>` with page variable `layout`.
- Toolbar include 使用 `@layout/view_model_toolbar`。

新代码改进点：

- 新增 UI 时，优先使用项目文本 style 和 attr，避免重复写裸 `textSize`、`textStyle` 和 color 值。

## 带参数和返回结果的 Activity

文件：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/child/AssetSelectActivity.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/assetmanager/child/AssetSelectViewModel.kt`
- `app/src/main/res/layout/activity_asset_select.xml`

可参考写法：

- 可能需要返回结果的页面使用 `getIntent(context, ...)`。
- Intent extra 使用 `Constants.Extra.*`。
- ViewModel 从 `mBundle` 读取参数。
- Activity 只保留 View 初始化、点击监听、结果返回和数据收集。
- ViewModel 维护选中项和页面标题状态。

新代码改进点：

- 分页内容遵循 `android-paging-state-ui`。
- 不要把分页/列表逻辑塞进普通页面骨架 skill。

## 带 Toolbar、参数、观察者和长任务的复杂 Activity

文件：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/continuewriting/ContinueWritingActivity.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/continuewriting/ContinueWritingViewModel.kt`
- `app/src/main/res/layout/activity_continue_writing.xml`

可参考写法：

- `start(context, workId, storyId, workTitle)` 配合 `Constants.Extra.*`。
- ViewModel 持有 `ToolbarViewModel`，并在 `onCreate()` 更新标题。
- ViewModel 读取 `mBundle` 参数。
- Activity 观察 UI-facing LiveData，并处理弹窗/导航。
- ViewModel 持有 API 调用和 SSE handler。
- `onCleared()` 取消长任务 handler。

不要直接照搬：

- 长任务/SSE 逻辑如果重复出现，应单独抽异步任务 skill。
- 只有敏感词检查和导航确实属于视图/流程行为时，才保留在页面中。

## Fragment Tab 页面

文件：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/create/CreateFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/create/CreateViewModel.kt`
- `app/src/main/res/layout/fragment_create.xml`

可参考写法：

- `BaseViewModelFragment<FragmentCreateBinding, CreateViewModel>`.
- `companion object newInstance()`.
- `onPrepareView()` 承载页面初始化；只有逻辑较多时才拆 `initView()` 和观察区。
- `setOnSingleClickListener` for click actions.
- 页面是 Tab 结构时，子 Fragment 通过现有 adapter 模式承载。

新代码改进点：

- 避免空生命周期重写。
- 页面变复杂时，把监听和观察者从 `initView()` 拆出去。

## Base 与共享文件

常用项目基础文件：

- `app/src/main/java/com/kuaigeglobal/epnova/base/viewmodel/AppViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/base/viewmodel/AppRepositoryViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/base/viewmodel/ToolbarViewModel.kt`
- `app/src/main/res/layout/view_model_toolbar.xml`
- `app/src/main/java/com/kuaigeglobal/epnova/common/Constants.java`
- `app/src/main/java/com/kuaigeglobal/epnova/core/AppRepository.kt`

搜索建议：

- `rg -n "BaseViewModelActivity<|BaseViewModelFragment<|ToolbarViewModel\\(" app/src/main/java/com/kuaigeglobal/epnova/feature -g "!**/OssManager.kt"`
- `rg -n "fun start\\(|fun getIntent\\(|fun newInstance\\(" app/src/main/java/com/kuaigeglobal/epnova/feature -g "!**/OssManager.kt"`
- `rg -n "layout=\"@layout/view_model_toolbar\"|name=\"layout\"" app/src/main/res/layout`
