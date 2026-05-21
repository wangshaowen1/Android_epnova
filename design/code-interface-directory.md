# Epnova 主包代码接口目录

生成时间：2026-05-20  
扫描范围：`app/src/main/java/com/kuaigeglobal/epnova`  
排除范围：`**/OssManager.kt`  
统计结果：927 个 Kotlin/Java 文件，其中 Kotlin 695 个、Java 232 个。

本文用于后续功能查找和实现前定位代码入口。它不是完整 API Reference，而是“先去哪找、再改哪一层”的目录索引。

## 快速查找规则

| 要找什么 | 优先目录/文件 | 说明 |
| --- | --- | --- |
| 页面入口 | `feature/<业务>/*Activity.kt`、`*Fragment.kt`、`*Dialog.kt` | UI、监听、导航、参数组装在这里 |
| 页面状态/业务逻辑 | `feature/<业务>/*ViewModel.kt` | 持有 `LiveData`、`ObservableField`、Adapter、请求编排 |
| 网络封装 | `feature/<业务>/*Repository.kt`、`core/data/CommonRepository.kt` | Repository 只包装网络/数据库/缓存 |
| Retrofit 接口 | `core/network/apis/ApiService.kt` | 所有主业务接口定义集中处 |
| 接口常量 | `core/network/apis/ApiConstants.kt` | URL path 常量集中处 |
| 请求体 | `core/network/apis/request/` | Retrofit `@Body` 对应请求体 |
| 响应体 | `core/network/apis/response/`、`core/network/apis/entity/` | 页面展示和业务判断的数据结构 |
| 路由跳转 | `router/AppRouter.kt`、`router/AppNavigator.kt`、`router/AppLaunchJump.kt` | scheme、登录拦截、push 启动后跳转 |
| 本地数据库 | `core/db/dao/`、`core/db/entity/`、`core/db/AppDatabase.kt` | Room 表、DAO、Paging RemoteMediator 缓存 |
| 三方 SDK 隔离 | `core/manager/`、`core/google/`、`core/facebook/` | 广告、支付、登录、统计、推送等 |
| 通用列表 | `common/adapter/`、`base/item/AppItemViewModel.kt` | QuickAdapter/MultiTypeAdapter/DataBinding item VM |
| 通用扩展 | `common/ext/`、`core/ext/` | View、Context、Paging、网络请求、敏感词、充值弹窗等 |

常用命令：

```powershell
rg "class .*ViewModel" app/src/main/java/com/kuaigeglobal/epnova/feature
rg "suspend fun .*" app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiService.kt
rg "fun (start|show|newInstance)" app/src/main/java/com/kuaigeglobal/epnova/feature
rg "ApiConstants.<接口名>" app/src/main/java/com/kuaigeglobal/epnova
```

## 顶层目录概览

| 目录 | 文件数 | 职责 |
| --- | ---: | --- |
| `core/` | 329 | 网络、数据库、用户态、配置、SSE、支付/广告/统计/登录/推送 SDK 管理、全局业务模型 |
| `feature/` | 258 | 页面和业务功能模块，按首页、创作、播放、剧集、我的、充值、登录、设置等拆分 |
| `widget/` | 195 | 自定义 View、弹窗基础能力、RecyclerView 装饰、刷新控件、富文本/标签/图片展示 |
| `common/` | 114 | 扩展函数、通用 Adapter、权限、工具类、常量、SP、JSBridge、拼音/ROM/图片缓存 |
| `kit/` | 17 | 媒体播放、音频录制、签名工具等偏工具链能力 |
| `base/` | 9 | 项目基类 ViewModel、item VM、模糊弹窗基类、RecyclerView VM |
| `router/` | 3 | App 内 scheme、登录拦截和启动后跳转 |
| `service/` | 1 | 下载服务 |
| `App.kt` | 1 | Application 初始化、生命周期、SDK 初始化入口 |

## 架构接口约定

- Activity 直接继承 `nhsd.basic.BaseViewModelActivity<VB, VM>`。
- Fragment 直接继承 `nhsd.basic.BaseViewModelFragment<VB, VM>`。
- 有 Repository 的 ViewModel 继承 `base/viewmodel/AppRepositoryViewModel<T>`。
- 无 Repository 的 ViewModel 继承 `base/viewmodel/AppViewModel`。
- Repository 继承 `core/AppRepository.kt` 或 `core/data/CommonRepository.kt`。
- `AppRepository` 注入 `ApiFactory.apiService` 和 `AppDatabase.getInstance()`。
- DataBinding 页面变量统一使用 `layout`，列表 item 变量统一使用 `item`。
- 网络请求优先使用 `core/ext/ViewModelExt.kt` 中的 `request(...)`、`requestWithLoading(...)`、`launchPolling(...)`。

