# 项目示例索引

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

本文件记录导航与事件通信 skill 的项目依据。生成代码时按这些文件的现有写法对齐，不要照搬通用 Android 模板。

## 核心文件

- `app/src/main/java/com/kuaigeglobal/epnova/router/AppRouter.kt`
  - `openFromPush(context, uri)`：Push URI 进入 `AppLaunchJump.launchJump(...)`。
  - `navigate(context, path)`：IRouter 入口，转到 `jump(path)`。
  - `jump(context, schemeUri, openBrowser)`：HTTP 且要求外部浏览器时走 `openBrowser(...)`，否则继续项目内分发。
  - `jump(schemeUri)`：处理空 URI、项目 scheme、apk 下载、HTTP 内置浏览器。
  - `jump(uri)`：按 `uri.host` 处理 `restoreCreator`、`toHome`、`toTargetEpisode`。
  - `startStory(...)`：作品/剧集恢复创作入口，供业务页直接复用。
- `app/src/main/java/com/kuaigeglobal/epnova/router/AppNavigator.kt`
  - `checkLoginIntercepted(...)`：访客拦截。
  - `checkRealLogin(...)`：真实登录拦截。
  - `startLogin(...)`：按配置选择手机号登录或统一登录、全屏页或弹窗。
  - `forceLogoutAndRestart()`：Token 失效后的统一退出重启。
- `app/src/main/java/com/kuaigeglobal/epnova/core/Events.java`
  - 项目全局事件集中定义处，事件类实现 `AppEvent`。
- `app/src/main/java/com/kuaigeglobal/epnova/core/ext/EventScopeExt.kt`
  - `onUserSignIn(...)`、`onUserSignOut(...)`、`onUserStatusChanged(...)` 是登录态监听的优先封装。

## 主 Tab 与登录态

- `app/src/main/java/com/kuaigeglobal/epnova/feature/main/MainActivity.kt`
  - `handleIntent(...)` 遇到 `KEY_SCHEME` 时调用 `AppRouter.jump(schemeUri)`。
  - 创建、作品、我的 tab 点击使用 `AppNavigator.checkLoginIntercepted(true)` 做拦截。
  - tab 连续点击发布 `Events.MainTabDoubleClickEvent`。
  - `observeEvents { on<Events.MainTabEvent> { ... } }` 中统一切换 tab。
  - `observeEvents { on<Events.UserChangedEvent> { ... } }` 中处理 `TOKEN_EXPIRED` 和用户信息刷新。
- `app/src/main/java/com/kuaigeglobal/epnova/kit/SignUtils.kt`
  - `signIn()` 发布 `Events.UserChangedEvent.SIGN_IN`。
  - `signOut()` 发布 `Events.UserChangedEvent.SIGN_OUT`。
- `app/src/main/java/com/kuaigeglobal/epnova/core/Response.kt`
  - 通用 `response(...)` 遇到授权错误、退出登录、黑名单等 code 时发布 `Events.UserChangedEvent.TOKEN_EXPIRED`。

## 事件监听与过滤

- `app/src/main/java/com/kuaigeglobal/epnova/feature/series/SeriesActivity.kt`
  - 监听 `Events.SeriesUpdatedEvent` 时校验 `event.seriesId == mRealVM.workId`。
  - 监听 `Events.DeleteTVSeriesEvent` 时校验 `event.workId == mRealVM.workId`。
  - 普通页面跳转仍使用 `DramasPlayPagerActivity.start(...)`、`WorksManagementActivity.start(...)`。
  - 作品恢复创作复用 `AppRouter.startStory(...)`，不是所有跳转都发全局事件。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/AssetManagementFragment.kt`
  - 监听 `RoleGenerateEvent`、`SceneGenerateEvent`、`PropGenerateEvent` 时先判断 `isActive`，再交给 ViewModel 处理。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/series/management/WorksManagementActivity.kt`
  - 删除作品后发布 `Events.DeleteTVSeriesEvent(mRealVM.workId)`，让相关页面自行刷新。

## 现有事件类型

常见可复用事件：

- `Events.MainTabEvent`：主 tab 切换，常量 `TAB_HOME`、`TAB_MY_CREATIONS`、`TAB_MINE`。
- `Events.MainTabDoubleClickEvent`：主 tab 连续点击刷新。
- `Events.UserChangedEvent`：登录成功、退出登录、Token 过期。
- `Events.ConfigChangeEvent`：配置变化。
- `Events.SeriesUpdatedEvent`：剧集更新。
- `Events.DeleteTVSeriesEvent`：作品/剧集删除后刷新。
- `Events.ShortDramasEditInfoEvent`：短剧信息编辑后刷新。
- `Events.RoleGenerateEvent`、`Events.SceneGenerateEvent`、`Events.PropGenerateEvent`：资产生成完成。

## 搜索命令

需要补充示例时，优先使用：

```powershell
rg -n "AppRouter|AppNavigator|FlowEventBus|observeEvents|Events\\." app/src/main/java/com/kuaigeglobal/epnova
rg -n "MainTabEvent|MainTabDoubleClickEvent|UserChangedEvent" app/src/main/java/com/kuaigeglobal/epnova
rg -n "fun start\\(|getIntent\\(" app/src/main/java/com/kuaigeglobal/epnova/feature
```
