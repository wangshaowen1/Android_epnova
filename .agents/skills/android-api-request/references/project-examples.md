# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

这些文件体现当前项目 API 接入方式。只复制结构，不要照搬业务逻辑。

## 核心网络文件

- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiConstants.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiService.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiResponse.java`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiException.java`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiFactory.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/Response.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/ext/ViewModelExt.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/AppRepository.kt`

注意：

- 不要修改 `ApiResponse.java` 和 `ApiException.java`。
- `ApiFactory.apiService` 已统一创建 Retrofit Service；业务 ViewModel 默认通过 `mRepo.api` 使用它。

## 普通请求体和接口方法

参考：

- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/request/WorkIdBody.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/request/SaveStorySummaryBody.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/request/WorkInfoUpdateBody.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/request/DeleteWorkBody.kt`

接口方法参考：

- `ApiService.updateStorySummary(...)`
- `ApiService.updateWorkInfo(...)`
- `ApiService.batchDeleteWork(...)`
- `ApiService.getWorkInfo(...)`

## ViewModel 中 requestWithLoading

参考：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/info/InputViewModel.kt`

可复制点：

- 用户点击保存时调用 `requestWithLoading({ mRepo.api.xxx(body) }) { ... }`。
- 请求成功后通过回调通知页面。
- ViewModel 从 `mBundle` 取参数，构造请求体。

不要复制的问题：

- 新代码里对复杂字符串处理、正则、UI 文本格式化要按实际页面需要拆分，不要塞进 API 接入规范。

## ViewModel 中 request

参考：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/rewards/RewardsViewModel.kt`

可复制点：

- 后台加载列表/状态时用 `request(apiBlock = { ... }, onSuccess = { ... })`。
- API 成功后更新 `ObservableField`、adapter 数据、事件流。
- 用户触发但不需要全屏 loading 的操作，也可以使用 `request(...)`。

注意：

- 广告轮询属于异步任务分支，不属于普通 API 接入模板。

## response(...) 直接处理

参考：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/continuewriting/ContinueWritingViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/MyWorksViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/home/HomeViewModel.kt`

适用：

- 已经在 `launchWithLoading` 或自定义协程里。
- 一个方法里组合多个接口、数据库或 SSE 状态。
- 历史代码周边已使用 `response(...)`。

新代码优先考虑 `request(...)` / `requestWithLoading(...)`，除非组合逻辑更适合 `response(...)`。

## 功能 Repository 示例

参考：

- `app/src/main/java/com/kuaigeglobal/epnova/feature/series/SeriesRepository.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/data/CreationRepository.kt`

可复制点：

- Repository 继承 `AppRepository` 或 `CommonRepository`。
- 简单封装使用 `withContext(Dispatchers.IO) { api.xxx(body) }`。
- 只有复用、组合、缓存、复杂请求构造时才新增 Repository 方法。

## 分页接口示例

参考：

- `ApiService.userPlayHistoryPage(...)`
- `ApiService.homeWaterfallPage(...)`
- `ApiService.workAssetRoleImagePage(...)`

规则：

- 返回 `ApiResponse<PagedList<T>?>`。
- 页面层实现必须走 `android-paging-state-ui`。
- 新 `PagingSource` 不要默认直接用 `ApiFactory.apiService`，优先传入 `mRepo.api`。

## SSE 接口示例

参考：

- `ApiService.sseContentOptimize(...)`
- `ApiService.sseContentAIExpand(...)`
- `ApiService.singleWorkGenerate(...)`

规则：

- 返回 `Flow<SSEvent>`。
- 需要 `@Headers("Accept: text/event-stream", "Cache-Control: no-cache", "Connection: keep-alive")`。
- 具体处理器参考 SSE/异步任务规范，不在普通 API request skill 中展开。

## 常用搜索命令

```powershell
rg -n "接口路径|常量名|方法名|Body名|Data名|Info名" app/src/main/java/com/kuaigeglobal/epnova/core/network/apis app/src/main/java/com/kuaigeglobal/epnova/feature -g "!**/OssManager.kt"
rg -n "requestWithLoading\\(|request\\(|response\\(|mRepo\\.api\\." app/src/main/java/com/kuaigeglobal/epnova/feature -g "!**/OssManager.kt"
rg -n "@POST\\(ApiConstants\\.|ApiResponse<|PagedList<|Flow<SSEvent>" app/src/main/java/com/kuaigeglobal/epnova/core/network/apis
```
