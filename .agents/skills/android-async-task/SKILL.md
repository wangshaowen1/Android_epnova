---
name: android-async-task
description: "Android 异步长任务规范。仅在本次实际涉及轮询、polling、launchPolling、SSE、流式生成、生成任务、视频合成、任务状态检查、下载任务、Job 保存、onCleared 取消、重复启动保护、取消逻辑、Coroutine 长任务、DownloadTask、DownloadingDialog 或 SSEStreamHandler 时使用。普通 loading/error 文案或一次性接口请求不要触发本 skill。"
---

# Android 异步任务

## 快速决策

按任务类型选择项目已有能力：

- 一次性接口请求：使用 `request(...)` 或 `requestWithLoading(...)`，不需要读取本 skill。
- 用户点击触发且需要 loading：使用 `requestWithLoading(...)` 或 `launchWithLoading(...)`。
- 后端任务状态轮询：优先使用 `launchPolling(...)`，保存返回的 `Job`。
- SSE 流式文本生成：使用 `SSEStreamHandler` + 对应 `SseChunkProcessor`，不要手写 OkHttp SSE。
- 下载/导出视频：复用 `DownloadTask`、`DownloadingDialog`、`DownloadingViewModel.DownloadStatusHolder`。
- 本地播放器进度、视频合成状态、临时 UI 进度模拟：可以保存普通 `Job`，用 `while (isActive)` 或延迟递归，但必须有取消入口。

只有找不到同类实现、不确定项目异步封装用法、需要新增完整长任务，或发现轮询/SSE/下载生命周期问题时，才读取 [project-examples.md](references/project-examples.md) 对齐项目示例。

## Job 管理

长任务必须有明确生命周期。

- ViewModel 内保存为 `private var xxxJob: Job? = null`。
- 启动前先判断重复：持续任务通常 `if (xxxJob?.isActive == true) return`；互斥任务通常先 `xxxJob?.cancel()` 再重新启动。
- `onCleared()` 必须取消所有保存的 `Job`、SSE handler、下载任务、播放器/合成器等资源。
- 取消后把字段置空，并复位 `loading`、`downloading`、`generating`、按钮可用状态等 UI 状态。
- `CancellationException` 必须放行，不要当成普通错误吞掉。
- 一次性简单接口请求沿用附近 `request(...)`、`requestWithLoading(...)` 或 `launch { response(...) }`，不需要为了统一格式升级成本 skill。
- 需要状态回滚、自定义异常提示或 `savingIds`、loading/progress 等必然清理时，使用 `launch(block = { response(...) }, onError = { ... }, onComplete = { ... })`。
- 不要在 `launch` 内手写 `try/catch/finally` 包网络请求，除非有明确特殊原因；接口业务失败放 `response.error`，网络/协程异常放 `launch.onError`，临时状态清理放 `launch.onComplete`。
- 不在 Activity/Fragment 里持有长任务逻辑；UI 只展示 loading、progress、toast、dialog、状态页。

## 轮询任务

后端返回任务状态时，优先使用 `launchPolling(...)`。

```kotlin
private var scriptPollingJob: Job? = null

fun pollScript() {
    if (scriptPollingJob?.isActive == true) return
    isScriptLoading.set(true)

    scriptPollingJob = launchPolling(
        intervalMs = 3000L,
        retryOnNetworkError = true,
        apiBlock = { mRepo.api.pollScriptResult(PollScriptBody(workId.value, storyId)) },
        checkState = { response ->
            when {
                response?.data?.isSuccess() == true -> PollingState.SUCCESS
                response?.data?.isFail() == true -> PollingState.FAIL
                else -> PollingState.CONTINUE
            }
        },
        onSuccess = { response -> handlePollingSuccess(response?.data) },
        onFail = { response, _ -> handlePollingFail(response?.data) },
        onPolling = { updatePollingUi(it?.data) }
    )
}

override fun onCleared() {
    scriptPollingJob?.cancel()
    super.onCleared()
}
```

轮询规则：

- `PollingState.SUCCESS`：停止轮询，写入结果，关闭 loading，开放下一步。
- `PollingState.FAIL`：停止轮询，关闭 loading，展示失败状态或重试入口。
- `PollingState.CONTINUE`：更新进度/文案后等待下一次。
- `retryOnNetworkError = true` 只用于生成任务这种可以继续等的场景；会导致网络异常后继续轮询，不要误用于必须立即失败的提交。
- 任务创建接口和轮询接口分开：创建任务用 `requestWithLoading(...)`，创建成功后重置状态并启动轮询。

