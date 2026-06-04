# TopEarners 与 MyCreations 创作播放体系最终分析

整理时间：2026-06-04  
范围：只分析，不改业务代码。旧草稿已清理，本文作为 `design/project/play` 下当前唯一推荐方案。

## 1. 核心结论

后续应把播放器相关模型拆成两套体系：

| 体系 | 业务范围 | 数据模型 | 播放器 |
| --- | --- | --- | --- |
| 创作播放体系 | TopEarners 推荐作品（`TopEarnersAdapter`）、`MyCreationsFragment`、作品详情剧集列表，参考 FastFilm | 新 `CreatorWorkInfo / CreatorEpisodeInfo / CreatorPlatformInfo`，字段保持 FastFilm | 新创作播放器 |
| 内容/发行播放体系 | 首页内容区、书架、搜索、内容消费、解锁、续播、点赞收藏 | 现有 `WorkVOInfo`、现有 `core.network.apis.entity.WorkInfo/EpisodeInfo` 或后续已发行内容模型 | 现有内容播放器或单独评估 |

不能继续让新 TopEarners/MyCreations 播放链路直接复用当前 `core.network.apis.entity.WorkInfo/EpisodeInfo`。当前类名虽然和 FastFilm 一样，但语义已经被多套业务共用：一边有未发行创作需要的生成、发布审核、发行入口，另一边又混入内容消费需要的锁集、点赞、收藏、续播、解锁预加载。新创作播放业务如果继续混用，会把两套播放器耦在一起。

最终推荐：

```text
CreatorPlayableEpisode = CreatorEpisodeInfo + CreatorWorkContext
```

- 播放核心用 `CreatorEpisodeInfo`。
- 作品级展示信息用 `CreatorWorkContext`。
- TopEarners 一部作品当前只有一集，所以一条作品列表 item 映射成一条 `CreatorPlayableEpisode`。
- MyCreations/作品详情是单作品多剧集，直接把每个 `CreatorEpisodeInfo` 映射成多条 `CreatorPlayableEpisode`。
- 本文里的“推荐作品”只指 TopEarners 推荐作品（`TopEarnersAdapter`），不是另一个 Creation 推荐 source。
- TopEarners 推荐作品 item 点击直接进入新创作播放器，不加 FastFilm 中间的 `HomeWorkDetailActivity`。
- 首页内容区和 TopEarners 推荐作品完全不是同一类 source：TopEarners 推荐作品可以理解为未发行/创作中的产品素材，结构和未发行数据结构共用；首页内容区是已发行作品。两者分页源不能共享，播放器也不能共享。
- MyCreationsFragment 的列表业务逻辑不用动，后续主要改数据结构；其中“剧集列表播放”入口要切到新创作播放器，因为播放表现和 TopEarners 推荐作品的新播放器一致。
- 只有 TopEarners 推荐作品支持跨作品上下切换。MyCreations 单集作品只播放自己；作品详情剧集列表只在当前作品的剧集内上下切换，不跨到其他作品。

这里要特别避免一个误区：创作流程里某个作品发行后，确实会出现在首页内容区，但那时它已经属于“已发行内容”的分发链路，不再复用 TopEarners 推荐作品的分页源、缓存表或播放器实例。TopEarners 推荐作品、MyCreations 未发行作品和作品详情剧集列表可以共享 Creator 模型与新播放器能力；首页内容区不参与这个共享边界。

## 2. 当前本地结构判断

### 2.1 TopEarners 现状

当前链路：

```text
ApiService.recommendWorkList
-> PagedList<TopEarnerWorkInfo>
-> TopEarnersPagingSource
-> TopEarnersAdapter
-> 点击旧 DramasPlayPagerActivity
```

`TopEarnerWorkInfo` 字段其实是 FastFilm `WorkInfo` 的子集：

| TopEarners 字段 | FastFilm 字段 | 说明 |
| --- | --- | --- |
| `workId/userId/workTitle/workCoverUrl/workDesc/workRatio` | `WorkInfo` 同名字段 | 作品上下文 |
| `totalEpisodeCnt/playCount` | `WorkInfo` 同名字段 | 列表展示 |
| `totalEarnings` | `WorkInfo.totalEarnings` | 收益展示 |
| `platformLinks` | `WorkInfo.platformLinks` | 平台展示 |
| `workStoryItemList` | `WorkInfo.workStoryItemList` | 可播放剧集 |

