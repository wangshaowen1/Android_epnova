# 项目弹窗索引

仅在完整 Dialog Skill 已命中且目标附近没有足够同类实现时读取。

| 要确认的能力 | 搜索入口 |
| --- | --- |
| 中心/底部基类 | `BaseDialogFragment`、`BaseVMDialogFragment`、`BaseBottomPushDialogFragment` |
| 通用确认弹窗 | `showCommonDialog`、`showConfirmDialog`、`showConfirmPrompt` |
| 参数和展示 | `newInstance(`、`show(manager`、`mBundle` |
| 输入与 IME | `windowSoftInputMode`、`hideSoftInput`、`EditText` |
| 结果返回 | `setFragmentResult`、`registerForActivityResult`、`onConfirm` |

```powershell
rg -n "class .*Dialog|Base.*DialogFragment|show(Common|Confirm).*Dialog" app/src/main/java
rg -n "dismissAllowingStateLoss|setFragmentResult|mBundle|getArguments" app/src/main/java
```

先读所选 Base 类，再读一个同位置、同复杂度弹窗；不要用业务名称列表替代现场搜索。
