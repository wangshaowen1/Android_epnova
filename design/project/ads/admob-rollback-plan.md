# AdMob 激励广告回退方案

## 背景

Unity Ads 当前不可用，需要回到 AdMob 激励广告。

本次只允许修改广告 SDK 和广告相关接口逻辑，不改非广告业务流程、不改 UI 样式、不做无关重构。

历史依据：

- `310cbbf [Feature] 1.1.0 广告集成平台隔离`：新增 `IRewardedAdPlatform`、`AdmobRewardedAdPlatform`，`RewardedAdManager` 默认使用 AdMob。
- `cf3f471 [Feature] 1.2.0 Unity ads 广告接入 接口未更新`：把 `play-services-ads` 换成 `unity-ads`，把 `MobileAds.initialize` 换成 Unity 初始化，把默认平台从 AdMob 改为 Unity。
- `fe93834 [Feature] 1.2.0 广告接口`：把广告接口改成当前 `/ad/reward/.../prepare` 与 `/ad/reward/.../complete`。

用户补充的接口截图确认：当前 `/ad/reward/.../complete` 不是客户端主动发奖接口，而是 AdMob SSV 处理状态查询接口。字段 `adFlowStatus` 表示服务端是否已完成 AdMob SSV 验签回调后的业务处理，不代表 HTTP 请求状态。

因此，本次不应回退到更早的 `app/story/unlock/ad/result`、`reward/task/ad/preReport`、`reward/task/ad/pollResult` 旧接口。正确方案是保留当前 `/ad/reward/...` 接口，只把 SDK、初始化和默认广告平台切回 AdMob，并把注释语义从“complete 确认”修正为“complete 轮询查询”。

已确认约束：

- 接口文档中标为 `long` 的 ID，客户端一律继续用 `String` 接收和提交。
- 请求头由项目公共链路处理，本次广告回退不处理 header。
- AdMob 后台已配置正确的 SSV callback URL。
- 生产广告位 `ca-app-pub-9071858947673651/4907921363` 仍有效。
- 服务端保证 `nonce` 在 AdMob SSV 回调的 `custom_data` 中按原值解析并完成业务处理。
- `/ad/reward/.../complete` 轮询接口在 `adFlowStatus=0` 时一定返回 HTTP 200，客户端按业务字段继续轮询。

## 接口匹配结论

用户提供的 AdMob 接口与当前代码大体匹配。

| 场景 | 接口文档 | 当前代码 | 结论 |
| --- | --- | --- | --- |
| 剧集解锁准备 | `POST /ad/reward/story/unlock/prepare`，返回 `data.nonce`，客户端把 `nonce` 写入 AdMob `customData` | `AD_REWARD_STORY_UNLOCK_PREPARE` + `storyUnlockAdPrepare(...)` + `RewardAdPrepareData.nonce` | 匹配 |
| 剧集解锁结果查询 | `POST /ad/reward/story/unlock/complete`，body `workId/storyId/nonce`，返回 `adFlowStatus` | `AD_REWARD_STORY_UNLOCK_COMPLETE` + `storyUnlockAdComplete(...)`，`RewardedAdManager` repeat 轮询 | 匹配 |
| 广告任务准备 | `POST /ad/reward/task/prepare`，body `taskRecordId`，返回 `data.nonce`，客户端把 `nonce` 写入 AdMob `customData` | `AD_REWARD_TASK_PREPARE` + `rewardAdTaskPrepare(...)` | 匹配 |
| 广告任务结果查询 | `POST /ad/reward/task/complete`，body `nonce`，返回 `adFlowStatus` | `AD_REWARD_TASK_COMPLETE` + `rewardAdTaskComplete(...)`，`RewardsViewModel` repeat 轮询 | 匹配 |
| 签到广告准备 | `POST /ad/reward/checkin/prepare`，返回 `data.nonce`，客户端把 `nonce` 写入 AdMob `customData` | `AD_REWARD_CHECKIN_PREPARE` + `rewardAdCheckInPrepare(...)` | 匹配 |
| 签到广告结果查询 | `POST /ad/reward/checkin/complete`，body `nonce`，返回 `adFlowStatus` | `AD_REWARD_CHECKIN_COMPLETE` + `rewardAdCheckInComplete(...)`，`RewardsViewModel` repeat 轮询 | 匹配 |
| SSV customData | 接口说明要求客户端将 `nonce` 设置到 AdMob `customData` | `RewardedAdManager` 当前把 `nonce` 传给平台；`AdmobRewardedAdPlatform` 的 `load(..., customData)` 会执行 `setCustomData(customData)` | 切到 AdMob 后匹配 |

