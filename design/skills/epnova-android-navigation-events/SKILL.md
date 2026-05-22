---
name: epnova-android-navigation-events
description: "Epnova Android 页面跳转与事件通信规范。用户提到 AppRouter、AppNavigator、FlowEventBus、Events、observeEvents、页面跳转、scheme、deep link、push 跳转、H5 jumpUri、登录拦截、登录态变化、UserChangedEvent、MainTabEvent、主 tab 跳转、跨页面刷新、事件总线、直接 start Activity、Activity.start/newInstance、Fragment 页面通信，或需要判断什么时候走 router、什么时候发事件、什么时候直接 start Activity 时使用。"
---

# Epnova Android 导航与事件

## 快速决策

按调用来源和耦合关系选择入口：

- 服务端下发、Push、H5、scheme、deep link、外部可配置 URI：使用 `AppRouter.jump(...)`、`AppRouter.openFromPush(...)` 或 `AppLaunchJump.launchJump(...)`。
- 登录拦截、登录页、登录弹窗、真实登录判断、Token 过期：使用 `AppNavigator`、`SignUtils` 和 `Events.UserChangedEvent`。
- 调用方明确知道目标 Activity，参数简单，属于同一业务上下文：优先使用目标页 `start(...)` / `getIntent(...)`。
- 主 tab 切换：发布 `Events.MainTabEvent`，由 `MainActivity` 统一切换；需要回到主栈时再配合 `ActivityUtils.finishToActivity(MainActivity::class.java, false)`。
- 跨页面、一对多、广播式刷新：使用 `Events.XxxEvent` + `FlowEventBus.postSync(...)` + `observeEvents { on<Events.XxxEvent> { ... } }`。
- 父子页面、Dialog -> 宿主、Adapter item -> 页面、单次结果返回：优先使用 callback、宿主方法、`ActivityResult`、共享 ViewModel 或直接调用，不要滥用全局事件。
- 单个页面内部的一次性 UI 事件：放在 ViewModel 的 `LiveData` / `Flow`，不要新增 `Events` 全局事件。

更多项目落点见 [project-examples.md](references/project-examples.md)。

## AppRouter

`AppRouter` 用于跨入口跳转，不用于代替所有页面跳转。

- 适用场景：Push 跳转、H5 `jumpUri`、服务端配置跳转、外部 scheme、需要按 `uri.host` 分发的 deep link。
- 已有入口：`openFromPush(context, uri)`、`navigate(context, path)`、`jump(context, schemeUri, openBrowser)`、`jump(schemeUri)`、`jump(uri)`。
- 已有 host：`toHome`、`toTargetEpisode`、`restoreCreator`。
- 新增 host 时，先校验 query 参数为空和类型转换失败的情况；缺少核心参数时直接 return 或给出用户可理解提示。
- 只把“路由分发”和“必要的跳转前轻量判断”放进 `AppRouter`。复杂页面业务、提交、轮询、状态管理仍交给目标页面 ViewModel / Repository。
- 普通按钮跳转到明确 Activity 时，不要为了统一入口强行绕 `AppRouter`。

示例：

```kotlin
FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
ActivityUtils.finishToActivity(MainActivity::class.java, false)
```

## AppNavigator 与登录态

登录相关入口统一收敛到 `AppNavigator`。

- 访客不能进入的功能：调用 `AppNavigator.checkLoginIntercepted(showToast = true)`，返回 `true` 时立即中断后续逻辑。
- 必须是真实账号的功能：调用 `AppNavigator.checkRealLogin(showToast = true)`。
- 主 tab、创建、作品、我的等入口拦截优先使用 `checkLoginIntercepted(...)`，不要在页面里重复判断和拼登录页跳转。
- 登录成功/退出由 `SignUtils.signIn()` / `SignUtils.signOut()` 发布 `Events.UserChangedEvent.SIGN_IN` / `SIGN_OUT`。
- Token 过期由通用 `response(...)` 发布 `TOKEN_EXPIRED`，`MainActivity` 统一调用 `AppNavigator.forceLogoutAndRestart()`；业务页面不要重复重启 App。
- 监听登录态变化时，可优先使用 `onUserSignIn(...)`、`onUserSignOut(...)`、`onUserStatusChanged(...)` 这些项目扩展，避免漏掉 `TOKEN_EXPIRED` 的差异处理。

## 直接 Start Activity

目标页明确、参数明确、只有一个调用结果时，使用目标页自己的静态入口。

- 在目标 Activity companion object 中提供 `start(...)` 或 `getIntent(...)`，Intent extra key 抽成常量。
- 调用点使用 `setOnSingleClickListener` 或现有 click binding，避免重复点击。
- 传参优先传业务 id、轻量 model 或已在项目中可序列化的对象；不要传 View、Adapter、Callback 等生命周期对象。
- 同业务内部跳转，例如 `SeriesActivity.start(workId)`、`DramasPlayPagerActivity.start(...)`、`WorksManagementActivity.start(...)`，不要改成 `FlowEventBus`。

## FlowEventBus 与 Events

全局事件只用于跨页面状态同步，不用于替代局部回调。

- 新事件定义放在 `core/Events.java`，实现 `AppEvent`，命名为 `XxxEvent`。
- 事件字段要带足过滤条件，例如 `workId`、`storyId`、`seriesId`、`eventType`，避免所有页面无差别刷新。
- 发送方使用 `FlowEventBus.postSync(Events.XxxEvent(...))` 作为默认选择；确实需要异步投递时才用 `post(...)`。
- 接收方在 `observeEvents { ... }` 内处理，Activity/Fragment 中优先放在 `initObservers()`、`onPrepareView()` 后的观察区。
- 接收方必须按页面生命周期和业务 id 过滤，例如 `if (isActive && event.workId == mRealVM.workId) refresh()`。
- 多个页面都需要响应的刷新、删除、生成完成、配置变化、主 tab 双击，可以用全局事件；Dialog 关闭结果、Adapter item 点击、单页面按钮反馈不要用全局事件。

示例：

```kotlin
observeEvents {
    on<Events.DeleteTVSeriesEvent> { event ->
        if (isActive && event.workId == mRealVM.workId) {
            refresh()
        }
    }
}
```

## 主 Tab 跳转

主 tab 的选中状态只由 `MainActivity` 维护。

- 外部希望切到首页/作品/我的：发布 `Events.MainTabEvent`。
- `MainActivity` 监听后执行 `toHome()` 或 `binding.mrg.check(...)`。
- 从深链或路由回到首页时，可以在发 `MainTabEvent(TAB_HOME)` 后调用 `ActivityUtils.finishToActivity(MainActivity::class.java, false)`。
- tab 连续点击刷新使用 `Events.MainTabDoubleClickEvent`，不要让业务页直接操作主 tab View。
- 需要登录的 tab 点击统一走 `AppNavigator.checkLoginIntercepted(true)` 拦截。

## 生成与审查清单

完成导航或事件代码前检查：

- 入口是否真的需要 `AppRouter`，还是目标 `Activity.start(...)` 更直接。
- 是否把登录拦截统一交给 `AppNavigator`，没有散落重复判断。
- 是否只有跨页面一对多同步才新增 `Events`。
- 新事件是否包含足够过滤字段，观察方是否按 `isActive` 和业务 id 过滤。
- 主 tab 切换是否只通过 `MainTabEvent` 触发，没有直接操作 `MainActivity` 内部 View。
- Token 过期是否只走已有 `TOKEN_EXPIRED` -> `forceLogoutAndRestart()` 链路。
- 没有让 Adapter、Dialog、ViewModel 直接持有 Activity/Fragment/View 做跳转。