## Core 接口索引

| 模块 | 关键文件 | 作用 |
| --- | --- | --- |
| Application | `App.kt` | App 初始化、Debug 状态、生命周期追踪、用户协议后初始化、Unity Ads 等 SDK 初始化 |
| 用户态 | `core/User.kt` | 用户信息、登录/游客状态、token、余额等运行时用户数据 |
| 配置 | `core/Config.kt`、`core/Global.kt` | App 配置、环境、自定义 baseUrl、全局余额刷新等 |
| Repository 基类 | `core/AppRepository.kt`、`core/data/CommonRepository.kt` | 网络与数据库入口封装 |
| 网络工厂 | `core/network/apis/ApiFactory.kt` | 创建 `ApiService`、`EventService` |
| Retrofit 客户端 | `core/network/RetrofitClient.kt` | OkHttp、公共 header、SSE client、Gson、日志、超时配置 |
| SSE | `core/network/SseCallAdapter.kt`、`core/network/sse/`、`core/network/sse/processor/` | SSE Flow 适配、分片处理、文本/JSON/故事/剧集处理器 |
| Room | `core/db/AppDatabase.kt`、`core/db/dao/`、`core/db/entity/` | 本地缓存、PagingSource、RemoteKeys、历史记录 |
| 支付 | `core/google/billing/BillingManager.kt`、`PurchaseResult.kt` | Google Billing 商品加载、下单、消耗、订单状态上报 |
| 广告 | `core/manager/RewardedAdManager.kt`、`core/manager/ad/IRewardedAdPlatform.kt`、`UnityRewardedAdPlatform.kt` | 激励广告平台抽象、加载、展示、释放 |
| 登录 | `core/google/login/GoogleSignInHelper.kt`、`core/facebook/login/FacebookSignInHelper.kt` | Google/Facebook 登录和退出 |
| 推送 | `core/google/messaging/MyFirebaseMessagingService.kt` | FCM 消息处理和通知 |
| 统计 | `core/manager/EventManager.kt`、`core/google/event/AnalyticsHelper.kt`、`AppsFlyerManager.kt` | App 埋点、Firebase、AppsFlyer、GrowingIO |
| 升级 | `core/manager/UpgradeManager.kt` | 版本检查、强更/普通更新弹窗和下载 |
| 语言 | `core/manager/LocaleHelper.kt` | Context 语言包装 |
| 设备指纹 | `core/manager/OverseasDeviceFingerprint.kt` | 海外设备指纹生成 |

## 网络接口索引

### 网络入口

| 文件 | 内容 |
| --- | --- |
| `core/network/apis/ApiService.kt` | 主业务 Retrofit 接口，约 120 个方法 |
| `core/network/apis/EventService.kt` | `eventTracking(...)`，独立埋点服务 |
| `core/network/apis/ApiConstants.kt` | 接口 path、baseUrl、业务常量 |
| `core/network/apis/request/` | 请求体，100 个文件 |
| `core/network/apis/response/` | 响应包装/页面响应，42 个文件 |
| `core/network/apis/entity/` | 核心业务实体，86 个文件 |

### ApiService 方法分组