`TopEarnerWorkStoryItem` 是 FastFilm `EpisodeInfo` 的轻量子集：

| TopEarners 字段 | FastFilm 字段 | 影响 |
| --- | --- | --- |
| `storyId` | `EpisodeInfo.storyId` | 可作为播放 item 唯一 id |
| `videoUrl` | `EpisodeInfo.videoUrl` | 可直接播放 |
| `duration: Int?` | `EpisodeInfo.duration: Long?` | 需要统一成秒或毫秒，播放器内部建议转成 `durationMs` |
| `workRatio` | `EpisodeInfo.workRatio` | 控制播放器比例 |
| `playCount` | `EpisodeInfo.playCount` | 列表/播放量展示 |

它缺少内容消费播放器需要的字段：

- `lockStatus`
- `costNums`
- `liked/likeCount`
- `storyUnlockPreload` 相关字段

这些缺失不影响 TopEarners/FastFilm 播放，因为创作播放体系不需要锁集解锁、点赞收藏和续播状态。

它也缺少 MyCreations/作品详情需要的发布审核字段，例如 `publishStatus`、`rejectReason`、`coverBadgeType`、`globalPublishStatus`、`publishedEpisodeCnt`、`showPublishButton`、`workContractPublishPageUrl`。这些字段不属于内容消费体系，后续应该挪到新的 `CreatorWorkInfo/CreatorEpisodeInfo` 上，只是 TopEarners 推荐作品当前不依赖它们。

### 2.2 MyCreations 现状

当前 `MyCreationsFragment` 通过 `MyWorksViewModel.getWorkInformation()` 加载：

```text
getMyWork(MyWorkBody)
-> PagedList<WorkInfo>
-> WorkInformationInfo(work = WorkInfo)
-> MyWorksAdapter
```

点击单集完成作品时：

```text
getEpisodes(workId)
-> episodeList(WorkIdPagedBody)
-> List<EpisodeInfo>
-> FeedRepository.setPreloadedSeries(workId, validPlayList)
-> 旧 DramasPlayPagerActivity
```

这里的 `WorkInformationInfo` 结构来自 FastFilm，理论上属于创作体系。但当前它嵌入的是本项目现有 `WorkInfo`，这就是风险点：同一个 `WorkInfo` 同时被创作列表和内容解锁播放器使用。

### 2.3 本项目现有 WorkInfo/EpisodeInfo 风险

现有 `core.network.apis.entity.WorkInfo` 和 FastFilm `WorkInfo` 已经不完全一致。

本项目现有 `WorkInfo` 偏向创作 + 发布审核：

- `globalPublishStatus`
- `publishedEpisodeCnt`
- `coverBadgeType`
- `workContractPublishPageUrl`
- `showPublishButton`
- `hasPublishedOrUnderReviewEpisode()`

但它缺 FastFilm TopEarners 需要的部分字段：

- `platformLinks`
- `totalEarnings`
- `shelfTime`
- `generationStatusFlag`
- `stepProgressSubTitle`
- `getPlatformLogoUrls()`

现有 `EpisodeInfo` 又同时包含创作发布审核字段和内容消费字段。发布审核字段应该进入 Creator 体系：

- `publishStatus`
- `rejectReason`
- `coverBadgeType`

内容消费字段不要进入 Creator 体系：

- `liked`
- `likeCount`
- `lockStatus`
- `costNums`

这些字段会把播放器带向旧的解锁链路。TopEarners 新播放逻辑如果直接使用这个 `EpisodeInfo`，容易让后续代码误接：

- `storyUnlockPreload`
- `episodePlayInfo`
- `userPlayerState`
- 解锁弹窗
- 首充/广告解锁
- 点赞收藏
- 进度上报

所以新模型必须隔离。

## 3. 数据源与模型前提

后端会处理不同入口的数据结构差异，所以前端不需要强制所有接口都返回完全同一个 DTO。可以接受：

| 来源 | 当前或未来接口结构 | 前端处理 |
| --- | --- | --- |
| TopEarners | `TopEarnerWorkInfo` 或 FastFilm 风格 work | mapper 到 `CreatorPlayableEpisode` |
| MyCreations | FastFilm 风格 `CreatorWorkInfo` | 列表用作品模型，进入剧集播放时转 episode feed |
| 作品详情剧集列表 | FastFilm 风格 `CreatorEpisodeInfo` | 直接转 `CreatorPlayableEpisode` |

