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

## 基类选择

优先按内容复杂度和展示位置选择基类：

- 简单中心弹窗，无页面状态/接口/复杂业务：`BaseDialogFragment<VB>`。
- 中心弹窗，带倒计时、支付、下载、接口、复杂状态：`BaseVMDialogFragment<VB, VM>`。
- 简单底部弹窗，无 ViewModel，只做选项选择/输入回调：`BaseBottomPushDialogFragment<VB>`。
- 底部弹窗，带 ViewModel、接口提交、列表状态、复杂选择：`BaseBottomPushVMDialogFragment<VB, VM>`。
- 只是通用确认/提示，不需要自定义布局：优先使用 `showCommonDialog(...)`、`showConfirmDialog(...)` 或 `showConfirmPrompt(...)`。
- 极小的 ViewBinding-only 非 Fragment 弹窗可以使用 `CommonDialog` / `Dialog(context, style)`，但不要作为复杂弹窗默认方案。

不要为了“规范完整”给简单确认弹窗新增 Fragment + ViewModel。也不要在需要接口、倒计时、支付、列表状态的弹窗里硬塞无 VM 基类。

简单业务弹窗不为了 DataBinding 表达式把接口实体或 Dialog 本身塞进 XML；优先在 Dialog 内直接设置文案、选中态、显隐和点击监听。业务复杂不只按接口/长任务判断：只要存在多处派生展示状态、样式分支、选中态、参数规整或复用状态，就改用 VM 弹窗基类，并让 XML 只绑定 `layout`。VM 可以暴露源对象和少量选择状态，简单颜色、背景、显隐用 XML 三元表达式表达；不要把每个颜色/背景/显隐都拆成一堆 Observable，也不要在 XML `<data>` 里单独声明接口实体或 Dialog 变量来承载展示。

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