| 业务 | 方法名 |
| --- | --- |
| App 配置/通用 | `appConfig`、`tipOff`、`getAppShelfStatus`、`upgrade`、`activityConfig`、`sensitiveWordCheck`、`sensitiveImageCheck` |
| 登录/用户 | `loginBygGuest`、`loginByThird`、`loginByFlashMobile`、`loginByPhone`、`logoutApp`、`getUserHomeInfo`、`modifyUserInfo`、`sendSmsValidateCode`、`bindPhone`、`logOff`、`cancelLogOff`、`logOffStatus` |
| 分享/反馈/VIP | `getUserShareUrl`、`shareReport`、`feedback`、`vipConfig`、`daily`、`musicGenerateSourceReport` |
| AI/SSE 文本生成 | `sseContentOptimize`、`sseContentRandom`、`sseWorkDescOptimize`、`sseScriptFromIdea`、`sseScriptFromStory`、`sseContentAIExpand`、`sseContentSynopsis`、`singleWorkGenerate` |
| 创作前置/剧本 | `creationGeneratePre`、`saveWorkSummary`、`saveStoryEpisodeScript`、`saveSingleEpisodeScript`、`saveEpisodeScript`、`dramaContentEdit`、`sceneContentEdit` |
| 短剧生成 | `shotDramasCalculatePrice`、`shotDramaGenerate`、`dramaGenerate`、`dramaGenerateSourceReport`、`dramaRolesPolling`、`publishShortDrama` |
| 风格/灵感/模型 | `creationStyleList`、`getInspirationQuickList`、`videoSetConfig`、`getStoryboardModelConfig`、`switchModel`、`recommendClickReport` |
| 角色/形象 | `optimizeRoleImageDesc`、`roleImageRegenerate`、`roleTagList`、`getHistoryRoles`、`historyRoleSwitch`、`dramasAddRole`、`dramasDelRole` |
| 生成中/短剧信息 | `generatingBookList`、`shortDramasInfo`、`releasedShortDramasInfo`、`deleteDramaInfo`、`latestEpisode`、`getWorkInfo` |
| 配音/合成 | `shortDramasSynthesis`、`shortDramasSynthesisUpload`、`shortDramasDubbing`、`shortDramasDubbingRetry`、`shortDramasDubbingUse`、`shortDramasDubbingStatus` |
| 分镜图片 | `shortDramasSegmentImageReplace`、`shortDramasSegmentImageHistory`、`shortDramasSegmentImageRegenerate`、`shortDramasSegmentImageDescOptimize`、`shortDramasSegmentImageGenerateRetry` |
| 剧集/发布/追更 | `seriesFollow`、`seriesSetPrice`、`seriesPriceConfig`、`seriesModifyInfo`、`seriesPublish`、`episodeList`、`singleEpisodeDetails`、`singleEpisodeSeriesDetails`、`saveSequelNovelStory` |
| 资产管理 | `generateAllAssetDescription`、`workAssetAllStatus`、`workAssetSingleStatus`、`workAssetSceneList`、`workAssetSceneSave`、`workAssetSceneDelete`、`workAssetRoleList`、`workAssetRoleSave`、`workAssetRoleDelete`、`workAssetPropList`、`workAssetPropSave`、`workAssetPropDelete`、`workAssetSingleGenerate`、`workAssetOneKeyGenerate` |
| 分镜管理 | `generateAllStoryboardDescription`、`generateAllStoryboardDescriptionStatus`、`workStoryboardSingleStatus`、`workStoryboardList`、`workStoryboardSave`、`workStoryboardDelete`、`workStoryboardSingleGenerate`、`workStoryboardVideoGenerate`、`workStoryboardVideoSingleGenerate`、`dropRepaint`、`workStoryboardVideoSynthesis` |
| 我的作品 | `getMyWork`、`batchDeleteWork`、`batchDeleteWorkEpisode`、`getWorkStatusList`、`userContinuableSeries` |
| 充值/钱包 | `queryWalletInfo`、`queryPointDetailList`、`coinsConfigList`、`pointConfigList`、`paymentConfigList`、`createOrderAlipay`、`createOrderWechat`、`createOrder`、`getOrderStatus`、`queryAiPrice`、`h5PayWx`、`h5PayAlipay`、`powerCalculate` |
| 小说上传/新手链路 | `novelUploadSummary`、`pollNovelDocsSummary`、`pollSummaryResult`、`saveScript`、`pollNovelScript`、`pollScriptResult`、`novelUploadDelete`、`getNewerExampleWorkList`、`createTaskFromNovel`、`continueTaskFromNovel`、`createTaskFromInspiration`、`createSummaryTaskFromInspiration`、`retrySummaryTaskFromInspiration`、`createTaskFromSingleEpisode`、`retryTaskFromSingleEpisode`、`continueTaskFromNovelDoc` |
| 首页/播放 | `bookShelfHome`、`homeWaterfallPage`、`episodeDirectoryPage`、`episodeDirectoryInfo`、`episodePlayInfo`、`simpleWorkInfo`、`playCountInc`、`likeToggle`、`collectToggle`、`nextWorkRecommend`、`userPlayerState`、`userPlayReport`、`homeSearch` |
| 播放解锁/奖励 | `storyUnlockPreload`、`storyUnlockInfo`、`storyUnlockCoins`、`storyUnlockAdPrepare`、`storyUnlockAdResult`、`rewardAdPoll`、`episodeUnlockLockStatus`、`rewardCheckInList`、`rewardCheckInReport`、`rewardAdTaskList`、`rewardAdPreReport` |
| 其他分页/取消 | `userPlayHistoryPage`、`userCollectionPage`、`workAssetRolePage`、`workAssetRoleImagePage`、`workAssetPropPage`、`workAssetScenePage`、`cancelSceneImage`、`cancelRoleImage`、`cancelPropImage`、`generateSubtitle`、`subtitleTaskStatus`、`getFirstRechargeDialogInfo`、`updateStorySummary`、`reportSubmission` |

