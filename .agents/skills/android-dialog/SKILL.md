---
name: android-dialog
description: "Android 弹窗实现规范。新增弹窗、复杂重构、基类选择、参数传递、show/newInstance、按钮事件、回调结果、生命周期、键盘处理或关闭逻辑不清晰时使用。已有弹窗的局部改造、DataBinding 绑定、简单选择态、按钮回调、文案/样式调整，优先读目标 Dialog、XML、调用点和 quick rules，不读取完整弹窗 skill。"
---

# Android 弹窗

## 触发条件

当任务实际新增、重构或审查 Android 项目的弹窗结构时使用本 skill：

- 用户明确要求新增、重构或审查弹窗、Dialog、DialogFragment、底部弹窗、中心弹窗、确认弹窗、输入弹窗、选择弹窗、通用弹窗、支付弹窗、规则弹窗、举报弹窗、下载弹窗。
- 需要选择 `BaseDialogFragment`、`BaseVMDialogFragment`、`BaseBottomPushDialogFragment`、`BaseBottomPushVMDialogFragment`、`CommonDialog` 或快捷 `showCommonDialog(...)` / `showConfirmPrompt(...)`。
- 涉及弹窗参数传递、`arguments`、`mBundle`、按钮事件、`setOnSingleClickListener`、回调、结果返回、`dismissAllowingStateLoss()`、键盘关闭或返回键拦截。

只改已有弹窗文案、Toast、字段展示、简单样式、局部数据绑定或单个资源引用时，优先读目标代码和 `android-check-quick.md`，不使用完整弹窗 skill。

如果弹窗是可见 UI 且本次确实按 Figma/截图实现或校准，再读取 Figma XML 规则。弹窗内部只有本次实际新增或修改列表代码时，才按非分页/分页情况读取列表相关 skill。

## 生成决策

先按当前 feature 或同类弹窗实现判断，不要先套模板。决策顺序固定为：

1. 先定展示形态：中心弹窗、底部弹窗、通用确认。
2. 再判断是否真的需要 ViewModel。
3. 再决定 XML 是否绑定 `layout`。
4. 最后才决定是否拆函数。

不要为了“规范完整”给简单弹窗新增 Fragment + ViewModel + 一堆初始化函数。

## 基类选择

优先按展示位置和真实业务复杂度选择基类：

- 简单中心弹窗，无接口、无长任务、无复用状态：`BaseDialogFragment<VB>`。
- 中心弹窗，带倒计时、支付、下载、接口请求、长任务或复用页面状态：`BaseVMDialogFragment<VB, VM>`。
- 简单底部弹窗，只做选项选择、本地输入校验或一次性回调：`BaseBottomPushDialogFragment<VB>`。
- 底部弹窗，带接口提交、长任务、复杂输入流程、列表/adapter VM、余额/登录/支付等复用状态：`BaseBottomPushVMDialogFragment<VB, VM>`。
- 只是通用确认/提示，不需要自定义布局：优先使用 `showCommonDialog(...)`、`showConfirmDialog(...)` 或 `showConfirmPrompt(...)`。
- 极小的 ViewBinding-only 非 Fragment 弹窗可以使用 `CommonDialog` / `Dialog(context, style)`，但不要作为复杂弹窗默认方案。

不要在需要接口、倒计时、支付、下载、轮询、adapter VM 或复用全局/页面状态的弹窗里硬塞无 VM 基类。

## ViewModel 边界

以下情况不要新增 ViewModel：

- 普通确认、提示、规则说明、活动弹窗、纯展示弹窗。
- 简单选项选择、日期/时间/地区 picker、支付方式选择等，只返回一次性结果。
- 简单输入弹窗，只做本地非空/长度校验，然后 callback 返回结果。
- 只有 1-2 个字段展示、简单显隐或按钮点击，Dialog 内直接设置更清楚。

以下情况才使用 VM 弹窗基类：