核心不是接口 DTO 必须统一，而是进入播放器前必须统一成：

```text
CreatorPlayableEpisode = CreatorEpisodeInfo + CreatorWorkContext
```

这样既能接受 TopEarners 当前轻量结构，也能接受未来 FastFilm 完整结构。

首页内容区不纳入这个统一入口。它的数据源是已发行作品，和 TopEarners 推荐作品的未发行/创作中素材不是一套业务；即使某些字段未来相似，也不要和 TopEarners 推荐作品共用 feed 或播放器。

换句话说，`CreatorPlayableEpisode` 是创作播放器的播放单元，不是全站所有视频播放单元。首页内容区如果后续也需要新的已发行内容播放器，应按内容/发行体系单独评估，不从 TopEarners 推荐作品 source 里取数据。

## 4. 新模型命名建议

不要再在新播放模块里直接 import：

```kotlin
com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
```

新实体类不要新建子包，也不要放到业务模块，直接放在现有 `entity` 包下：

```text
core/network/apis/entity/
    CreatorWorkInfo.kt
    CreatorEpisodeInfo.kt
    CreatorPlatformInfo.kt
    CreatorWorkContext.kt
    CreatorPlayableEpisode.kt
    CreatorPlaySource.kt
    CreatorPublishStatus.kt
    CreatorCoverBadgeType.kt
```

这些类和旧 `WorkInfo/EpisodeInfo` 在同一个 package 下，但必须靠类名做语义隔离：推荐用 `Creator*`，不要用 `TopEarner*`，因为同一套模型会服务：

- TopEarners
- MyCreations
- 作品详情
- 作品详情剧集列表
- 后续 FastFilm 迁移来的创作播放页面

因为新旧类在同一个 `entity` 包下，后续实现时要避免“省 import 以后误用旧类”的问题：新创作播放器、TopEarners 新 feed、MyCreations 新模型和作品详情剧集播放相关代码，只允许使用 `CreatorWorkInfo / CreatorEpisodeInfo / CreatorPlayableEpisode`；旧 `WorkInfo/EpisodeInfo` 只留给内容/发行播放体系和迁移前的旧链路。

## 5. 新旧字段边界

### 5.1 CreatorWorkInfo

字段应以 FastFilm `WorkInfo` 为准，覆盖 TopEarners 和 MyCreations：

```kotlin
data class CreatorWorkInfo(
    val workId: String? = null,
    val userId: String? = null,
    val workTitle: String? = null,
    val workDesc: String? = null,
    val workStyleCode: String? = null,
    val workRatio: String? = null,
    val workCoverUrl: String? = null,
    val workType: Int? = null,
    val workStatus: Int? = null,
    val totalEpisodeCnt: Int? = null,
    val finishedEpisodeCnt: Int? = null,
    val lastEpisodeNo: Int? = null,
    val lastGenerateTime: String? = null,
    val playCount: Int? = null,
    val workStoryItemList: List<CreatorEpisodeInfo>? = null,
    val contractStatus: Int? = null,
    val publishStatus: Int? = null,
    val rejectReason: String? = null,
    val contractLevel: String? = null,
    val publishPlatformImageList: List<String>? = null,
    val workSelfPublishProcessUrl: String? = null,
    val workContractPublishPageUrl: String? = null,
    val showPublishButton: Int? = null,
    val globalPublishStatus: Int? = null,
    val publishedEpisodeCnt: Int? = null,
    val coverBadgeType: Int? = null,
    val platformLinks: List<CreatorPlatformInfo>? = null,
    val totalEarnings: String? = null,
    val shelfTime: Long? = null,
    val generationFailedFlag: Int? = null,
    val generationStatusFlag: Int? = null,
    val stepProgressSubTitle: String? = null
)
```

发布审核字段必须归到 `CreatorWorkInfo`。这套逻辑发生在作品未发行阶段，服务 MyCreations、作品详情、发布入口、审核角标、驳回原因和发行按钮，不应该继续挂在旧内容消费 `WorkInfo` 上。

同时建议把发布审核判断从旧 `WorkInfo` 的成员函数迁出来，变成 Creator 体系自己的方法或 UI VM 方法，例如：