新增接口建议顺序：

1. 在 `ApiConstants.kt` 增加 path 常量。
2. 在 `request/` 或 `response/`、`entity/` 增加数据类。
3. 在 `ApiService.kt` 增加 Retrofit 方法。
4. 在目标 `Repository` 包一层业务方法。
5. 在 `ViewModel` 里用 `request(...)` 或 `requestWithLoading(...)` 调用。

## Repository 索引

| Repository | 位置 | 主要方法/用途 |
| --- | --- | --- |
| `AppRepository` | `core/AppRepository.kt` | 项目基础 Repository，暴露 `api`、`appDatabase` |
| `CommonRepository` | `core/data/CommonRepository.kt` | 通用 Repository 预留层 |
| `CreationRepository` | `feature/creator/data/CreationRepository.kt` | `getInspirationQuickList`、`videoSetConfig`、`sensitiveWordCheck` |
| `AssetManagementRepository` | `feature/creator/assets/AssetManagementRepository.kt` | 资产状态、场景/角色/道具列表与保存、删除、生成 |
| `EditStoryboardRepository` | `feature/editor/EditStoryboardRepository.kt` | 短剧详情、生成/合成、价格、配音、分镜图片替换/重绘/优化/重试 |
| `SeriesRepository` | `feature/series/SeriesRepository.kt` | 剧集详情、关注、定价、价格配置、修改信息、发布 |
| `CoinRepository` | `feature/recharge/CoinRepository.kt` | 金币/积分配置、Google/支付宝/微信下单、支付配置 |
| `ThirdLoginRepository` | `feature/login/data/ThirdLoginRepository.kt` | 三方登录 |
| `FlashLoginRepository` | `feature/login/data/FlashLoginRepository.kt` | 一键登录、验证码登录、绑定手机 |
| `PhoneLoginRepository` | `feature/login/data/PhoneLoginRepository.kt` | 手机验证码登录和绑定 |
| `BindPhoneRepository` | `feature/login/data/BindPhoneRepository.kt` | 独立绑定手机页 |
| `BrowserRepository` | `feature/web/data/BrowserRepository.kt` | H5 支付桥接 `h5PayAlipay`、`h5PayWx` |
| `SettingsRepository` | `feature/settings/SettingsRepository.kt` | 设置页预留 Repository |
| `FeedRepository` | `feature/play/data/FeedRepository.kt` | 播放列表预加载缓存，`setPreloadedSeries`、`consumePreloadedSeries` |

## Feature 模块索引

