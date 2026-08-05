---
name: android-databinding-ui
description: "Android 页面状态 DataBinding 规范。仅在新增、重构或审查 Activity/Fragment/Dialog/XML 的稳定页面状态绑定、多个互斥区域、Observer 中批量写 View、ViewAnimator/ViewSwitcher、Observable/LiveData 到 text/visibility/enabled/selected 映射时使用。手势逐帧、动画过程、录音按压、播放器进度、第三方控件即时回调或一次性简单 View 赋值不触发。"
---

# Android 页面状态 DataBinding

## 状态边界

- ViewModel 是稳定页面状态的单一来源；XML 映射 text/visibility/enabled/selected，页面层保留生命周期、导航、弹窗、权限、动画和依赖 View 的即时交互。
- `binding.xxx` 只是类型安全访问 View，不代表状态已由 DataBinding 驱动。
- 接口结果、关系状态、数量、红点、选中、启用和稳定显隐优先绑定。
- 手势坐标、按压过程、动画逐帧、播放器进度、ViewPager 当前下标和第三方控件即时 callback 保留命令式更新。
- 不把瞬时 UI 状态写进接口实体；放 ViewModel 的 `Observable*` / `LiveData`，或确有多状态需求时使用专用 UiModel。
- XML 只写简单映射；跨多个条件的业务判断先在 ViewModel 收敛为语义状态。
- 无 VM 且参数创建后不再变化的多模式 Dialog，可绑定 Dialog 暴露的只读语义属性，不为绑定增加空壳 VM。

## 迁移流程

1. 用 `rg -n "binding\..*\.(text|visibility|isVisible|isEnabled|isSelected)\s*="` 定位直接写 View。
2. 区分稳定状态和瞬时交互；同一 View 有多个写入源时先合并状态。
3. 在 ViewModel 暴露语义状态，并让所有业务入口更新同一来源。
4. 在 XML 绑定属性或现有 BindingAdapter，删除重复 Observer 和失效旧 View。
5. 静态检查表达式类型、资源引用和旧赋值残留。

## DataBinding 易错类型

- `backgroundRes`、`textColorRes`、`imageResource` 等 `*Res` 适配器接收资源 ID；条件 drawable/颜色往往被解析成实际对象或色值，不能直接传给 `*Res`。
- 条件背景使用接收 Drawable 的适配器，条件文字色使用 `android:textColor`；必须传资源 ID 时由 VM 暴露带 `@DrawableRes/@ColorRes` 语义的 Int。
- `@drawable/x : 0` 会形成 Drawable/Int 混合分支；改为明确的 drawable resource ID 状态。
- 数量文本使用 string 格式化资源或类型明确的 BindingAdapter，不在 XML 导入 `String` 再对 LiveData 调 `valueOf`。
- 会主动发送 null 清空事件的状态声明为 `LiveData<T?>`，观察端显式处理；不要用非空类型包装可空 value。

## 互斥区域

- 同一位置三个及以上互斥布局优先 `ViewAnimator`；两个轻量子 View 可用 `ViewSwitcher` 或简单绑定。
- ViewModel 暴露单一语义索引，XML 绑定 `android:displayedChild`；索引用业务常量，不在页面散落数字。
- 需要“全部隐藏”时增加零尺寸空 child，不同时控制容器和所有 child 的显隐。
- 不让多个 Observer 分别控制互斥 child；键盘关闭、停止录音等副作用可以继续观察状态执行。

## 收敛阈值

出现任一情况时，停止追加局部 `isVisible`，先收敛状态：

- 同一属性由两个以上方法修改。
- 一个渲染方法连续修改三个以上 View 的稳定状态。
- 两个 Observer 共同决定同一区域。
- 新状态需要“先隐藏全部，再显示一个”。
- XML 已有 `<layout>`，但稳定状态仍主要由 Observer 手写。

## 检查

- Base 类已绑定 `layout` 和 lifecycle owner。
- 所有业务入口更新同一语义状态，互斥容器只有一个有效 child。
- 命令式代码只保留副作用和瞬时交互。
- 表达式值类型与 BindingAdapter 参数一致。
