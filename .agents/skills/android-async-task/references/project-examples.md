# 项目异步索引

仅在完整异步 Skill 已命中且当前 feature 没有足够同类实现时读取。

| 要确认的能力 | 搜索入口 |
| --- | --- |
| 轮询 helper | `launchPolling`、`PollingState` |
| SSE 生命周期 | `SSEStreamHandler`、`SseChunkProcessor`、`SSEvent` |
| 下载所有者 | `DownloadService`、`HttpDownload`、`DownloadProgressEvent` |
| 进度模拟 | `ProgressSimulator`、`AutoProgressSimulator` |
| 协程清理 | `onCleared`、`Job?.cancel`、`CancellationException` |

```powershell
rg -n "launchPolling|PollingState|SSEStreamHandler|SseChunkProcessor" app/src/main/java
rg -n "DownloadService|HttpDownload|DownloadProgressEvent|ProgressSimulator" app/src/main/java
rg -n "private var .*Job|onCleared\(|CancellationException" app/src/main/java
```

只读取目标任务和一个同所有者的完整示例；不要把页面 Job 与 Service/系统任务混为同一种生命周期。