- `CreatorWorkInfo.isPublishedOrUnderReview()`
- `CreatorWorkInfo.hasPublishedOrUnderReviewEpisode()`
- `CreatorWorkInfo.showPublishButton()`
- `CreatorWorkInfo.isPublishButtonEnabled()`
- `CreatorWorkInfo.isPublishReviewFailed()`
- `CreatorWorkInfo.publishBadgeText()`
- `CreatorWorkInfo.publishBadgeTextColor()`
- `CreatorWorkInfo.publishBadgeBackground()`

这些方法可以先照搬现有 `WorkInfo` 的业务判断，但常量建议用新的 `CreatorPublishStatus` / `CreatorCoverBadgeType`，不要让新模型反向依赖旧内容模型。

### 5.2 CreatorEpisodeInfo

字段应以 FastFilm `EpisodeInfo` 为准：

```kotlin
data class CreatorEpisodeInfo(
    var workId: String? = null,
    val storyId: String? = null,
    val storyTitle: String? = null,
    val storySummary: String? = null,
    val storyContent: String? = null,
    val sortNo: Int? = null,
    val stepProgress: Int? = null,
    val stepStatus: Int? = null,
    val storyboardTotal: Int? = null,
    val videoUrl: String? = null,
    val duration: Long? = null,
    val workRatio: String? = null,
    val playCount: Int? = null,
    val workCoverUrl: String? = null,
    val hasWatermark: Boolean? = null,
    val generationFailedFlag: Int? = null,
    val stepProgressDetail: Int? = null,
    val stepProgressSubTitle: String? = null,
    val publishStatus: Int? = null,
    val rejectReason: String? = null,
    val coverBadgeType: Int? = null
)
```

剧集上的发布审核字段也必须归到 `CreatorEpisodeInfo`，因为作品详情剧集列表需要展示单集的草稿、未发行、审核中、审核失败、已发行角标，也会用它判断删除限制和驳回原因。

同样建议迁移现有 `EpisodeInfo` 上的发布判断：

- `CreatorEpisodeInfo.isPublishedOrUnderReview()`
- `CreatorEpisodeInfo.publishBadgeText()`
- `CreatorEpisodeInfo.publishBadgeTextColor()`
- `CreatorEpisodeInfo.publishBadgeBackground()`

不要放这些旧内容消费字段：

- `lockStatus`
- `costNums`
- `liked`
- `likeCount`

如果某个创作页面需要点赞或展示量，单独设计 `CreatorEpisodeStats`，不要复用内容解锁播放器字段。

### 5.3 CreatorPlayableEpisode

播放器最终只消费这个：

```kotlin
data class CreatorPlayableEpisode(
    val episode: CreatorEpisodeInfo,
    val workContext: CreatorWorkContext,
    val source: CreatorPlaySource,
    val feedKey: String,
    val feedIndex: Long
)
```

`CreatorWorkContext` 存作品展示上下文：

```kotlin
data class CreatorWorkContext(
    val workId: String?,
    val userId: String?,
    val workTitle: String?,
    val workDesc: String?,
    val workCoverUrl: String?,
    val workRatio: String?,
    val totalEpisodeCnt: Int?,
    val playCount: Int?,
    val totalEarnings: String?,
    val shelfTime: Long?,
    val platformLinks: List<CreatorPlatformInfo>?
)
```

## 6. TopEarners 与 MyCreations 能不能混用

可以混用同一套 `CreatorWorkInfo / CreatorEpisodeInfo / CreatorPlayableEpisode`，但不建议混用同一个分页数据源。

### 6.1 能混用的部分

可以共用：

- 数据模型
- mapper
- 新播放器 Adapter
- PlayerManager
- ListController
- `CreatorPlayableEpisode` UI 绑定
- 发布审核字段与判断方法

原因：

- TopEarners 和 MyCreations 未来都会保持 FastFilm 字段结构。
- 两者最终播放单元都是 episode。
- TopEarners 当前一作品一集，本质是多作品单集 feed。
- MyCreations/作品详情是单作品多剧集 feed。
- 发布审核发生在未发行创作阶段，属于 MyCreations/作品详情的 Creator 业务；TopEarners 不一定展示这些字段，但模型可以用 nullable 字段承载。

### 6.2 不应混用的部分

不建议共用同一个 Room 表或同一个 cached paging flow。

原因：

| 来源 | 接口 | 列表语义 | 分页顺序 |
| --- | --- | --- | --- |
| TopEarners | `recommendWorkList(PagedBody)` | 收益作品榜/推荐作品 | 推荐排序 |
| MyCreations | `getMyWork(MyWorkBody)` | 我的作品列表 | 我的作品排序 |
| 作品详情剧集 | `episodeList(workId)` | 单作品剧集 | 集数排序 |