当前代码不匹配的点集中在 SDK 和平台选择：

- `app/build.gradle` 当前是 Unity Ads 依赖，不是 Google Mobile Ads SDK。
- `App.kt` 当前初始化 Unity Ads，不是 `MobileAds.initialize(...)`。
- `RewardedAdManager` 默认平台是 `UnityRewardedAdPlatform()`，不是 `AdmobRewardedAdPlatform()`。
- `RewardedAdManager`、`RewardsViewModel` 等注释把 `/complete` 描述成“完成确认”，需要修正成“结果查询/轮询”，避免后续误解。

## 接口细节

### 剧集解锁激励视频广告前置准备

```text
POST /ad/reward/story/unlock/prepare
```

说明：用户点击广告解锁剧集前调用，服务端生成剧集解锁广告流水并返回 `nonce`，客户端需要把 `nonce` 设置到 AdMob `customData`。

Body：

- `workId: String?`，可选，作品 ID。接口文档标为 `long`，客户端按确认约束用 `String`。
- `storyId: String?`，可选，剧集 ID。接口文档标为 `long`，客户端按确认约束用 `String`。

响应：

- `data.nonce: string`，本次广告唯一标识，即 `adFlowId`

### 剧集解锁激励视频广告结果查询

```text
POST /ad/reward/story/unlock/complete
```

说明：客户端轮询查询 AdMob SSV 处理状态，剧集解锁只能由服务端验签回调完成。

Body：

- `workId: String?`，可选，作品 ID。接口文档标为 `long`，客户端按确认约束用 `String`。
- `storyId: String?`，可选，剧集 ID。接口文档标为 `long`，客户端按确认约束用 `String`。
- `nonce: string`，必填，广告流水 ID，由 prepare 接口返回

响应：

- `data.adScene: int`，广告场景，`1=签到激励广告`，`2=福利任务激励广告`，`3=剧集解锁激励广告`
- `data.adFlowStatus: int`，广告流水处理状态，`0=待完成`，`1=已完成`，`2=已失效`
- `data.unlockStoryIds: array<long>`，本次成功解锁的剧集 ID 列表，仅 `adScene=3` 且 `adFlowStatus=1` 时返回
- `data.adUnlockRemainCount: int`，今日该作品广告解锁剩余次数，仅 `adScene=3` 时返回

### 广告任务激励视频广告前置准备

```text
POST /ad/reward/task/prepare
```

说明：用户点击福利任务激励视频广告前调用，服务端生成福利任务广告流水并返回 `nonce`，客户端需要把 `nonce` 设置到 AdMob `customData`。

Body：

- `taskRecordId: String?`，可选，福利广告任务记录 ID，即任务列表返回的 `taskRecordId`。接口文档标为 `long`，客户端按确认约束用 `String`。

响应：

- `data.nonce: string`，本次广告唯一标识，即 `adFlowId`

### 广告任务激励视频广告结果查询

```text
POST /ad/reward/task/complete
```

说明：客户端轮询查询 AdMob SSV 处理状态，奖励发放只能由服务端验签回调完成。

Body：

- `nonce: string`，必填，广告流水 ID，由对应场景的 prepare 接口返回

响应字段同“剧集解锁激励视频广告结果查询”。

### 签到任务激励视频广告前置准备

```text
POST /ad/reward/checkin/prepare
```

说明：用户点击签到激励视频广告前调用，服务端生成签到广告流水并返回 `nonce`，客户端需要把 `nonce` 设置到 AdMob `customData`。

响应：

- `data.nonce: string`，本次广告唯一标识，即 `adFlowId`

### 签到任务激励视频广告结果查询

```text
POST /ad/reward/checkin/complete
```

说明：客户端轮询查询 AdMob SSV 处理状态，奖励发放只能由服务端验签回调完成。

Body：

- `nonce: string`，必填，广告流水 ID，由对应场景的 prepare 接口返回

响应字段同“剧集解锁激励视频广告结果查询”。

## 目标链路

AdMob 客户端只负责加载广告、展示广告、接收 `OnUserEarnedReward`。是否真正解锁或发奖，以服务端收到 AdMob SSV 回调后的轮询结果为准。

### 剧集广告解锁