- 弹窗内发起接口请求、支付、下载、倒计时、轮询、SSE 或其他长任务。
- 存在多类型参数规整、保存流程、敏感词检查、未保存退出确认等可复用输入流程。
- 需要 `RecyclerViewVM`、ItemVM、adapter 状态或复杂选择状态。
- 需要复用当前项目已有 VM 能力，例如 `clickHandler`、`dismiss()`、协议勾选、客服/充值/算力配置、跳转协议页等。
- 同一状态需要被 XML、Dialog 点击逻辑和异步结果共同消费。

“有输入框”“有列表”“有 DataBinding 表达式”本身都不是新增 ViewModel 的理由。

## DataBinding 边界

无 VM 弹窗：

- 可以使用普通 XML，或仅因 BindingAdapter 常量表达式保留 `<layout>` 根节点。
- 不为了绑定而新增 `layout`、`dialog`、接口实体等 `<variable>`。
- 文案、显隐、选中态、点击监听优先在 Dialog 内直接设置。

有 VM 弹窗：

- XML `<data>` 默认只声明 `layout`，不要额外声明 `dialog` 或接口实体变量承载展示。
- VM 可以暴露源对象、少量选择状态和确实复用的展示状态。
- XML 可以直接写简单属性和三元表达式，例如 `@{layout.data.title}`、`@{layout.checked ? View.VISIBLE : View.GONE}`。
- 不要把每个颜色、背景、显隐、文案都拆成 Observable 或 `getXxxText()` / `isXxxVisible()` 透传函数。

列表 item XML 仍按列表规则绑定 `item` / `ItemXxxVM`；不要把列表 item 的写法套到 Dialog 根布局。

## 函数拆分

简单弹窗优先把初始化、点击和一次性赋值直接写在 `onPrepareView()`，不要为了模板拆 `initView()`、`initListener()`、`initObservers()`。

只有代码已经明显超过一个职责时才拆一级函数，例如：

- 初始化 adapter / decoration。
- 注册多个 observer。
- 输入弹窗的键盘、返回拦截、保存流程较长。
- 长任务状态展示或结果处理分支较多。

拆分后函数必须有独立职责，禁止继续拆出 `setupXxx()`、`bindXxx()`、`updateXxx()` 这类只包一两行代码的深层嵌套。

## 参数传递

Fragment 弹窗参数优先通过 `arguments` 传递。Key 优先使用 `Constants.Extra.*`，弹窗私有参数可以在 companion object 内定义 `ARG_XXX`。

无 VM 弹窗在 Dialog 内读取：

```kotlin
class CheckInSuccessDialog : BaseDialogFragment<DialogCheckInSuccessBinding>() {
    companion object {
        private const val ARG_REWARD_NUM = "rewardNum"

        fun newInstance(rewardNum: Int): CheckInSuccessDialog {
            return CheckInSuccessDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_REWARD_NUM, rewardNum)
                }
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        val rewardNum = arguments?.getInt(ARG_REWARD_NUM, 0) ?: 0
    }
}
```

带 VM 弹窗让 ViewModel 从 `mBundle` 读取：

```kotlin
class ReportDialogViewModel : AppRepositoryViewModel<AppRepository>() {
    private val userId by lazy { mBundle.getString(ReportDialog.USER_ID) }
}
```

不要通过构造函数传参给 Fragment/DialogFragment。非序列化的短生命周期回调可以作为属性赋值，但不要放进 `arguments`。

## show / newInstance

只创建实例、不立即展示时用 `newInstance(...)`：

```kotlin
fun newInstance(data: XxxInfo?): XxxDialog {
    return XxxDialog().apply {
        arguments = bundleOf(Constants.Extra.DATA to data)
    }
}
```

内部自行找到 FragmentManager 并立即展示时用 `show(...)`：

```kotlin
fun show(cost: Int): PowerConsumeConfirmDialog? {
    val manager = (topActivity as? FragmentActivity)?.supportFragmentManager ?: return null
    return PowerConsumeConfirmDialog().apply {
        arguments = Bundle().apply { putInt(Constants.Extra.COST, cost) }
        show(manager)
    }
}
```

有调用方上下文、需要设置回调时，返回 dialog 实例，方便外部继续赋值：