这些 feed 可以用同一个播放器，但要区分 source：

```kotlin
enum class CreatorPlaySource {
    TOP_EARNERS,
    MY_CREATIONS,
    WORK_SERIES
}
```

数据源建议：

```text
TopEarnersCreatorFeedRepository
MyCreationsCreatorFeedRepository
WorkSeriesCreatorFeedRepository
```

或者做一个统一 repository，但内部按 `CreatorPlaySource + sourceKey` 分发不同表和不同 flow。

## 7. 同源数据源要求

“列表和播放器里的数据源要是同一个，一边分页加载另一边也更新”这个要求要按 source 落地。这里的“同源”只在同一个 source 内成立，不跨 TopEarners 推荐作品和首页内容区。

### 7.1 TopEarners 推荐作品

TopEarners 推荐作品可以做到列表和播放器真正同源。这里同源的底层 canonical feed 应该是 `CreatorWorkInfo`，不要过早把 paging source 变成 `CreatorPlayableEpisode`：

```text
recommendWorkList
-> TopEarners RemoteMediator
-> top_earners_creator_feed Room 表
-> cached Flow<PagingData<CreatorWorkInfo>>
-> TopEarnersAdapter
-> CreatorPlayerActivity
```

映射规则：

```text
TopEarnerWorkInfo -> CreatorWorkInfo
CreatorWorkInfo.workStoryItemList.firstOrNull() -> CreatorPlayableEpisode
```

因为一部作品只有一集，所以 TopEarners 作品列表和播放器 episode feed 是一一对应的；但转换到 `CreatorPlayableEpisode` 应该发生在 repository 派生播放 feed 或播放器入口层，不放进 paging source。

点击路径：

```text
TopEarnersAdapter
-> CreatorPlayerActivity.start(source = TOP_EARNERS, position = currentPosition)
```

不经过 `HomeWorkDetailActivity`，也不需要详情页本地播放器。

首页内容区不走这个 source，不共享这个 repository，也不共享这个播放器。

### 7.2 MyCreations

MyCreations 的作品列表是作品级分页：

```text
getMyWork
-> Flow<PagingData<CreatorWorkInfo>>
```

播放器如果是从某个作品进入，实际要切的是该作品下的 episode：

```text
episodeList(workId)
-> Flow<PagingData<CreatorEpisodeInfo>>
-> Flow<PagingData<CreatorPlayableEpisode>>
```

因此 MyCreations “作品列表”和“播放器剧集列表”不是同一个 feed；不能强行要求它们共用一个 `PagingData<CreatorPlayableEpisode>`。它们可以共享模型和播放器，但分页源应该分开。

MyCreationsFragment 当前列表交互、编辑/删除/生成中轮询等逻辑不用作为本轮播放方案的改造重点；后续主要把 `WorkInformationInfo(work = WorkInfo)` 的数据模型切到 Creator 体系，并把点击播放从旧 `DramasPlayPagerActivity` 切到 `CreatorPlayerActivity`。

因此 MyCreations 的改造边界是：列表仍然是“我的作品”管理列表，分页、筛选、编辑、删除、生成状态等原业务不为播放器重写；只有列表承载的数据模型切到新的 `CreatorWorkInfo`，以及进入可播放剧集时切到 `WorkSeriesCreatorFeedRepository(workId)` 和新创作播放器。

MyCreations 单集作品点击后不做跨作品上下切换，只播放当前作品自己的可播放剧集；如果该作品只有一集，播放器里就只有这一条。除 TopEarners 推荐作品外，其他入口都不跨作品切换。

### 7.3 作品详情剧集列表

作品详情剧集列表和播放器可以同源：

```text
episodeList(workId)
-> WorkSeriesCreatorFeedRepository(sourceKey = workId)
-> cached Flow<PagingData<CreatorPlayableEpisode>>
-> 剧集列表 Adapter
-> CreatorPlayerActivity
```

这样在剧集列表加载下一页时，播放器能同步看到；播放器加载下一页时，剧集列表也能同步看到。

## 8. 播放器逻辑影响

### 8.1 可以复用的思想

可以参考 FastFilm：