| 模块 | 职责 | UI 入口 | ViewModel/Repository | 相关接口关键词 |
| --- | --- | --- | --- | --- |
| `feature/main` | 主框架、底部 Tab、升级检查、首页弹窗 | `MainActivity`、`ActivityDialog` | `MainViewModel` | `getUserHomeInfo`、`daily`、`loginTaskPopup`、`activityConfig` |
| `feature/home` | 首页书架、Banner、瀑布流、搜索 | `HomeFragment`、`SearchActivity` | `HomeViewModel`、`SearchViewModel`、`VideoRemoteMediator` | `getAppBanner`、`bookShelfHome`、`homeWaterfallPage`、`homeSearch` |
| `feature/mine` | 我的页、播放历史、收藏、我的创作入口 | `MineFragment`、`MineChildFragment`、`MyCreationsFragment`、`PhoneShowActivity` | `MineViewModel`、`MineChildViewModel` | `userPlayHistoryPage`、`userCollectionPage`、`queryWalletInfo` |
| `feature/works` | 我的作品、作品选择、角色图片选择、作品状态轮询 | `MyWorksFragment`、`MyListFragment`、`AssetSelectActivity`、`RoleImageSelectActivity` | `MyWorksViewModel`、`MyListModel`、`AssetSelectViewModel`、`RoleImageSelectViewModel` | `getMyWork`、`batchDeleteWork`、`getWorkStatusList`、`workAssetRoleImagePage` |
| `feature/creator` | AI 短剧创作主链路、小说上传、续写、资产、分镜、模型/风格设置 | `CreationRootActivity`、`CreationFragment`、`StoryEditorActivity`、`ContinueWritingActivity`、`AssetManagementFragment`、`StoryboardDesignFragment`、多个配置 Dialog | `CreationViewModel`、`StoryEditorViewModel`、`DramaGenerateStep1ViewModel`、`DramaGenerateStep2ViewModel`、`AssetManagementViewModel`、`StoryboardDesignViewModel`、`CreationRepository`、`AssetManagementRepository` | `createTaskFromNovel`、`pollSummaryResult`、`pollNovelScript`、`generateAllAssetDescription`、`workStoryboard*`、`switchModel` |
| `feature/editor` | 分镜编辑、单分镜视频编辑、视频合成、剧集信息编辑、通用输入弹窗 | `EditStoryboardActivity`、`EditStoryboardVideoActivity`、`DramaInfoEditActivity`、`InputDialog` | `EditStoryboardViewModel`、`EditStoryboardVideoViewModel`、`DramaInfoEditViewModel`、`InputViewModel`、`EditStoryboardRepository` | `shortDramasInfo`、`workStoryboardSave`、`workStoryboardVideoSynthesis`、`shortDramasSegmentImage*`、`updateWorkInfo` |
| `feature/play` | 竖滑播放、剧集目录、广告/金币解锁、下载/分享 | `DramasPlayPagerActivity`、`EpisodeChooseDialog`、`AdUnlockDialog`、`DownloadMethodDialog` | `DramasPlayPagerViewModel`、`EpisodeChooseViewModel`、`AdUnlockViewModel`、`EpisodeUnlockController`、`FeedPlayerManager` | `episodePlayInfo`、`nextWorkRecommend`、`storyUnlock*`、`episodeUnlockLockStatus`、`userPlayReport` |
| `feature/series` | 剧集详情、剧集列表、剧集管理、分享 | `SeriesActivity`、`WorksManagementActivity`、`SeriesShareDialog`、`EpisodeShareDialog` | `SeriesViewModel`、`WorksManagementViewModel`、`SeriesRepository`、`EpisodePagingSource` | `getWorkInfo`、`episodeList`、`seriesFollow`、`seriesSetPrice`、`seriesModifyInfo`、`seriesPublish` |
| `feature/recharge` | 钱包、金币/积分充值、支付方式、消耗确认、支付成功 | `MyBalanceActivity`、`PowerRechargeDialog`、`PowerConsumeConfirmDialog`、`PaySuccessDialog` | `MyBalanceViewModel`、`PowerConsumeConfirmViewModel`、`CoinRepository` | `queryWalletInfo`、`coinsConfigList`、`pointConfigList`、`paymentConfigList`、`createOrder`、`createOrderAlipay`、`createOrderWechat` |
| `feature/rewards` | 签到、看广告任务、奖励规则 | `RewardsActivity`、`RewardWelcomeDialog`、`RewardRulesDialog`、`CheckInSuccessDialog` | `RewardsViewModel` | `rewardCheckInList`、`rewardCheckInReport`、`rewardAdTaskList`、`rewardAdPreReport` |
| `feature/login` | 统一登录、手机号登录、一键登录、三方登录、绑定手机、隐私协议 | `UnifiedLoginActivity`、`UnifiedLoginDialog`、`PhoneLoginActivity`、`PhoneLoginDialog`、`PhoneLoginDialogActivity`、`BindPhoneActivity` | `LoginViewModel`、`UnifiedLoginViewModel`、两个 `PhoneLoginViewModel`、`BindPhoneViewModel`、登录 Repository | `loginByThird`、`loginByFlashMobile`、`loginByPhone`、`sendSmsValidateCode`、`bindPhone` |
| `feature/settings` | 设置、语言、客服、反馈、举报、注销、关于 | `SettingsActivity`、`LanguagesActivity`、`FeedbackActivity`、`ReportActivity`、`ReportDialog`、`LogoffActivity`、`ApplyLogOffActivity`、`AboutUsActivity`、`CustomerServiceDialog` | `SettingsViewModel`、`LanguagesViewModel`、`FeedbackViewModel`、`ReportViewModel`、`ReportDialogViewModel`、`LogoffViewModel` | `feedback`、`tipOff`、`reportSubmission`、`logOff`、`cancelLogOff`、`logOffStatus` |
| `feature/splash` | 启动页、隐私协议、新手引导、首充弹窗 | `SplashActivity`、`PolicyDialog`、`GuideStep1AdActivity`、`GuideStep2StorySelectActivity`、`GuideStep3GeneratingActivity`、`GuideStep4VideoActivity`、`FirstRechargeDialog` | `SplashViewModel`、`GuideStep2ViewModel`、`GuideStep3ViewModel`、`GuideStep4ViewModel`、`FirstRechargeViewModel` | `getNewerExampleWorkList`、`getFirstRechargeDialogInfo`、`createTaskFrom*` |
| `feature/web` | WebView/H5 容器、JSBridge、H5 支付 | `SimpleBrowserActivity`、`BrowserFragment` | `BrowserViewModel`、`BrowserRepository` | `h5PayWx`、`h5PayAlipay` |
| `feature/download` | 下载任务、素材拉取、ASS 字幕、FFmpeg 转换、下载进度弹窗 | `DownloadingDialog` | `DownloadingViewModel`、`DownloadTask`、`ShortDramaTransform`、`MicroAnimShortDramaTransform` | 依赖网络资源、相册保存和下载状态回调 |
| `feature/details` | 算力明细 | `ComputingPowerDetailsActivity`、`ComputingPowerDetailsFragment` | `ComputingPowerDetailsViewModel`、`ComputingPowerDetailsFragmentViewModel`、`ComputingPowerDetailsPagingSource` | `queryPointDetailList` |
| `feature/aicompose` | 生成中状态/列表 item | `ItemGeneratingVM` | `GeneratingViewModel` | 生成中 UI 状态 |
| `feature/upgrade` | 更新弹窗 | `UpgradeDialogFragment` | `EmptyViewModel` | 由 `UpgradeManager` 驱动 |
| 根级页面 | 前台拉起、图片预览 | `BringForegroundActivity`、`PhotoPagerActivity` | 无专属 Repository | App 启动/图片预览辅助 |