## SSE 流式生成

项目已有 `SSEStreamHandler`，新流式生成优先复用它。

- ViewModel 保存 `private var sseHandler: SSEStreamHandler? = null`。
- 开启新流前先 `sseHandler?.cancel()`，防止旧流继续回调。
- 使用 `viewModelScope` 作为 `SSEStreamHandler` 的 scope。
- 文本拼接、节流、字段解析放到 `PlainTextSseProcessor`、`StorySseProcessor`、`EpisodeSseProcessor` 或新增 processor。
- `onStart` 清空旧内容并设置生成中状态。
- `onUpdate` 只更新已被 XML 或观察者消费的 `LiveData` / `ObservableField`；内部拼接、状态机、临时结果优先用普通字段，必要时再一次性写回 UI 状态。
- `onComplete` 关闭生成中状态并开放后续按钮。
- `onErrorCallback` 关闭生成中状态，使用 `parseMessage(e)?.showToast()` 或页面错误态。
- `onCleared()` 中调用 `sseHandler?.cancel()`。

不要在页面里直接 new `EventSource` 或手写 callbackFlow；底层已经由 `SseCallAdapter` 和 `SSEStreamHandler` 收口。

## 下载任务

视频保存、资源下载、导出类任务优先按项目下载模式实现。

- 页面 ViewModel 实现 `DownloadingViewModel.DownloadStatusHolder`，提供 `downloadProgress` 和 `cancelDownload()`。
- Activity/Fragment 实现 `DownloadStatusHolderFactory`，通过 `DownloadingDialog.show(this)` 展示进度。
- 下载前用 `downloading.value == true` 防重复。
- 保存 `downloadJob` 或 `DownloadTask`，取消时停止任务并复位状态。
- 进度用 `ObservableInt` 或项目已有状态 holder，不要让 Dialog 自己发起下载。

```kotlin
private var downloadJob: Job? = null
val downloading = MutableLiveData(false)
override val downloadProgress = ObservableInt()

override fun cancelDownload() {
    downloadJob?.cancel()
    downloadJob = null
    downloading.value = false
}
```

## 本地循环与视频合成

无法直接套 `launchPolling(...)` 的本地任务，也要保存和取消。

- 播放器进度刷新使用 `while (isActive) { ... delay(...) }`，保存 `progressJob`，暂停/销毁时取消。
- 视频合成这类本地队列任务保存 `synthesisTask` 和 `loopSynthesisStatusJob`，每次启动前取消旧 loop。
- 递归 delay 轮询必须有退出条件，不能无限递归；优先用 `isActive` 或任务状态判断。
- 任务失败要关闭 Dialog/Loading，并展示 `reason` 或可理解错误文案。
- 页面销毁时释放播放器、合成任务、adapter item 内部 Job。

## 状态与错误

- loading/error/content 走项目已有 `ObservableBoolean`、`ObservableInt`、`LoadStateViewHolder`、`MutableLiveData` 或轻量状态对象；只有 UI 需要自动刷新时才新增 Observable/LiveData。
- 每个长任务至少明确这几种状态：未开始、进行中、成功、失败、取消。
- 失败时保留重试入口，重试前重置失败状态和旧 `Job`。
- 需要扣算力的生成任务，余额不足走项目已有 `PowerRechargeDialog` / `showRechargeDialog(...)`。
- 跨页面生成完成通知使用 `Events` + `FlowEventBus`，但页面内部进度不要用全局事件。

## 审查清单

完成异步任务前检查：

- 是否使用了项目已有 `requestWithLoading`、`launchPolling`、`SSEStreamHandler`、`DownloadTask`，没有重复造轮询/SSE/下载框架。
- 每个长任务是否保存 `Job` 或 handler，并在 `onCleared()` 取消。
- 是否有重复启动保护：`isActive` return、先 cancel、或 `downloading/generating` 标记。
- 成功、失败、异常、取消路径是否都复位 loading/progress/generating。
- `CancellationException` 是否放行。
- Activity/Fragment 是否只负责展示，不直接持有网络轮询或下载业务。
- Adapter item 内部启动的任务是否有 `clear()` 并由宿主 ViewModel 清理。