```kotlin
val dialog = CheckInSuccessDialog.newInstance(rewardNum, adRewardCoins)
dialog.onWatchAdClicked = { viewModel.preReportCheckInAd() }
dialog.show(supportFragmentManager)
```

## 按钮事件

- 确认、提交、保存、支付、删除、生成、跳转等用户高频或有副作用操作优先使用 `setOnSingleClickListener`。
- 关闭、取消、简单 toggle、只切 UI 状态的按钮，或同类弹窗已采用的简单监听，可以沿用 `setOnClickListener`。
- 点击确认后需要回调时，先校验输入/状态，再触发回调，最后 `dismissAllowingStateLoss()`。
- 取消、关闭按钮只做关闭或取消回调，不触发确认逻辑。
- 弹窗内需要接口请求时，优先交给 ViewModel 方法，Dialog 只负责收集输入、调用 VM、观察结果。

```kotlin
binding.btnConfirm.setOnSingleClickListener {
    val content = binding.xet.text.toString()
    if (content.length < 10) {
        getString(R.string.toast_inspiration_input, 10).showToast()
        return@setOnSingleClickListener
    }
    mRealVM.submit(content, phoneNumber)
}
```

## 结果返回

简单一次性结果可以用属性 callback：

```kotlin
var onConfirm: () -> Unit = {}
var onSave: ((String) -> Unit)? = null
var onEpisodeSelected: (EpisodeInfo?) -> Unit = {}
```

更复杂的结果或异步状态放在 ViewModel 的 `LiveData` / `Flow` 中，由 Dialog 观察后关闭：

```kotlin
mRealVM.isSubmit.observe(this) {
    if (!it) return@observe
    getString(R.string.report_success).showToast()
    dismissAllowingStateLoss()
}
```

跨页面或长期状态不建议用 Dialog 属性 callback 承担。支付、下载、倒计时、轮询等长任务必须在 ViewModel 中保存 `Job` 并在 `onCleared()` 取消。

## 输入和键盘

底部输入弹窗参考 `InputDialog` / `DramaTitleEditDialog`：

- 输入框初始化放在 `onPrepareView()`。
- 有未保存内容时，返回键/关闭按钮走确认离开逻辑。
- 关闭、dismiss、destroy 时清理软键盘。
- 敏感词检查、保存接口等异步逻辑交给 ViewModel 或已有扩展，Dialog 只负责 UI 流程。

## 通用确认弹窗

只是展示标题/内容/确认/取消，不需要自定义布局时使用快捷扩展：

```kotlin
showCommonDialog(title = getString(R.string.delete_work_warning)) {
    viewModel.delete()
}
```

或：

```kotlin
showConfirmPrompt(
    tip = getString(R.string.confirm_exit_without_saving),
    cancelText = getString(R.string.exit_directly),
    confirmText = getString(R.string.save_and_exit),
    cancel = { dismissAllowingStateLoss() },
    confirm = { save() }
)
```

不要为普通确认文案新建一个 DialogFragment，除非设计稿有明显自定义视觉结构或复杂交互。

## 审查清单

弹窗完成前检查：

- 基类选择符合展示位置和复杂度：中心/底部、有无 VM、有无接口/长任务。
- Fragment 参数通过 `arguments`，带 VM 的参数优先由 ViewModel 从 `mBundle` 读取。
- 没有使用 DialogFragment 构造函数传参。
- 确认、提交、保存、支付、删除、生成、跳转等操作使用 `setOnSingleClickListener` 或项目已有 click binding；关闭、取消、简单 toggle 可沿用当前代码的 `setOnClickListener`。
- 确认、取消、关闭语义清晰，不误触发回调。
- 接口、支付、下载、倒计时、轮询等复杂逻辑在 ViewModel，不在 Dialog 里堆业务。
- 回调只用于短生命周期结果返回，不持有 Activity/Fragment/View。
- 关闭时使用项目常见 `dismissAllowingStateLoss()`；需要不可取消时显式设置 `isCancelable = false`。
- 用户可见文案走 string 资源，UI 样式优先使用项目 style/attr/dimen/drawable。