## 路由与入口

| 文件 | 关键接口 | 用途 |
| --- | --- | --- |
| `router/AppRouter.kt` | `jump(schemeUri)`、`jump(uri)`、`startStory(...)`、`singleEpisodeSeriesDetails(...)` | 处理 `epnovascheme://`、http/apk 跳转、按剧集进度跳创作/编辑/播放 |
| `router/AppNavigator.kt` | `checkLoginIntercepted(...)`、`checkRealLogin(...)`、`startLogin(...)`、`forceLogoutAndRestart()` | 登录拦截、游客/真实登录判断、启动登录页或登录弹窗 |
| `router/AppLaunchJump.kt` | `launchJump(uri)` | Push/外部唤起 App 后延迟跳转 |
| `feature/main/MainActivity.kt` | `start(...)`、`startFromNewer(...)`、底部 Tab 切换 | App 主页面，当前 Tab 为 Home、Works、Mine |

当前已处理的 scheme host：

- `restoreCreator`：恢复创作，参数含 `workId`、`storyId`、`workTitle`、`workType`、`workRatio`、`totalEpisodeCnt`。
- `toHome`：切换到首页并回退到 `MainActivity`。
- `toTargetEpisode`：查询剧集锁定状态后打开播放页。

## 本地数据库索引

| 文件/目录 | 内容 |
| --- | --- |
| `core/db/AppDatabase.kt` | Room Database，暴露 `workInformationDao()` 等 DAO |
| `core/db/dao/WorkDao.kt` | 首页/瀑布流作品缓存、RemoteKeys |
| `core/db/dao/WorkInformationDao.kt` | 我的作品 Paging 缓存、批量插入/删除/upsert |
| `core/db/dao/ShortDramaDao.kt` | 短剧列表缓存 |
| `core/db/dao/SearchHistoryDao.kt` | 搜索历史 |
| `core/db/dao/AIMessageDao.kt`、`ChatSceneDao.kt` | AI 消息、聊天场景 |
| `core/db/entity/` | `WorkInfoEntity`、`RemoteKeys`、`WorkInformationInfo`、`SearchHistory`、`AIMessage` 等表实体 |
| `core/db/mapper/WorkInfoMapper.kt` | `WorkVOInfo` 与 `WorkInfoEntity` 互转 |
| `core/db/convert/` | Room TypeConverter，处理列表、角色、分镜、AI 消息等 JSON 转换 |

## UI 与列表基础能力