- `FeedPlayerManager` 的单播放器多 ViewHolder attach 模式
- `FeedListController` 的 `PagerSnapHelper` 上下切
- `pendingFirstSeekPosition` 首次 seek
- 播放完成 `smoothScrollToNext()`
- 下一条视频预加载

### 8.2 不应复用的旧本项目逻辑

新创作播放器不要接：

- `DramasPlayPagerViewModel.loadDrama()`
- `episodeDirectoryPage`
- `simpleWorkInfo`
- `userPlayerState`
- `episodePlayInfo`
- `storyUnlockPreload`
- `EpisodeUnlockController`
- 首充/广告解锁
- 内容点赞收藏
- 内容续播/进度上报
- `nextWorkRecommend`
- 选集入口和选集弹窗
- 播放器空白区点击后 3 秒自动隐藏 view
- 本项目旧播放器的中间播放按钮、右侧互动栏、收藏/点赞按钮联动

这些属于内容解锁体系。

### 8.3 UI 和交互必须保持 FastFilm

创作流程出来的作品和剧集，在新播放器里的显示和交互都按 FastFilm 播放器，不按本项目旧播放器。

保留：

- 作品/剧集标题
- 简介或平台区，按 FastFilm 规则展示
- 进度条
- 播放/暂停
- buffering/loading
- 上下滑切换
- 播放完成自动切下一条

不做：

- 选集按钮
- 选集弹窗
- 点赞
- 收藏
- 右侧互动栏
- 3 秒后自动隐藏标题、进度条等 view
- 旧播放器的锁集遮罩、解锁弹窗、首充/广告解锁

也就是说，新创作播放器的 UI 密度、作品间距、标题位置、进度条和基础控制都以 FastFilm 为准。即使数据核心用 `CreatorEpisodeInfo`，表现层也不能套用本项目旧 `feature/play/item_video_feed.xml` 的交互规则。

### 8.4 播放器判断字段

新播放器播放判断只需要：

```text
episode.storyId
episode.videoUrl
episode.duration
episode.workRatio
episode.workId
workContext.workCoverUrl
```

列表展示才需要：

```text
workContext.workTitle
workContext.workDesc
workContext.totalEarnings
workContext.platformLinks
workContext.totalEpisodeCnt
```

不要用 `lockStatus` 判断能不能播。创作播放体系下，能播与否只看 `videoUrl` 是否存在，或创作状态是否允许播放。

发布审核字段不参与新播放器的播放交互判断。它们主要服务 MyCreations 和作品详情列表上的角标、驳回原因、发行按钮、删除限制；播放器进入后仍按 FastFilm 播放表现，不因为审核字段接入旧内容解锁 UI。

## 9. 严格隔离设计

新旧体系即使实体类都放在 `core.network.apis.entity` 包下，也要按“入口、模型、repository、播放器”四层隔离，避免后续扩展再次大重构。

### 9.1 类型隔离

新创作体系只允许使用：

```text
CreatorWorkInfo
CreatorEpisodeInfo
CreatorPlatformInfo
CreatorWorkContext
CreatorPlayableEpisode
CreatorPlaySource
CreatorPublishStatus
CreatorCoverBadgeType
```

旧内容/发行消费体系继续使用：

```text
WorkVOInfo
WorkInfo
EpisodeInfo
DramasPlayPagerActivity
DramasPlayPagerViewModel
FeedRepository
```

隔离规则：

- 新创作播放器、TopEarners 推荐作品 feed、MyCreations 新模型、作品详情剧集播放，不直接依赖旧 `WorkInfo/EpisodeInfo`。
- 旧内容播放器不反向依赖 `Creator*`。
- `TopEarnerWorkInfo` 只作为接口 DTO 或过渡 DTO，在 mapper 层转成 `CreatorPlayableEpisode` 后不继续向播放器扩散。
- 发布审核字段属于 `CreatorWorkInfo/CreatorEpisodeInfo`，但内容解锁字段不进入 Creator 模型。

### 9.2 Mapper 隔离

所有不同接口结构都通过 mapper 收敛到 Creator 播放单元：

```text
TopEarnerWorkInfo
-> TopEarnersCreatorMapper
-> CreatorPlayableEpisode

CreatorWorkInfo + CreatorEpisodeInfo
-> WorkSeriesCreatorMapper
-> CreatorPlayableEpisode
```