1. 调用 `storyUnlockAdPrepare(WorkBody(workId, storyId))`。
2. 服务端返回 `nonce`。
3. 客户端加载 AdMob 激励广告，并执行 `ServerSideVerificationOptions.setCustomData(nonce)`。
4. 用户完整观看广告触发 `OnUserEarnedReward`。
5. 客户端轮询 `storyUnlockAdComplete(StoryUnlockAdCompleteBody(workId, storyId, nonce))`。
6. `adFlowStatus = 0` 继续轮询；`1` 解锁成功；`2` 失败或失效。

### 福利任务广告

1. 调用 `rewardAdTaskPrepare(RewardAdTaskPrepareBody(taskRecordId))`。
2. 服务端返回 `nonce`。
3. 客户端加载 AdMob 激励广告，并执行 `ServerSideVerificationOptions.setCustomData(nonce)`。
4. 用户完整观看广告触发 `OnUserEarnedReward`。
5. 客户端轮询 `rewardAdTaskComplete(RewardAdCompleteBody(nonce))`。
6. `adFlowStatus = 0` 继续轮询；`1` 发奖成功；`2` 失败或失效。

### 签到广告

1. 调用 `rewardAdCheckInPrepare(UserTokenBody())`。
2. 服务端返回 `nonce`。
3. 客户端加载 AdMob 激励广告，并执行 `ServerSideVerificationOptions.setCustomData(nonce)`。
4. 用户完整观看广告触发 `OnUserEarnedReward`。
5. 客户端轮询 `rewardAdCheckInComplete(RewardAdCompleteBody(nonce))`。
6. `adFlowStatus = 0` 继续轮询；`1` 发奖成功；`2` 失败或失效。

## 当前代码保留项

这些接口逻辑当前已经符合用户贴出的 AdMob 接口，不应回退到旧路径：

- `ApiConstants.AD_REWARD_STORY_UNLOCK_PREPARE = "ad/reward/story/unlock/prepare"`
- `ApiConstants.AD_REWARD_STORY_UNLOCK_COMPLETE = "ad/reward/story/unlock/complete"`
- `ApiConstants.AD_REWARD_CHECKIN_PREPARE = "ad/reward/checkin/prepare"`
- `ApiConstants.AD_REWARD_CHECKIN_COMPLETE = "ad/reward/checkin/complete"`
- `ApiConstants.AD_REWARD_TASK_PREPARE = "ad/reward/task/prepare"`
- `ApiConstants.AD_REWARD_TASK_COMPLETE = "ad/reward/task/complete"`
- `RewardAdPrepareData(nonce)`
- `RewardAdCompleteData(adScene, adFlowStatus, unlockStoryIds, adUnlockRemainCount)`
- `StoryUnlockAdCompleteBody(workId, storyId, nonce)`
- `RewardAdTaskPrepareBody(taskRecordId)`
- `RewardAdCompleteBody(nonce)`
- `RewardedAdManager.completeAdResult()` 的 repeat 轮询结构
- `RewardsViewModel.onAdWatched(...)` 和 `onCheckInAdWatched(...)` 的 repeat 轮询结构

类型说明：

- 接口文档里 `workId/storyId/taskRecordId` 是 `long`，当前项目请求体统一使用 `String?`。这是已确认约束，后续实现不得因为文档标 `long` 改成 `Long`。

## 需要修改

### Gradle

恢复 Google Mobile Ads SDK，移除 Unity Ads SDK。

```gradle
// AdMob 激励广告 SDK
implementation 'com.google.android.gms:play-services-ads:25.2.0'
```

删除：

```gradle
implementation 'com.unity3d.ads:unity-ads:...'
```

如果暂时不删除 `UnityRewardedAdPlatform.kt`，就不能移除 Unity 依赖，否则会编译失败。推荐同时移除 Unity 平台实现文件，保持 SDK 单一。

### Manifest