| 文件 | 用途 |
| --- | --- |
| `base/item/AppItemViewModel.kt` | 项目列表 item VM 基类 |
| `common/adapter/QuickAdapter.kt` | DataBinding 列表 Adapter 常用入口 |
| `common/adapter/MultiTypeAdapter.kt`、`DefaultViewType` | 多类型 item 列表 |
| `common/adapter/BannerAdapter.kt` | Banner 列表适配 |
| `common/ext/ItemViewModelExtensions.kt` | item VM 缓存和局部更新判断 |
| `common/ext/PagingExtensions.kt` | Paging 刷新、LoadState 展示、错误 toast |
| `core/databinding/BindingAdapters.kt` | 图片、RecyclerView、选中态、标签、颜色、分页等 DataBinding 适配器 |
| `widget/stateful/` | 多状态 Adapter 和分页 Footer |
| `widget/refreshrecycleview/` | 旧刷新 RecyclerView 封装 |

## 常用功能落点

### 创建/生成短剧

1. 入口：`feature/creator/CreationRootActivity.kt`、`CreationFragment.kt`、`StoryEditorActivity.kt`。
2. 文本/小说/灵感生成：`CreationViewModel`、`DramaGenerateStep1ViewModel`、`DramaGenerateStep2ViewModel`。
3. 数据层：`CreationRepository`、`ApiService.createTaskFromNovel`、`createTaskFromInspiration`、`pollSummaryResult`、`pollNovelScript`。
4. 资产：`AssetManagementFragment`、`AssetManagementViewModel`、`AssetManagementRepository`。
5. 分镜：`StoryboardDesignFragment`、`StoryboardDesignViewModel`。

### 资产/角色/场景/道具

1. 入口：`feature/creator/assets/AssetManagementFragment.kt`。
2. ViewModel：`AssetManagementViewModel`，处理批量生成、保存、删除、取消图片、AI 价格。
3. Repository：`AssetManagementRepository`。
4. 请求体：`QueryAssetBody`、`SaveSceneBody`、`SaveRoleBody`、`SavePropBody`、`GenerateAssetBody`、`TaskIdBody`。
5. 实体：`SimpleSceneInfo`、`SimpleRoleInfo`、`SimplePropInfo`、`RoleImageInfo`。

### 分镜编辑/视频生成

1. 分镜描述编辑：`feature/editor/EditStoryboardActivity.kt`、`EditStoryboardViewModel`。
2. 分镜视频编辑/下载：`EditStoryboardVideoActivity.kt`、`EditStoryboardVideoViewModel`。
3. 数据层：`EditStoryboardRepository`。
4. 生成/状态：`workStoryboardSingleGenerate`、`workStoryboardVideoGenerate`、`workStoryboardVideoSingleGenerate`、`workStoryboardSingleStatus`。
5. 合成：`workStoryboardVideoSynthesis`、`feature/editor/VideoSynthesis*`、`feature/download/transform/*`。

### 播放/解锁

1. 播放入口：`feature/play/DramasPlayPagerActivity.kt`。
2. 播放状态：`DramasPlayPagerViewModel`、`FeedPlayerManager`、`FeedListController`。
3. 剧集选择：`EpisodeChooseDialog`、`EpisodeChooseViewModel`。
4. 解锁：`EpisodeUnlockController`、`AdUnlockDialog`。
5. 接口：`episodePlayInfo`、`episodeDirectoryPage`、`storyUnlockPreload`、`storyUnlockInfo`、`storyUnlockCoins`、`storyUnlockAdPrepare`、`storyUnlockAdResult`、`episodeUnlockLockStatus`。

### 充值/支付

1. 钱包入口：`feature/recharge/MyBalanceActivity.kt`。
2. ViewModel：`MyBalanceViewModel`，加载金币/积分配置、Google 商品、余额、倒计时。
3. Repository：`CoinRepository`。
4. Google 支付：`core/google/billing/BillingManager.kt`。
5. H5 支付：`feature/web/BrowserRepository.kt`、`SimpleBrowserActivity`、`BrowserFragment`。
6. 接口：`coinsConfigList`、`pointConfigList`、`paymentConfigList`、`createOrder`、`createOrderAlipay`、`createOrderWechat`、`getOrderStatus`。

### 登录/绑定