mapper 是唯一允许接触源 DTO 差异的地方。播放器、Adapter、PlayerManager 不关心数据来自 TopEarners、MyCreations 还是作品详情剧集列表，只消费 `CreatorPlayableEpisode`。

### 9.3 Source 隔离

`CreatorPlaySource` 只描述创作播放 source，不描述首页内容区：

```kotlin
enum class CreatorPlaySource {
    TOP_EARNERS,
    MY_CREATIONS,
    WORK_SERIES
}
```

source 行为边界：

| Source | 是否跨作品上下切 | 数据范围 |
| --- | --- | --- |
| `TOP_EARNERS` | 是 | TopEarners 推荐作品 feed，一作品一集 |
| `MY_CREATIONS` | 否 | 当前 MyCreations 点击作品自己的可播放剧集 |
| `WORK_SERIES` | 否 | 当前作品详情的剧集列表 |

除 `TOP_EARNERS` 外，其他 source 都不跨作品切换。首页内容区不加入这个 enum。

### 9.4 Repository 隔离

推荐保留 source-specific repository，避免一个“大一统 repository”后续被塞入首页、解锁、审核、推荐等多套规则：

```text
TopEarnersCreatorFeedRepository
MyCreationsCreatorFeedRepository
WorkSeriesCreatorFeedRepository
```

如果实现上需要统一入口，也只做薄分发：

```text
CreatorFeedRepository
-> when (CreatorPlaySource)
   -> TopEarnersCreatorFeedRepository
   -> MyCreationsCreatorFeedRepository
   -> WorkSeriesCreatorFeedRepository
```

统一入口不能持有旧内容播放器的 `FeedRepository`，也不能把首页内容区塞进 `CreatorPlaySource`。

### 9.5 播放器隔离

新创作播放器建议独立命名：

```text
CreatorPlayerActivity
CreatorVideoFeedAdapter
CreatorFeedPlayerManager
CreatorFeedListController
```

代码目录也建议独立，避免直接塞进旧 `feature/play` 根目录。可选：

```text
feature/creatorplay/
```

如果最终为了复用包结构放在 `feature/play/creator/`，也要保持子目录独立，不和旧 `DramasPlayPager*`、旧 `VideoFeedAdapter`、旧 `FeedRepository` 混放。

可以参考 FastFilm 的实现方式，但不要复用本项目旧播放器类名和状态模型。隔离目标：

- `CreatorPlayerActivity` 不调用 `DramasPlayPagerViewModel.loadDrama()`。
- `CreatorVideoFeedAdapter` 不绑定旧 `VideoFeedAdapter` 的选集、点赞收藏、解锁 UI。
- `CreatorFeedPlayerManager` 只处理播放、进度、loading、上下切和自动下一条。
- 发布审核字段不驱动播放器 UI，只由 MyCreations/作品详情列表和发行入口使用。

### 9.6 静态检查规则

开发时可以用静态搜索做边界检查：

```powershell
rg -n "WorkInfo|EpisodeInfo|DramasPlayPager|FeedRepository" app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
rg -n "CreatorWorkInfo|CreatorEpisodeInfo|CreatorPlayableEpisode" app/src/main/java/com/kuaigeglobal/epnova/feature/play/DramasPlayPagerActivity.kt app/src/main/java/com/kuaigeglobal/epnova/feature/play/DramasPlayPagerViewModel.kt app/src/main/java/com/kuaigeglobal/epnova/feature/play/VideoFeedAdapter.kt app/src/main/java/com/kuaigeglobal/epnova/feature/play/data/FeedRepository.kt
```

期望结果：

- 新创作播放器目录不出现旧 `WorkInfo/EpisodeInfo/DramasPlayPager/FeedRepository`。
- 旧内容播放器目录不出现 `Creator*`。
- 如果 mapper 目录出现新旧类型并存，必须是明确的转换函数，不把源 DTO 继续传给 UI 或播放器。

## 10. 建议落地顺序

### 阶段 1：模型隔离

1. 新增 `CreatorWorkInfo / CreatorEpisodeInfo / CreatorPlatformInfo`。
2. 新增 `CreatorPlayableEpisode / CreatorWorkContext / CreatorPlaySource`。
3. 把未发行创作阶段的发布审核字段和判断方法迁到 `CreatorWorkInfo/CreatorEpisodeInfo`，包括发布状态、全局发布状态、角标、驳回原因、已发行集数、发行入口和发行按钮判断。
4. `recommendWorkList` 先可以继续返回 `TopEarnerWorkInfo`，但进入 creator paging source 后立刻 mapper 成 `CreatorWorkInfo`；`CreatorPlayableEpisode` 由播放器 feed 再派生。
5. 不强制所有接口马上统一 DTO；允许 TopEarners、MyCreations、作品详情剧集列表保持不同结构，只要进入新创作播放器前统一映射到 `CreatorPlayableEpisode`。
6. 首页内容区不纳入新创作播放器模型合并，不和 TopEarners 推荐作品共享 source。

