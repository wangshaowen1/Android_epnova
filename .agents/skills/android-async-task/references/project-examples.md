# 项目示例索引

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

本文件记录异步长任务 skill 的项目依据。生成轮询、SSE、下载、生成任务时优先对齐这些写法。

## 核心扩展

- `app/src/main/java/com/kuaigeglobal/epnova/core/ext/ViewModelExt.kt`
  - `request(...)`：无 loading 的接口请求。
  - `requestWithLoading(...)`：接口请求 + loading + toast/error 回调。
  - `launchWithLoading(...)`：自定义协程 + loading。
  - `PollingState`：`SUCCESS`、`FAIL`、`CONTINUE`。
  - `launchPolling(...)`：后端任务状态轮询；内部使用 `viewModelScope`、IO 切换、`isActive`，并放行 `CancellationException`。

## 轮询示例

- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/generate/DramaGenerateStep1ViewModel.kt`
  - `private var novelSummaryPollingJob: Job? = null`。
  - `pollSummary()` 中先判断 `if (novelSummaryPollingJob?.isActive == true) return`。
  - 轮询前设置 `isSummaryLoading`、`isBalanceFail`、`nextStepEnabled`、`storySummaryResult`。
  - `launchPolling(...)` 根据 `isSuccess()`、`isSensitive()`、`isFail()`、`isNoPower()` 映射 `PollingState`。
  - `onCleared()` 取消 `novelSummaryPollingJob`。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/generate/DramaGenerateStep2ViewModel.kt`
  - `private var scriptPollingJob: Job? = null`。
  - 创建剧本任务成功后 `resetPollingState()` 并进入轮询。
  - `pollNovelScript()` 中防重复，`onSuccess` / `onFail` 分别收敛到 `handlePollingSuccess(...)` / `handlePollingFail(...)`。
  - `onCleared()` 取消 `scriptPollingJob`。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/AssetManagementViewModel.kt`
  - 资产描述生成保存 `loopAllAssetDescriptionStatusJob`。
  - 页面暂停取消 `loopNextEnabledJob`，销毁取消任务并清理 item。
  - 手动递归轮询任务状态，失败时设置 `LoadStateViewHolder.RETRY`。

## SSE 示例

- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/CreationViewModel.kt`
  - 保存 `private var sseHandler: SSEStreamHandler? = null`。
  - `sseGenerate(...)` 开始前 `sseHandler?.cancel()`。
  - `SSEStreamHandler(scope = viewModelScope, processor = PlainTextSseProcessor(...))`。
  - `onStart` 清空输入，`onUpdate` 更新 `input`，`onComplete` 和 `onErrorCallback` 关闭 `promptGenerating`。
  - `onCleared()` 调用 `sseHandler?.cancel()`。
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/sse/SSEStreamHandler.kt`
  - 内部保存 `private var job: Job? = null`。
  - `start(flow)` 开始前 `cancel()`，使用 `AtomicBoolean` 防止重复完成/错误回调。
  - `CancellationException` 作为正常取消处理。
  - `cancel()` 中设置结束状态、`_isStreaming=false`、取消 job 并置空。
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/SseCallAdapter.kt`
  - Retrofit SSE 返回 `Flow<SSEvent>`。
  - `awaitClose` 中取消底层 `EventSource`。

## 下载与导出示例

- `app/src/main/java/com/kuaigeglobal/epnova/feature/download/DownloadingViewModel.kt`
  - `DownloadStatusHolder` 定义 `downloadProgress` 和 `cancelDownload()`。
  - `DownloadStatusHolderFactory` 由 Activity/Fragment 提供当前 holder。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/download/DownloadingDialog.kt`
  - Dialog 只展示进度，从宿主拿 `DownloadStatusHolder`，不直接启动下载。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/download/DownloadTask.kt`
  - `DownloadTask.download()` 回调 `onDownloadStart`、`onDownloadProgress`、`onDownloadSuccess`、`onDownloadFail`。
  - `cancel()` 清空 listener、复位 generating、取消底层 call。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/EditStoryboardVideoViewModel.kt`
  - 实现 `DownloadingViewModel.DownloadStatusHolder`。
  - 保存 `downloadJob`，`download(...)` 中用 `if (downloading.value == true) return` 防重复。
  - 成功和错误路径都置空 `downloadJob`、复位 `downloading`。
  - `onCleared()` 调用 `cancelDownload()`。

## 本地循环与合成示例

- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/EditStoryboardVideoViewModel.kt`
  - `progressJob` 用 `while (isActive)` 每 200ms 更新播放器字幕。
  - `loopSynthesisStatusJob` 检查 `VideoSynthesisTask` 状态，成功关闭 dialog 并结束，失败 toast，生成中 delay 后继续。
  - `onCleared()` 取消 `progressJob`、`loopSynthesisStatusJob`，释放播放器，并清理 adapter item。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/ItemStoryboardVM.kt`
  - item 内部保存 `loopGenStatusJob`、`loopSubtitleGenStatusJob`。
  - `clear()` 设置 `cleared = true` 并取消两个 Job。
  - 宿主 ViewModel 销毁时遍历 `adapter.items.forEach { it.clear() }`。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/VideoSynthesisTask.kt`
  - 本地视频合成任务有 `cancel()`，合成前取消旧 transform，失败时记录 reason。
- `app/src/main/java/com/kuaigeglobal/epnova/feature/editor/VideoSynthesisManager.kt`
  - 管理合成队列，存在同 story 的旧任务时先 cancel。

## 搜索命令

```powershell
rg -n "launchPolling|PollingState|Job\\?|onCleared|SSEStreamHandler|DownloadStatusHolder|downloadJob|while \\(isActive\\)" app/src/main/java/com/kuaigeglobal/epnova -g "*.kt" -g "*.java" -g "!**/OssManager.kt"
rg -n "retryOnNetworkError|checkState|onPolling|onSuccess|onFail" app/src/main/java/com/kuaigeglobal/epnova/feature -g "*.kt"
rg -n "SSEStreamHandler|PlainTextSseProcessor|StorySseProcessor|EpisodeSseProcessor|SseCallAdapter" app/src/main/java/com/kuaigeglobal/epnova -g "*.kt"
```