1. 登录拦截：`router/AppNavigator.kt`。
2. 统一登录：`feature/login/unified/UnifiedLoginActivity.kt`、`UnifiedLoginDialog.kt`、`UnifiedLoginViewModel.kt`。
3. 手机登录：`feature/login/phone/PhoneLoginActivity.kt`、`PhoneLoginDialog.kt`、`PhoneLoginViewModel.kt`。
4. 一键登录：`feature/login/flash/PhoneLoginDialogActivity.kt`、`feature/login/flash/PhoneLoginViewModel.kt`。
5. 绑定手机：`feature/login/bind/BindPhoneActivity.kt`、`BindPhoneViewModel.kt`。
6. 三方 SDK：`GoogleSignInHelper`、`FacebookSignInHelper`。

### 设置/反馈/举报/注销

1. 设置入口：`feature/settings/SettingsActivity.kt`、`SettingsViewModel.kt`。
2. 反馈：`FeedbackActivity`、`FeedbackViewModel`、`ApiService.feedback`。
3. 举报：`ReportActivity`、`ReportDialog`、`ReportViewModel`、`ReportDialogViewModel`、`ApiService.tipOff`、`reportSubmission`。
4. 注销：`LogoffActivity`、`ApplyLogOffActivity`、`LogoffViewModel`、`ApiService.logOff`、`cancelLogOff`、`logOffStatus`。
5. 语言：`LanguagesActivity`、`LanguagesViewModel`、`LocaleHelper`。

## 通用扩展索引

| 文件 | 常用能力 |
| --- | --- |
| `core/ext/ViewModelExt.kt` | `request`、`requestWithLoading`、`launchPolling` |
| `core/ext/DataExt.kt` | 敏感词检查、客服跳转、AIGC 元数据、充值/首充弹窗 |
| `core/ext/CommonExt.kt` | 浏览器打开、ResponseBody 保存、ApiResponse 异常包装 |
| `core/ext/EventScopeExt.kt` | 用户登录/登出事件订阅 |
| `common/ext/Context.kt` | Fragment 参数、打开浏览器、下载文件、选择图片、Activity 清理 |
| `common/ext/DialogExt.kt` | 通用确认弹窗 |
| `common/ext/ImageExt.kt`、`ImageLoaderExt.kt` | 图片加载、缩略图、bitmap 转换/预加载 |
| `common/ext/PagingExtensions.kt` | Paging 刷新和错误处理 |
| `common/ext/ViewExt.kt`、`ViewExtensionsExt.kt` | View 动画、截图缓存/保存到相册、焦点处理 |
| `common/ext/TextViewExt.kt` | 文本高亮、角色标签解析 |
| `common/ext/StringExt.kt`、`FormatExt.kt` | 字符串清理、省略、金额/时长/数量格式化 |
| `common/permissions/` | 权限请求封装 |
| `common/utils/` | 业务工具集合，图片保存、加密、日期、文件、导航、进度模拟等 |

## Widget 索引

| 目录/文件 | 用途 |
| --- | --- |
| `widget/CommonDialog.kt`、`GeneratingDialog.kt`、`LoadingDialog2.java` | 通用弹窗/生成中/加载 |
| `widget/CodeInputView.kt`、`PhoneEditText.java`、`RestrictedEditText.kt`、`DecimalInputFilter.kt` | 输入控件 |
| `widget/CoverPlayerView.kt`、`FullVideoView.java`、`PlayerSeekbar.kt`、`PlayerRoundProgressBar.kt` | 播放相关控件 |
| `widget/AnimatedAvatarsView.kt`、`MultiAvatarView.java` | 头像展示 |
| `widget/listmoretextview/`、`expandtextview/`、`htmlview/`、`htmltag/` | 富文本/展开文本/HTML 标签 |
| `widget/decoration/` | RecyclerView 间距和分割线 |
| `widget/actionsheet/`、`blurdialog/` | ActionSheet、模糊弹窗 |
| `widget/guideview/` | 引导蒙层 |
| `widget/roundedimageview/` | 圆角图片 |

## 维护注意

- `ApiResponse.java` 不应修改。
- `OssManager.kt` 按项目忽略规则不扫描、不编辑，除非明确要求。
- 新增硬编码 key 时放到对应常量中，不要散落在 Activity/Fragment/ViewModel。
- 新增三方 SDK 能力优先落到 `core/manager/<feature>/` 抽象层，业务代码不要直接依赖 SDK 类型。
- 新增列表优先复用 `QuickAdapter<ItemXxxVM>` 和 `AppItemViewModel<T>` 的项目模式。
- 有轮询/SSE/下载/播放任务时，在 ViewModel `onCleared()` 或页面销毁处取消 Job 和 handler。
