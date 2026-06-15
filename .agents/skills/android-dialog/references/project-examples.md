# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

## 基类和工具入口

常见外部基类：

- `com.nhsd.basic.BaseDialogFragment`
- `com.nhsd.basic.BaseVMDialogFragment`
- `com.nhsd.basic.BaseBottomPushDialogFragment`
- `com.nhsd.basic.BaseBottomPushVMDialogFragment`

项目内工具：

- `app/src/main/java/com/kuaigeglobal/epnova/widget/CommonDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/common/ext/DialogExt.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/base/BaseBlurDialogFragment.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/base/BaseBlurVMDialogFragment.kt`

说明：

- `CommonDialog` / `DialogExt` 适合普通确认和提示。
- `BaseBlurDialogFragment` / `BaseBlurVMDialogFragment` 是项目自定义 blur 分支，只有附近已有同类需求或设计明确要求 blur 背景时参考，不作为默认弹窗基类。

## BaseDialogFragment 示例

简单中心弹窗：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/main/ActivityDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/rewards/CheckInSuccessDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/rewards/RewardRulesDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/login/PrivacyPolicyDialog.kt`

可参考写法：

- 无复杂页面状态和接口时使用 `BaseDialogFragment<VB>`。
- 参数通过 `arguments` 读取。
- `ActivityDialog.show(...)` 内部自行找 `topActivity as? FragmentActivity` 并立即展示。
- `CheckInSuccessDialog.newInstance(...)` 只创建实例，调用方设置 `onWatchAdClicked` 后再 `show(...)`。
- `PrivacyPolicyDialog` 用 callback 返回同意/拒绝结果。

注意：

- 简单中心弹窗不要新增 ViewModel。
- 关闭按钮只关闭弹窗，不要误触发确认回调。

## BaseVMDialogFragment 示例

带 ViewModel 的中心弹窗：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/splash/newer/FirstRechargeDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/splash/newer/FirstRechargeViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/download/DownloadingDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/upgrade/UpgradeDialogFragment.kt`

可参考写法：

- 弹窗包含倒计时、支付、下载进度、接口请求、长任务时使用 `BaseVMDialogFragment<VB, VM>`。
- ViewModel 通过 `mBundle` 读取参数。
- Dialog 负责按钮、观察事件、关闭；支付/下载/倒计时逻辑在 ViewModel。
- 长任务 `Job` 在 ViewModel 中保存并在 `onCleared()` 取消。
- 不可取消场景可在 `onCreate()` 中设置 `isCancelable = false`，参考 `DownloadingDialog`。

## BaseBottomPushDialogFragment 示例

无 ViewModel 的底部弹窗：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/play/more/DownloadMethodDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/info/DramaTitleEditDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/BottomDatePickerDialog.java`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/BottomTimePickerDialog.java`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/AreaPicker.java`

可参考写法：

- 底部选项、简单输入、日期/时间/地区选择使用 `BaseBottomPushDialogFragment<VB>`。
- `DownloadMethodDialog` 通过宿主实现接口 `DownloadTypeLauncher` 返回选择结果。
- `DramaTitleEditDialog` 用 `onSave: ((String) -> Unit)?` 返回输入结果。
- 输入弹窗关闭时注意隐藏软键盘。

## BaseBottomPushVMDialogFragment 示例

带 ViewModel 的底部弹窗：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/info/InputDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/play/more/EpisodeChooseDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/recharge/PowerConsumeConfirmDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/settings/report/ReportDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/set/VideoSetDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/recharge/PowerRechargeDialog.kt`

可参考写法：

- 底部弹窗包含接口提交、列表选择、复杂输入、余额校验、充值等状态时使用 `BaseBottomPushVMDialogFragment<VB, VM>`。
- 参数通过 `arguments` 传入，ViewModel 用 `mBundle` 读取。
- `InputDialog.show(...)` 设置参数和 `callback`，保存成功后调用 callback 并关闭。
- `EpisodeChooseDialog` 通过 `onEpisodeSelected` 返回选择结果。
- `ReportDialog` 点击提交后调用 `mRealVM.submit(...)`，观察 `isSubmit` 成功后 toast 并关闭。
- `PowerConsumeConfirmDialog` 点击确认前先做余额校验，再触发 `onConfirm`。

## 通用确认弹窗示例

- `app/src/main/java/com/kuaigeglobal/epnova/common/ext/DialogExt.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/widget/CommonDialog.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/series/SeriesActivity.kt`

适合场景：

- 删除确认。
- 退出未保存确认。
- 单按钮提示。
- 普通文案确认，无自定义复杂布局。

示例写法：

```kotlin
showCommonDialog(title = getString(R.string.delete_work_warning)) {
    mRealVM.delete()
}
```

或参考 `InputDialog.closeConfirm()` 使用 `showConfirmPrompt(...)` 做“保存并退出/直接退出”。

## 搜索建议

```bash
rg -n "BaseDialogFragment|BaseVMDialogFragment|BaseBottomPushDialogFragment|BaseBottomPushVMDialogFragment" app/src/main/java/com/kuaigeglobal/epnova -g "!**/OssManager.kt"
rg -n "showCommonDialog\\(|showConfirmPrompt\\(|CommonDialog\\(" app/src/main/java/com/kuaigeglobal/epnova -g "!**/OssManager.kt"
rg -n "var on[A-Z].*\\(|callback:|onConfirm|onCancel|onSave|on.*Selected" app/src/main/java/com/kuaigeglobal/epnova -g "!**/OssManager.kt"
```
