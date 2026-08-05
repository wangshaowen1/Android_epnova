---
name: android-dialog
description: "Android 弹窗实现规范。仅在本次实际新增、重构或审查 Dialog/DialogFragment 的基类选择、参数传递、show/newInstance、按钮事件、结果回调、生命周期、键盘处理或关闭逻辑时使用。已有弹窗只改文案、Toast、字段、样式、局部 DataBinding、简单选择或按钮回调时，优先读目标代码，不触发完整弹窗 skill。"
---

# Android 弹窗

本次确实按 Figma/截图还原可见 UI，或弹窗内部实际新增列表时，才追加对应 skill。只有基类或工具用法不清楚时读取 [项目弹窗索引](references/project-examples.md)。

## 基类决策

| 场景 | 优先选择 |
| --- | --- |
| 简单中心弹窗，无接口/长任务/复杂状态 | `BaseDialogFragment<VB>` |
| 中心弹窗，含接口、支付、下载、计时或复杂状态 | `BaseVMDialogFragment<VB, VM>` |
| 简单底部选择或输入回调 | `BaseBottomPushDialogFragment<VB>` |
| 底部弹窗，含接口或复杂列表状态 | `BaseBottomPushVMDialogFragment<VB, VM>` |
| 普通确认/提示，无自定义布局需求 | `showCommonDialog` / `showConfirmDialog` / `showConfirmPrompt` |

使用 DataBinding、输入框或 RecyclerView 本身不是新增 ViewModel 的理由。简单输入、单次选择和按钮 callback 可保持无 VM；接口、支付、下载、轮询/SSE、倒计时或需要恢复的复杂状态才进入 VM。

## 布局与绑定

- 根布局默认 `wrap_content` 且纵向约束闭合；设计稿总高度只用于核对，不直接固定。
- 长文案、系统字号和多语言区域允许伸缩、滚动或省略；固定高度只用于确实固定的按钮、封面、播放器等局部控件。
- 设置全宽、安全区或 IME 前先检查所选 Base Dialog 已有 Window/insets/键盘处理，避免重复 padding 或高度。
- 无 VM 弹窗直接使用 binding 和局部字段，不伪造 `layout`、`dialog` 或 `entity` 变量。
- VM 弹窗页面变量默认只用 `layout`；不为 XML 可直接读取的简单字段增加透传 Observable/getter。
- 简单初始化直接写在 `onPrepareView()`；只有监听、观察、列表等职责独立时拆 `init*()`，不生成空包装。

## 参数与展示

- DialogFragment 参数通过 `arguments`；公共 key 优先 `Constants.Extra.*`，私有 key 放 companion object。
- 带 VM 的参数由 ViewModel 从 `mBundle` 读取；不要用构造函数给 Fragment/DialogFragment 传参。
- 只构建实例用 `newInstance(...)`；立即展示用 `show(FragmentManager, ...)`。
- 调用方需要设置短生命周期 callback 时，`show` 返回 dialog 实例。

## 事件与结果

- 确认、提交、保存、支付、删除、生成和跳转等有副作用动作使用 `setOnSingleClickListener` 或项目同类单击绑定。
- 关闭、取消、简单 toggle 可沿用附近 `setOnClickListener`。
- 先校验输入/状态，再回调或调用 VM，最后按当前生命周期关闭；取消路径不触发确认逻辑。
- 属性 callback 只适合当前进程、当前宿主、可接受配置变更或进程重建丢失的一次性结果。
- Fragment result、ActivityResult 或共享状态只能解决各自生命周期内的结果传递，不等于持久化。支付结果、任务 ID 等关键结果必须由 SavedState、持久存储或服务端状态承担。
- 异步结果由 VM 暴露 LiveData/Flow，Dialog 观察成功后再关闭。

## 所有权与关闭

- Dialog ViewModel 创建的页面任务在 `onCleared()` 取消。
- Service、系统下载、应用级 Manager 或持久队列拥有的任务，弹窗关闭只解绑/重绑观察，不误取消底层任务。
- 正常交互使用 `dismiss()` 或附近封装。
- 只有 FragmentManager 已保存状态、结果允许丢失且附近同类确实采用时，才用 `dismissAllowingStateLoss()`。
- 所有关闭路径避免重复回调，并清理弹窗自己拥有的键盘、监听器和临时任务。

## 输入弹窗

- 输入初始化放 `onPrepareView()`；未保存内容的返回/关闭走确认离开逻辑。
- 关闭时清理软键盘。
- 敏感词检查、保存接口等异步逻辑交给 ViewModel 或已有扩展，Dialog 只组织 UI 流程。

## 检查

- 基类与位置、状态复杂度匹配，没有因 DataBinding/输入/列表自动加 VM。
- 高度、约束、安全区和 IME 能适应长文案与多语言。
- 参数通过 arguments/mBundle，结果机制符合生命周期和持久性要求。
- 有副作用操作有防重复点击，确认/取消/关闭语义没有串线。
- 页面任务与后台任务所有权明确；`dismissAllowingStateLoss()` 没被当默认关闭方式。