当前已有 AdMob App ID，先保留：

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-9071858947673651~9374017668" />
```

需要确认该 App ID 与当前线上包名、AdMob 后台应用一致。

### App 初始化

隐私同意后、主进程内恢复 AdMob 初始化：

```kotlin
if (ProcessUtils.isMainProcess()) {
    ioScope.launch {
        MobileAds.initialize(instance) {
        }
    }
}
```

删除 Unity 初始化和 Unity import。

### RewardedAdManager

默认平台改回 AdMob：

```kotlin
private val platform: IRewardedAdPlatform = AdmobRewardedAdPlatform()
```

注释需要修正：

- `complete 服务端确认` 改为 `complete 接口轮询查询 AdMob SSV 处理状态`
- `nonce` 说明补充为 `由 prepare 返回，并写入 AdMob customData`

### IRewardedAdPlatform

当前接口参数名是 `nonce`，AdMob 实现参数名是 `customData`。逻辑上可用，但语义容易误解。

推荐改名为 `customData` 或 `ssvCustomData`：

```kotlin
fun load(
    context: Context,
    customData: String?,
    onLoaded: () -> Unit,
    onFailed: () -> Unit
)
```

调用侧继续传 `nonce`，因为接口文档明确要求把 `nonce` 写入 AdMob `customData`。

### AdmobRewardedAdPlatform

沿用当前已有实现：

- Debug 广告位：`ca-app-pub-3940256099942544/5224354917`
- Release 广告位：`ca-app-pub-9071858947673651/4907921363`
- `RewardedAd.load(...)`
- `ServerSideVerificationOptions.Builder().setCustomData(customData)`
- `show(...)` 中用 `OnUserEarnedRewardListener` 触发业务层轮询

需要确认生产广告位和 AdMob SSV 回调仍可用。

### RewardsViewModel

接口逻辑保留，注释修正：

- `complete 确认结果` 改成 `complete 结果查询/轮询`
- `重复确认` 改成 `重复查询`
- `nonce` 注释补充为 `prepare 返回，已写入 AdMob customData`

## 文件清单

预计修改：

- `app/build.gradle`
- `app/src/main/java/com/kuaigeglobal/epnova/App.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/manager/RewardedAdManager.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/manager/ad/IRewardedAdPlatform.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/manager/ad/UnityRewardedAdPlatform.kt`：推荐删除或停止编译
- `app/src/main/java/com/kuaigeglobal/epnova/feature/rewards/RewardsViewModel.kt`：只改广告注释/命名语义
- `app/src/main/java/com/kuaigeglobal/epnova/feature/play/more/AdUnlockDialog.kt`：只改 Unity 注释为 AdMob 注释

通常不需要改：

- `app/src/main/AndroidManifest.xml`：已有 AdMob App ID，仅核对
- `ApiConstants.kt`
- `ApiService.kt`
- `RewardAdData.kt`
- `RewardAdBody.kt`
- `RewardsActivity.kt`

不应修改：

- 非广告页面 UI、layout、drawable、文案样式
- 非广告接口、非广告 Repository/ViewModel
- 支付、登录、创作、下载、OssManager
- 广告任务列表和签到列表接口本身
- 旧接口 `app/story/unlock/ad/result`、`reward/task/ad/preReport`、`reward/task/ad/pollResult`

## 实施顺序

1. 恢复 AdMob SDK 依赖。
2. `App.kt` 改回 `MobileAds.initialize(...)`。
3. `RewardedAdManager` 默认平台切回 `AdmobRewardedAdPlatform`。
4. 将 `IRewardedAdPlatform.load` 的参数语义改清楚为 `customData` 或 `ssvCustomData`，调用侧传 prepare 返回的 `nonce`。
5. 删除或停用 `UnityRewardedAdPlatform.kt` 和 Unity import。
6. 修正广告注释：`complete` 是结果查询/轮询，不是客户端发奖确认。
7. 静态检查 Unity 残留、AdMob SDK、接口路径和轮询逻辑。

## 静态检查

默认不跑 Gradle，按项目约定先做静态检查：

```powershell
rg -n "UnityAds|UnityRewardedAdPlatform|unity-ads|com\.unity3d" app build.gradle
rg -n "MobileAds|AdmobRewardedAdPlatform|play-services-ads|ServerSideVerificationOptions|setCustomData" app build.gradle
rg -n "AD_REWARD_STORY_UNLOCK_PREPARE|AD_REWARD_STORY_UNLOCK_COMPLETE|AD_REWARD_CHECKIN_PREPARE|AD_REWARD_CHECKIN_COMPLETE|AD_REWARD_TASK_PREPARE|AD_REWARD_TASK_COMPLETE" app/src/main/java
rg -n "app/story/unlock/ad/result|reward/task/ad/preReport|reward/task/ad/pollResult|rewardAdPreReport|rewardAdPoll|StoryUnlockAdResultBody" app/src/main/java
```

期望结果：

- 第一条无业务残留。
- 第二条能命中 AdMob SDK、初始化和 SSV customData。
- 第三条能命中当前 `/ad/reward/...` 接口常量。
- 第四条无旧接口残留。

## 待确认

暂无。SSV callback、生产广告位、`nonce` 解析和 `adFlowStatus=0` 返回 HTTP 200 已由用户确认。