### 阶段 2：TopEarners 推荐作品同源 feed

1. 新增 TopEarners 推荐作品专用 Room 表和 RemoteMediator。
2. Repository 使用 `repositoryScope.cachedIn()` 保存 `Flow<PagingData<CreatorWorkInfo>>`。
3. `TopEarnersAdapter` collect work feed；新播放器从同一个 work feed 派生 `CreatorPlayableEpisode`，不重新请求接口。
4. TopEarners 推荐作品 item 点击直接进入 `CreatorPlayerActivity`，不加 `HomeWorkDetailActivity`，也不再进旧 `DramasPlayPagerActivity`。

### 阶段 3：新创作播放器

1. 新增 `CreatorPlayerActivity`。
2. Adapter 绑定 `CreatorPlayableEpisode`。
3. PlayerManager 播 `CreatorEpisodeInfo.videoUrl`。
4. UI 和交互按 FastFilm 播放器实现，只保留标题、简介/平台区、进度条、播放暂停、loading 和上下滑。
5. 不接内容解锁逻辑，不做选集、点赞收藏和 3 秒隐藏 view。

### 阶段 4：MyCreations / 作品详情接入

1. MyCreationsFragment 的列表业务逻辑保持现状，主要把承载数据改为新 `CreatorWorkInfo`。
2. 作品详情剧集列表改为 `CreatorEpisodeInfo`。
3. MyCreations 和作品详情中现有的发布审核 UI、发行按钮、驳回原因、删除限制等逻辑改为读取 Creator 模型，不再依赖旧 `core.network.apis.entity.WorkInfo/EpisodeInfo`。
4. 当前 MyCreations/作品详情里的剧集播放入口切到 `CreatorPlayerActivity`，不再走旧 `DramasPlayPagerActivity`。
5. 剧集列表和播放器通过 `WorkSeriesCreatorFeedRepository(workId)` 做同源分页。
6. MyCreations 单集作品只播放自己，不新增“我的作品列表跨作品播放” source。

## 11. 文档清理说明

已删除前面的中间草稿，只保留本文。之前的 WorkInfo-first 和 Episode-first 草稿都只是推导过程，最终以本文的“两套体系隔离 + CreatorEpisode 播放核心”方案为准。

## 12. 本次静态核对范围

Epnova：

- `core/network/apis/entity/TopEarnerWorkInfo.kt`
- `core/network/apis/entity/WorkInfo.kt`
- `core/db/entity/WorkInformationInfo.kt`
- `feature/create/child/TopEarnersAdapter.kt`
- `feature/create/child/TopEarnersViewModel.kt`
- `feature/create/child/TopEarnersPagingSource.kt`
- `feature/mine/child/MyCreationsFragment.kt`
- `feature/works/MyWorksViewModel.kt`
- `feature/works/WorkInformationRemoteMediator.kt`
- `feature/works/MyWorksAdapter.kt`
- `feature/series/SeriesActivity.kt`
- `feature/play/DramasPlayPagerViewModel.kt`
- `feature/play/data/FeedRepository.kt`
- `feature/play/VideoFeedAdapter.kt`
- `feature/play/manger/FeedPlayerManager.kt`
- `feature/play/manger/FeedListController.kt`
- `core/network/apis/ApiService.kt`

FastFilm：

- `core/network/apis/entity/WorkInfo.kt`
- `core/network/apis/entity/PlatformInfo.kt`
- `core/db/entity/WorkInformationInfo.kt`
- `feature/works/WorkInformationRemoteMediator.kt`
- `feature/works/MyWorksViewModel.kt`
- `feature/works/MyWorksFragment.kt`
- `feature/series/SeriesActivity.kt`
- `feature/play/VideoFeedAdapter.kt`
- `feature/play/manger/FeedPlayerManager.kt`
- `feature/play/manger/FeedListController.kt`

未运行 Gradle；本文只做分析和方案整理。
