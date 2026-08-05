---
name: android-async-task
description: "Android 异步长任务规范。仅在本次实际涉及轮询、polling、launchPolling、SSE、流式接口、支付状态检查、任务状态检查、下载任务、Job 保存、onCleared 取消、重复启动保护、取消逻辑、Coroutine 长任务、DownloadService、HttpDownload 或 SSEStreamHandler 时使用。普通 loading/error 文案或一次性接口请求不要触发本 skill。"
---

# Android 异步任务

## 能力选择

| 任务 | 优先能力 |
| --- | --- |
| 一次性请求 | `request(...)` / `requestWithLoading(...)`，不走本 skill |
| 后端任务状态轮询 | `launchPolling(...)`，保存返回的 Job |
| SSE 流式结果 | `SSEStreamHandler` + 对应 `SseChunkProcessor` |
| APK/持久文件下载 | `DownloadService`、`HttpDownload`、现有进度事件 |
| 播放器进度、本地模拟、短轮询 | 保存普通 Job，使用 `while (isActive)` 或有界重试 |

只有附近实现不足、需要确认项目封装或生命周期时，才读取 [项目异步索引](references/project-examples.md)。

## 先确定所有者

- Activity/Fragment 不实现网络轮询、SSE 或持久下载状态机。
- ViewModel 创建的页面级 Job、SSE handler、临时下载在 `onCleared()` 取消。
- View 自己拥有的播放器、动画、逐帧进度可以留在 UI 层，但必须在当前基类和 View 缓存策略对应的真实 View 生命周期释放。
- Service、系统 DownloadManager、应用级 Manager 或持久队列拥有的任务可跨页面继续；页面只用任务 ID 解绑/重绑观察，除非用户主动取消，不在 `onCleared()` 停止底层任务。
- Adapter item 内部任务必须有明确 `clear()`，并由实际宿主在回收或销毁路径调用。

所有长任务启动前选择一种重复策略：仍在执行则 return，或先 cancel 再启动。结束后清空引用；页面级任务的成功、失败、异常和取消都要复位 loading/generating/progress 等临时状态。

## 轮询

- 保存 `launchPolling(...)` 返回的 Job。
- `SUCCESS` 写入结果并结束；`FAIL` 展示失败/重试并结束；`CONTINUE` 只更新当前进度后继续。
- `retryOnNetworkError = true` 只用于允许继续等待的后台任务，不用于必须立即失败的提交。
- 创建任务与查询状态分开：创建成功后重置旧状态，再开始轮询。
- 固定次数短轮询先查同类实现，必须有最大次数、退出条件和重复启动保护。

## SSE

- ViewModel 保存单个 handler，新流启动前取消旧 handler，scope 使用 `viewModelScope`。
- 文本拼接、协议字段解析和节流进入已有或新增 processor，不在页面直接创建 EventSource/callbackFlow。
- `onStart` 清旧内容并进入生成中；`onUpdate` 只写 UI 实际消费的状态；`onComplete` 和 error 都退出生成中。
- `onCleared()` 取消 handler。

## 下载与本地循环

- 先区分“页面关闭即取消”的临时下载和“离开页面仍继续”的持久下载。前者由 ViewModel 保存 Job；后者由 Service/系统/Manager 持有，页面保存任务 ID 和观察状态。
- 优先复用现有下载服务、文件校验和进度事件，不在页面再实现 DownloadManager 轮询。
- 播放器进度或本地模拟用 `while (isActive)`，暂停/销毁时取消；递归 delay 必须有退出条件。
- 支付、订单和会员权益的最终结果以服务端状态为准，不只依赖客户端回调。

## 错误与状态

- 只有 UI 需要自动刷新时使用 `Observable*` / `LiveData`；内部状态机和临时结果用普通字段。
- 网络请求的业务失败放 `response.error`，协程异常放 `launch.onError`，必然清理放 `launch.onComplete`。
- 不在普通 `launch` 里再手写请求 `try/catch/finally`；确需捕获时重新抛出 `CancellationException`。
- 页面内部进度不发全局事件；只有真正跨页面的完成通知才考虑现有 Events/FlowEventBus。

## 检查

- 每个任务的所有者、重复启动策略、终态和取消入口明确。
- 页面任务按生命周期取消，持久任务可凭任务 ID 重绑且没有被页面销毁误取消。
- 成功、失败、异常、取消都清理临时状态；`CancellationException` 没被吞掉。
- 已复用现有轮询、SSE、下载能力，没有重复造状态机。
