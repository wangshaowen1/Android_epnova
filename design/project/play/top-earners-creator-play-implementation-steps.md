# TopEarners 创作播放器分步实现计划

整理时间：2026-06-04
关联方案：`design/project/play/top-earners-creator-play-final-analysis.md`
范围：实现步骤设计与执行进度记录。

## 0. 总原则

这次改造必须分步骤做，不适合一次性写完。

原因：

- 会同时涉及实体、发布审核字段、分页数据源、新播放器、TopEarners 点击链路、MyCreations/作品详情播放入口。
- 新旧 `WorkInfo/EpisodeInfo` 同包存在，误 import 的风险很高。
- 旧内容播放器和新创作播放器必须严格隔离，一次性大改会让问题难定位。

默认执行方式：

- 每完成一个阶段先做静态检查，再汇报改了什么、检查了什么、发现什么风险。
- 默认不跑 Gradle；如果需要编译验证，再单独确认后跑。
- 不改无关旧逻辑，不回退当前工作区已有改动。
- 代码实现阶段如果发现某一步需要额外扩大范围，先停下来说明，不继续硬写。
- 后续新增或修改字段、常量、业务判断函数、跨模型转换函数时必须写注释；注释要说明业务含义和边界，不写无意义复述。

## 1. 阶段一：Creator 实体和发布审核能力

### 1.1 改动目标

先把新旧模型边界立住，不接播放器。

新增实体都放在现有包：

```text
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/
```

新增文件建议：

```text
CreatorWorkInfo.kt
CreatorEpisodeInfo.kt
CreatorPlatformInfo.kt
CreatorWorkContext.kt
CreatorPlayableEpisode.kt
CreatorPlaySource.kt
CreatorPublishStatus.kt
CreatorCoverBadgeType.kt
```

### 1.2 具体要做

1. `CreatorWorkInfo` 以 FastFilm `WorkInfo` 为主，补齐本项目未发行创作阶段需要的发布审核字段。
2. `CreatorEpisodeInfo` 以 FastFilm `EpisodeInfo` 为主，补齐剧集发布审核字段。
3. 把现有 `WorkInfo/EpisodeInfo` 上和发布审核相关的判断迁到 Creator 体系：
   - `isPublishedOrUnderReview()`
   - `hasPublishedOrUnderReviewEpisode()`
   - `showPublishButton()`
   - `isPublishButtonEnabled()`
   - `isPublishReviewFailed()`
   - `publishBadgeText()`
   - `publishBadgeTextColor()`
   - `publishBadgeBackground()`
4. Creator 判断里不要反向依赖旧 `WorkInfo` 常量，改用 `CreatorPublishStatus` / `CreatorCoverBadgeType`。
5. 不删除旧 `WorkInfo/EpisodeInfo` 的方法，避免旧链路被破坏。

### 1.3 完成后检查

静态检查：

```powershell
rg -n "WorkInfo\\.|EpisodeInfo\\.|PUBLISH_STATUS_|COVER_BADGE_" app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity -g "Creator*.kt"
rg -n "lockStatus|costNums|liked|likeCount" app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity -g "Creator*.kt"
```

期望：

- `Creator*.kt` 不依赖旧 `WorkInfo` / `EpisodeInfo` 常量。
- `Creator*.kt` 不包含内容消费字段：`lockStatus/costNums/liked/likeCount`。
- 旧 `WorkInfo/EpisodeInfo` 还在，旧代码不受影响。

### 1.4 停下来确认

这一阶段完成后先停，确认实体字段和发布审核判断是否够用。确认后再做 mapper。

## 2. 阶段二：Mapper 边界

### 2.1 改动目标

建立唯一的数据转换边界。不同接口结构可以不同，但进新播放器前必须统一为：

```text
CreatorPlayableEpisode = CreatorEpisodeInfo + CreatorWorkContext
```

### 2.2 具体要做

新增 mapper，建议放新创作播放模块下，例如：

```text
app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/mapper/
```

建议 mapper：

```text
TopEarnersCreatorMapper.kt
CreatorWorkSeriesMapper.kt
LegacyCreatorBridgeMapper.kt
```

职责：

- `TopEarnersCreatorMapper`：`TopEarnerWorkInfo -> CreatorWorkInfo`，并保留 TopEarners 一作品一集到 `CreatorPlayableEpisode` 的转换能力供播放器层使用。
- `CreatorWorkSeriesMapper`：`CreatorWorkInfo + CreatorEpisodeInfo -> CreatorPlayableEpisode`。
- `LegacyCreatorBridgeMapper`：临时桥接当前接口还没完全切 Creator 时的旧 `WorkInfo/EpisodeInfo -> CreatorWorkInfo/CreatorEpisodeInfo`。

`LegacyCreatorBridgeMapper` 只能在迁移阶段使用，不能让旧 DTO 继续传到播放器、Adapter 或 PlayerManager。

### 2.3 完成后检查

静态检查：

```powershell
rg -n "import .*TopEarnerWorkInfo|import .*TopEarnerWorkStoryItem|import .*TopEarnerPlatformLinkItem|import .*\\.WorkInfo|import .*\\.EpisodeInfo" app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
rg -n "CreatorPlayableEpisode" app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
```

期望：

- 旧 `WorkInfo/EpisodeInfo` 只出现在 mapper 或临时 bridge 里。
- `TopEarnerWorkInfo` 不出现在播放器、Adapter、PlayerManager。
- 播放相关代码只消费 `CreatorPlayableEpisode`。

### 2.4 停下来确认

确认 mapper 输出是否能覆盖三个入口：

- TopEarners 推荐作品，一作品一集。
- MyCreations 单集作品，只播放自己。
- 作品详情剧集列表，只在当前作品剧集内切换。

## 3. 阶段三：TopEarners 同源 feed

### 3.1 改动目标

先把 TopEarners 推荐作品列表数据源切到 `CreatorWorkInfo`，保证列表和播放器后续吃同一个 creator work source。播放器需要播放时，再从同一个 work feed 派生 `CreatorPlayableEpisode`。

### 3.2 具体要做

1. 保留 `recommendWorkList` 接口当前返回结构，不要求后端立即统一 DTO。
2. 新增 TopEarners 专用 creator feed repository，建议：

```text
TopEarnersCreatorFeedRepository
```

3. paging source 内部把 `TopEarnerWorkInfo` 映射为 `CreatorWorkInfo`，不把播放器播放单元提前塞进分页源。
4. `TopEarnersAdapter` 绑定 `CreatorWorkInfo`，列表保持作品维度展示。
5. 后续新播放器从同一个 `TopEarnersCreatorFeedRepository.getWorkFeed()` 派生 `CreatorPlayableEpisode`，不重新请求接口。
5. TopEarners 是唯一跨作品上下切换 source：

```kotlin
CreatorPlaySource.TOP_EARNERS
```

### 3.3 完成后检查

静态检查：

```powershell
rg -n "DramasPlayPagerActivity|HomeWorkDetailActivity|FeedRepository" app/src/main/java/com/kuaigeglobal/epnova/feature/create
rg -n "CreatorWorkInfo|TopEarnersCreatorFeedRepository|getWorkFeed" app/src/main/java/com/kuaigeglobal/epnova/feature/create app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
```

期望：

- TopEarners 新链路不再准备旧播放器数据。
- TopEarners 列表使用 `CreatorWorkInfo` work feed，不在 paging source 内提前转换 `CreatorPlayableEpisode`。
- 还没接点击前，可以暂时保留旧点击入口，但要明确下一阶段移除。
- 首页内容区不接入这个 repository。

### 3.4 停下来确认

确认 TopEarners 列表展示没有字段缺口。确认后再做播放器骨架。

## 4. 阶段四：新创作播放器骨架

### 4.1 改动目标

新建独立播放器，不复用旧 `DramasPlayPager*` 状态模型。

建议目录：

```text
app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/
```

建议类：

```text
CreatorPlayerActivity
CreatorPlayerViewModel
CreatorVideoFeedAdapter
CreatorFeedPlayerManager
CreatorFeedListController
```

### 4.2 具体要做

1. 参考 FastFilm 播放器结构实现上下滑播放。
2. Adapter 只绑定 `CreatorPlayableEpisode`。
3. PlayerManager 只处理：
   - 播放/暂停
   - loading
   - 进度条
   - 上下切换
   - 播放完成自动下一条
4. UI 和交互按 FastFilm，不能接：
   - 选集
   - 点赞
   - 收藏
   - 右侧互动栏
   - 3 秒隐藏 view
   - 锁集遮罩
   - 解锁弹窗
   - 首充/广告解锁

### 4.3 完成后检查

静态检查：

```powershell
rg -n "DramasPlayPager|EpisodeUnlock|storyUnlockPreload|episodePlayInfo|userPlayerState|lockStatus|costNums|liked|likeCount|EpisodeInfo|WorkInfo" `
  app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/CreatorPlayerActivity.kt `
  app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/CreatorPlayerViewModel.kt `
  app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/CreatorVideoFeedAdapter.kt `
  app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/CreatorFeedPlayerManager.kt `
  app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/CreatorFeedListController.kt
rg -n "CreatorWorkInfo|CreatorEpisodeInfo|CreatorPlayableEpisode" app/src/main/java/com/kuaigeglobal/epnova/feature/play/DramasPlayPagerActivity.kt app/src/main/java/com/kuaigeglobal/epnova/feature/play/DramasPlayPagerViewModel.kt app/src/main/java/com/kuaigeglobal/epnova/feature/play/VideoFeedAdapter.kt app/src/main/java/com/kuaigeglobal/epnova/feature/play/data/FeedRepository.kt
```

期望：

- 新创作播放器骨架文件不出现旧解锁、点赞、旧播放器状态。
- 旧播放器文件不出现 `Creator*`。
- 发布审核字段不驱动播放器 UI。

说明：`feature/creatorplay/mapper/LegacyCreatorBridgeMapper` 是旧 DTO 到 Creator 模型的迁移桥接层，会显式出现旧模型和旧字段过滤注释，不纳入播放器骨架禁入扫描。

### 4.4 停下来确认

确认播放器骨架和 FastFilm 表现一致，再接真实入口。

## 5. 阶段五：TopEarners 点击进入新播放器

### 5.1 改动目标

TopEarners 推荐作品点击直接进入新创作播放器，并支持跨作品上下切换。

### 5.2 具体要做

1. `TopEarnersAdapter` 点击从旧播放器入口切到：

```text
CreatorPlayerActivity.start(source = TOP_EARNERS, position = currentPosition)
```

2. 不经过 `HomeWorkDetailActivity`。
3. 不再调用旧 `DramasPlayPagerActivity`。
4. 播放器 collect TopEarners 同一个 creator feed。

### 5.3 完成后检查

静态检查：

```powershell
rg -n "HomeWorkDetailActivity|DramasPlayPagerActivity|setPreloadedSeries" app/src/main/java/com/kuaigeglobal/epnova/feature/create
rg -n "CreatorPlayerActivity|TOP_EARNERS" app/src/main/java/com/kuaigeglobal/epnova/feature/create app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
```

手动验证建议：

- TopEarners 列表能显示。
- 点击任意 item 进入新播放器。
- 上下滑只在 TopEarners feed 内跨作品切。
- 播放完成能自动切下一条。
- 没有选集、点赞收藏、解锁弹窗、3 秒隐藏。

### 5.4 停下来确认

这是第一个真实入口。完成后建议用户实际跑一次页面验证，再继续 MyCreations。

## 6. 阶段六：MyCreations 和作品详情数据结构迁移

### 6.1 改动目标

MyCreations 列表逻辑不重写，只把承载数据切到 Creator 体系；作品详情剧集列表切到 `CreatorEpisodeInfo`。

### 6.2 具体要做

1. `WorkInformationInfo` 或对应列表承载结构改为持有 `CreatorWorkInfo`。
2. `MyWorksAdapter` / item VM 读取 `CreatorWorkInfo` 字段。
3. MyCreations 现有编辑、删除、生成中轮询、发布审核角标等逻辑保持原行为。
4. 作品详情剧集列表改为读取 `CreatorEpisodeInfo`。
5. 发布审核 UI、发行按钮、驳回原因、删除限制改读 Creator 模型方法。

### 6.3 完成后检查

静态检查：

```powershell
rg -n "core.network.apis.entity.WorkInfo|core.network.apis.entity.EpisodeInfo" app/src/main/java/com/kuaigeglobal/epnova/feature/mine app/src/main/java/com/kuaigeglobal/epnova/feature/works app/src/main/java/com/kuaigeglobal/epnova/feature/series
rg -n "CreatorWorkInfo|CreatorEpisodeInfo|publishBadgeText|isPublishedOrUnderReview|showPublishButton" app/src/main/java/com/kuaigeglobal/epnova/feature/mine app/src/main/java/com/kuaigeglobal/epnova/feature/works app/src/main/java/com/kuaigeglobal/epnova/feature/series
```

期望：

- MyCreations/作品详情主链路不再依赖旧 `WorkInfo/EpisodeInfo`。
- 旧内容播放器相关代码不被改动。
- 发布审核展示和按钮逻辑仍在。

### 6.4 停下来确认

这一阶段最容易牵动 Room、Paging、Adapter 和发布审核 UI。完成后必须先静态复核改动范围，再决定是否跑一次编译。

## 7. 阶段七：MyCreations / 作品详情播放入口切新播放器

### 7.1 改动目标

把剧集播放入口切到新创作播放器，但不跨作品切换。

### 7.2 具体要做

1. 新增或完善：

```text
WorkSeriesCreatorFeedRepository(workId)
```

2. MyCreations 单集作品点击后：

```text
source = MY_CREATIONS
sourceKey = workId
```

只播放当前作品自己的可播放剧集。如果只有一集，播放器里只有一条。

3. 作品详情剧集列表点击后：

```text
source = WORK_SERIES
sourceKey = workId
position = episodePosition
```

只在当前作品剧集内上下切换。

4. 移除这些入口到旧播放器的调用：

```text
DramasPlayPagerActivity
FeedRepository.setPreloadedSeries
```

### 7.3 完成后检查

静态检查：

```powershell
rg -n "DramasPlayPagerActivity|setPreloadedSeries|FeedRepository" app/src/main/java/com/kuaigeglobal/epnova/feature/mine app/src/main/java/com/kuaigeglobal/epnova/feature/series
rg -n "MY_CREATIONS|WORK_SERIES|WorkSeriesCreatorFeedRepository|CreatorPlayerActivity" app/src/main/java/com/kuaigeglobal/epnova/feature/mine app/src/main/java/com/kuaigeglobal/epnova/feature/series app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
```

手动验证建议：

- MyCreations 单集作品只能播放自己。
- MyCreations 多集作品只在当前作品剧集内切。
- 作品详情剧集列表只在当前作品剧集内切。
- 不会切到其他作品。
- 没有旧播放器 UI。

### 7.4 停下来确认

确认 MyCreations 和作品详情入口后，再做全局隔离检查。

## 8. 阶段八：全局隔离复核和收尾

### 8.1 改动目标

确认新旧体系没有互相污染。

### 8.2 必做检查

静态检查：

```powershell
rg -n "DramasPlayPager|EpisodeUnlock|storyUnlockPreload|episodePlayInfo|userPlayerState|lockStatus|costNums|liked|likeCount" app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
rg -n "CreatorWorkInfo|CreatorEpisodeInfo|CreatorPlayableEpisode|CreatorPlayerActivity" app/src/main/java/com/kuaigeglobal/epnova/feature/play
rg -n "HomeWorkDetailActivity|DramasPlayPagerActivity|setPreloadedSeries" app/src/main/java/com/kuaigeglobal/epnova/feature/create app/src/main/java/com/kuaigeglobal/epnova/feature/mine app/src/main/java/com/kuaigeglobal/epnova/feature/series
rg -n "TOP_EARNERS|MY_CREATIONS|WORK_SERIES|HOME_CONTENT" app/src/main/java/com/kuaigeglobal/epnova
```

期望：

- 新创作播放器不含旧解锁/点赞/旧播放器状态。
- 旧 `feature/play` 不依赖 Creator。
- TopEarners 不再走 `HomeWorkDetailActivity` 或旧播放器。
- MyCreations/作品详情不再用 `setPreloadedSeries` 进旧播放器。
- 不存在 `HOME_CONTENT` 被塞进 `CreatorPlaySource`。

### 8.3 编译验证建议

默认我不直接跑 Gradle。如果要做完整验证，建议在阶段八由用户确认后再跑：

```powershell
./gradlew :app:assembleDebug
```

如果工程有指定 flavor 或构建变体，以当前分支实际脚本为准，不猜命令。

### 8.4 最终人工验证清单

- TopEarners 推荐作品：可跨作品上下切。
- MyCreations 单集作品：只播放自己。
- MyCreations 多集作品：只在当前作品剧集内切。
- 作品详情剧集列表：只在当前作品剧集内切。
- 首页内容区：不受影响，不接入 Creator 播放体系。
- 发布审核角标、发行按钮、驳回原因、删除限制仍可用。
- 新播放器没有选集、点赞收藏、解锁、3 秒隐藏 view。

## 9. 推荐执行节奏

建议按下面节奏推进：

1. 先做阶段一、二，确认模型和 mapper。
2. 再做阶段三、四、五，打通 TopEarners 推荐作品完整链路。
3. TopEarners 验证通过后，再做阶段六、七。
4. 最后做阶段八全局复核。

不要跳过阶段一和阶段二。它们是后面避免大重构的隔离基础。

## 10. 执行进度

### 2026-06-04 阶段一已完成

已新增：

```text
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorWorkInfo.kt
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorEpisodeInfo.kt
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorPlatformInfo.kt
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorWorkContext.kt
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorPlayableEpisode.kt
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorPlaySource.kt
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorPublishStatus.kt
app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/CreatorCoverBadgeType.kt
```

已完成内容：

- `CreatorWorkInfo` 承载 FastFilm 风格作品字段，并补入未发行创作阶段的发布审核字段。
- `CreatorEpisodeInfo` 承载 FastFilm 风格剧集字段，并补入剧集发布审核字段。
- 发布审核判断已迁到 Creator 体系，包括角标文案、角标颜色、角标背景、发布按钮判断、审核中/已发布判断、驳回判断。
- Creator 判断使用 `CreatorPublishStatus` / `CreatorCoverBadgeType`，没有反向依赖旧 `WorkInfo/EpisodeInfo` 常量。
- 旧 `WorkInfo/EpisodeInfo` 未改动。

已做静态检查：

```powershell
rg -n "WorkInfo\\.|EpisodeInfo\\.|PUBLISH_STATUS_|COVER_BADGE_" app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity -g "Creator*.kt"
rg -n "lockStatus|costNums|liked|likeCount" app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity -g "Creator*.kt"
```

检查结果：

- 无旧 `WorkInfo.` / `EpisodeInfo.` 常量依赖。
- 无 `PUBLISH_STATUS_` / `COVER_BADGE_` 旧常量命名。
- 无 `lockStatus/costNums/liked/likeCount` 内容消费字段。

未做：

- 未做 mapper。
- 未接 TopEarners 数据源。
- 未建新播放器。
- 未改 MyCreations / 作品详情入口。
- 未运行 Gradle。

下一步：

- 阶段二：Mapper 边界。

### 2026-06-04 阶段二已完成

已新增：

```text
app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/mapper/TopEarnersCreatorMapper.kt
app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/mapper/CreatorWorkSeriesMapper.kt
app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/mapper/LegacyCreatorBridgeMapper.kt
```

已完成内容：

- `TopEarnersCreatorMapper`：负责 `TopEarnerWorkInfo -> CreatorWorkInfo / CreatorPlayableEpisode`，TopEarners 一作品一集取 `workStoryItemList.firstOrNull()`。
- `CreatorWorkSeriesMapper`：负责 `CreatorWorkInfo + CreatorEpisodeInfo -> CreatorPlayableEpisode`，支持 `WORK_SERIES` 和 `MY_CREATIONS` 两类不跨作品 source。
- `LegacyCreatorBridgeMapper`：临时负责旧 `WorkInfo/EpisodeInfo -> CreatorWorkInfo/CreatorEpisodeInfo`，用于接口未完全切 Creator 前的过渡。
- mapper 输出统一为 `CreatorPlayableEpisode`，播放器后续只消费 Creator 播放单元。

已做静态检查：

```powershell
rg -n "import .*TopEarnerWorkInfo|import .*TopEarnerWorkStoryItem|import .*TopEarnerPlatformLinkItem|import .*\\.WorkInfo|import .*\\.EpisodeInfo" app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
rg -n "CreatorPlayableEpisode" app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
rg -n "DramasPlayPager|FeedRepository|HomeWorkDetailActivity|EpisodeUnlock|lockStatus|costNums|liked|likeCount" app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay
git diff --check -- app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/entity/Creator*.kt design/project/play/top-earners-creator-play-implementation-steps.md
```

检查结果：

- `TopEarnerWorkInfo/TopEarnerWorkStoryItem/TopEarnerPlatformLinkItem` 只出现在 `TopEarnersCreatorMapper`。
- 旧 `WorkInfo/EpisodeInfo` 只出现在 `LegacyCreatorBridgeMapper`。
- `CreatorPlayableEpisode` 只在 mapper 输出层使用，尚未进入播放器。
- `feature/creatorplay` 下没有旧播放器、解锁、点赞收藏相关关键字。
- `git diff --check` 无输出。

未做：

- 未接 TopEarners 数据源。
- 未建新播放器。
- 未改 TopEarners 点击入口。
- 未改 MyCreations / 作品详情入口。
- 未运行 Gradle。

下一步：

- 阶段三：TopEarners 同源 feed。

### 2026-06-04 阶段三已完成

已新增：

```text
app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/data/TopEarnersCreatorFeedRepository.kt
app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay/data/TopEarnersCreatorPagingSource.kt
```

已修改：

```text
app/src/main/java/com/kuaigeglobal/epnova/feature/create/child/TopEarnersViewModel.kt
app/src/main/java/com/kuaigeglobal/epnova/feature/create/child/TopEarnersAdapter.kt
app/src/main/java/com/kuaigeglobal/epnova/feature/create/child/ItemTopEarnersVm.kt
app/src/main/res/layout/item_top_earners.xml
```

已删除：

```text
app/src/main/java/com/kuaigeglobal/epnova/feature/create/child/TopEarnersPagingSource.kt
```

已完成内容：

- TopEarners 推荐作品分页源从 `TopEarnerWorkInfo` 切为 `CreatorWorkInfo`。
- `TopEarnersCreatorPagingSource` 继续请求 `recommendWorkList`，并在 paging source 内通过 `TopEarnersCreatorMapper` 转成 Creator work 模型。
- `TopEarnersCreatorFeedRepository` 持有同一个 cached `Flow<PagingData<CreatorWorkInfo>>`，TopEarners 列表已改为从这个 repository 获取 work feed，后续播放器也应从同一个 work feed 派生播放 feed。
- `TopEarnersAdapter` 改为 `PagingDataAdapter<CreatorWorkInfo, ...>`。
- `ItemTopEarnersVm` 改为承载 `CreatorWorkInfo`，列表 XML 只从 VM 暴露的方法读取封面、标题、播放量、收益。
- `TopEarnerWorkInfo` 不再进入 `feature/create/child` 列表 UI 层，只留在 API DTO 和 TopEarners mapper。

临时保留：

- `TopEarnersAdapter` 的点击入口仍临时保留旧 `DramasPlayPagerActivity.start(...)`，阶段五再替换为 `CreatorPlayerActivity`。
- 这一阶段只完成同源 feed，不建新播放器、不改点击入口。

已做静态检查：

```powershell
rg -n "TopEarnersPagingSource|TopEarnersCreatorPagingSource|TopEarnersCreatorFeedRepository|PagingData<TopEarnerWorkInfo>|PagingData<CreatorWorkInfo>|PagingData<CreatorPlayableEpisode>|PagingDataAdapter<" app/src/main/java/com/kuaigeglobal/epnova/feature/create app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay -g "*.kt"
rg -n "TopEarnersPagingSource|TopEarnersCreatorPagingSource|TopEarnersCreatorFeedRepository|PagingData<TopEarnerWorkInfo>|PagingData<CreatorWorkInfo>|PagingData<CreatorPlayableEpisode>|TopEarnerWorkInfo" app/src/main/java/com/kuaigeglobal/epnova -g "*.kt"
rg -n "DramasPlayPagerActivity|HomeWorkDetailActivity|FeedRepository|setPreloadedSeries" app/src/main/java/com/kuaigeglobal/epnova/feature/create app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay -g "*.kt"
git diff --check -- app/src/main/java/com/kuaigeglobal/epnova/feature/create app/src/main/java/com/kuaigeglobal/epnova/feature/creatorplay app/src/main/res/layout/item_top_earners.xml design/project/play/top-earners-creator-play-implementation-steps.md
```

检查结果：

- `TopEarnersPagingSource` 已无代码引用。
- TopEarners 列表 ViewModel 返回 `Flow<PagingData<CreatorWorkInfo>>`。
- TopEarners 列表 Adapter 已绑定 `CreatorWorkInfo`。
- 后续试改已将 TopEarners 新链路改为直接解析 `CreatorWorkInfo`，旧 `TopEarnerWorkInfo.kt` 已删除。
- `feature/creatorplay` 不依赖 `feature/create/child`。
- `HomeWorkDetailActivity`、`FeedRepository`、`setPreloadedSeries` 没有进入 TopEarners 新 feed。
- `DramasPlayPagerActivity` 只剩 TopEarners 点击入口临时引用，阶段五处理。
- `git diff --check` 无空白错误；当前 Git 输出了 CRLF 换行提示。

未做：

- 未建新播放器。
- 未改 TopEarners 点击入口到新播放器。
- 未改 MyCreations / 作品详情入口。
- 未运行 Gradle。

下一步：

- 阶段四：新创作播放器骨架。

### 2026-06-04 注释补充

本次按要求补齐当前新增代码的注释：

- Creator 实体字段、状态常量和发布审核判断函数。
- TopEarners / WorkSeries / Legacy bridge mapper 的转换边界。
- TopEarners creator feed repository 和 paging source 的同源边界。
- TopEarners ViewModel 共享 feed 入口、Adapter 临时旧播放器入口、平台图标绑定和 Diff key。
- TopEarners item VM 的展示字段来源。

后续执行要求已加入“总原则”：新增或修改字段、常量、业务判断函数、跨模型转换函数时必须写注释，并说明业务含义和边界。

### 2026-06-04 TopEarners 映射优化试改

本次按“以 CreatorWorkInfo / CreatorEpisodeInfo 为主”的方向调整 TopEarners 链路：

- `ApiService.recommendWorkList` 返回类型由 `PagedList<TopEarnerWorkInfo>` 改为 `PagedList<CreatorWorkInfo>`。
- `TopEarnersCreatorPagingSource` 不再做 `TopEarnerWorkInfo -> CreatorWorkInfo` 字段搬运，只调用 `TopEarnersCreatorMapper.normalizeWork()` 做 TopEarners 单集字段兜底。
- `TopEarnersCreatorMapper` 删除 TopEarner DTO 依赖，改为：
  - `normalizeWork(CreatorWorkInfo?)`：补齐 episode 缺失的 workId、标题、简介、比例、播放量和封面。
  - `toPlayableEpisodeOrNull(CreatorWorkInfo?)`：后续播放器从共享 work feed 派生播放单元。
- `LegacyCreatorBridgeMapper` 暂不调整，继续显式桥接旧 `WorkInfo / EpisodeInfo`，用于严格过滤旧内容消费字段。

检查结果：

- `TopEarnerWorkInfo.kt` 已删除，TopEarners 新链路不再保留旧 DTO。
- `toCreatorWorkInfo` 只剩 `LegacyCreatorBridgeMapper` 使用，用于旧 `WorkInfo / EpisodeInfo` 到 Creator 模型的显式隔离。

### 2026-06-04 阶段四进度：新创作播放器骨架

本阶段已完成独立 Creator 播放器骨架：

- 新增 `CreatorPlayerActivity`，注册到 Manifest；阶段四只搭骨架，不接 TopEarners 点击入口。
- 新增 `CreatorPlayerViewModel`，只保存播放来源、初始位置和当前播放单元，不承载旧播放器状态。
- 新增 `CreatorVideoFeedAdapter`，使用 `PagingDataAdapter<CreatorPlayableEpisode>`，便于阶段五直接接 TopEarners 共享分页源。
- 新增 `ItemCreatorVideoFeedVm`，`item_creator_video_feed.xml` 只绑定 item VM，不直接引用实体类。
- 新增 `CreatorFeedPlayerManager`，独立管理 ExoPlayer，只处理播放、暂停、loading、进度、seek 和完播事件。
- 新增 `CreatorFeedListController`，只处理上下滑吸附、当前 item 播放和完播下一条。
- 新增 `activity_creator_player.xml` 和 `item_creator_video_feed.xml`，UI 只保留播放器、封面、标题、进度条、loading/error 和中心播放按钮。

边界说明：

- 阶段四不提交真实 feed；阶段五接 TopEarners 时从 `TopEarnersCreatorFeedRepository.getWorkFeed()` 派生 `CreatorPlayableEpisode` 后提交给播放器 Adapter。
- `activity_creator_player.xml` 使用项目通用 `view_model_toolbar` include；不再手写 StatusBar + 返回按钮。
- Adapter 只做 ViewHolder、点击转发、播放器 View 和 loading 动画启停；封面、标题、时长、稳定 key、播放器状态到 UI 字段的映射都在 `ItemCreatorVideoFeedVm`。
- `CreatorWorkSeriesMapper` / `TopEarnersCreatorMapper` 的 feedKey 已修正为精确到 episode，避免同一作品多集时 Diff 或播放器 mediaId 串位。
- 新播放器骨架不包含选集、点赞、收藏、右侧互动栏、3 秒隐藏、锁集遮罩、解锁弹窗、首充/广告解锁。
- `LegacyCreatorBridgeMapper` 仍在 `feature/creatorplay/mapper` 下保留，用于迁移期显式过滤旧内容字段；它不是播放器骨架的一部分。

阶段四静态检查：

- 新播放器骨架文件未命中 `DramasPlayPager / EpisodeUnlock / lockStatus / costNums / liked / likeCount / EpisodeInfo / WorkInfo`。
- 旧播放器核心文件未命中 `CreatorWorkInfo / CreatorEpisodeInfo / CreatorPlayableEpisode / CreatorPlayerActivity`。
- `item_creator_video_feed.xml` 未直接引用 `CreatorPlayableEpisode` 或 `item.episode / item.workContext`。
- 播放器 layout 引用的 `ic_play_default_cover / icon_play_off / loading / icon_back_left` 均存在。
- 未运行 Gradle。

### 2026-06-04 阶段五进度：TopEarners 点击进入新播放器

本阶段已完成 TopEarners 到新播放器的真实入口：

- `TopEarnersAdapter` 点击入口从旧 `DramasPlayPagerActivity` 切到 `CreatorPlayerActivity.start(CreatorPlaySource.TOP_EARNERS, currentPosition)`。
- `CreatorPlayerViewModel.playableFeed()` 按 source 分发播放器 feed；阶段五先接 `TOP_EARNERS`。
- `TOP_EARNERS` 播放 feed 从 `TopEarnersCreatorFeedRepository.getWorkFeed()` 派生 `CreatorPlayableEpisode`，不新建 PagingSource，不重新请求接口。
- `CreatorPlayerActivity` 已 collect `playableFeed()` 并提交给 `CreatorVideoFeedAdapter`。
- `CreatorPlayerActivity` 只在首次进入时使用初始 position，避免后续 Paging 刷新时跳回点击位置。
- `CreatorPlayerViewModel` 已修复 `PagingData.map` nullable 编译问题：`PagingData.map` 的返回泛型要求非空，TopEarners 现在直接调用 `TopEarnersCreatorMapper.toPlayableEpisode(...)`。
- `TopEarnersCreatorMapper.toPlayableEpisode(...)` 在 work 缺少 episode 时会生成空视频兜底 episode，保留原列表 position，让播放器展示错误态而不是过滤 item 导致点击位置错位。
- `item_creator_video_feed.xml` 已移除封面 ImageView，新播放器加载时不再先显示封面再切首帧，避免封面二次加载和尺寸跳动。
- `item_creator_video_feed.xml` 已按 FastFilm 播放器信息层级补回作品/剧集简介，XML 仍只绑定 `ItemCreatorVideoFeedVm`，不直接访问实体字段。
- `CreatorVideoFeedAdapter` 的 loading 动画改为可见且 View 已有尺寸后启动；如果下一帧仍未测量完成，会等到 pre-draw 再启动，避免旋转圆心落在左上角。
- 已核对 FastFilm 和本项目旧播放器右上角逻辑：两者都是本人作品显示编辑按钮、更多菜单内显示下载，非本人作品更多菜单内显示举报；但本项目旧播放器下载链路不含 FastFilm 的下载方式弹窗，不能直接照旧播放器搬。
- 编辑入口先弹 `redraw_cover_confirm` 确认框，再通过 `CreatorStoryboardEditorLauncher` 进入现有分镜视频编辑页；launcher 负责 Creator episode 到旧编辑页参数的隔离桥接。
- 下载入口已按 FastFilm 补齐：当前视频 `hasWatermark == true` 时直接下载无水印源；否则先弹 `DownloadMethodDialog` 选择带水印/无水印，带水印版本通过 `VideoWithWatermarkResource` 叠加 AI 水印后保存，下载进度继续展示 `DownloadingDialog`。

阶段五逻辑检查：

- `feature/create` 下未命中 `HomeWorkDetailActivity / DramasPlayPagerActivity / setPreloadedSeries`。
- `TopEarnersAdapter` 已命中 `CreatorPlayerActivity` 和 `CreatorPlaySource.TOP_EARNERS`。
- 新播放器仍未包含选集、点赞收藏、解锁弹窗、3 秒隐藏等旧播放器逻辑。
- `item_creator_video_feed.xml` 仍只绑定 `ItemCreatorVideoFeedVm`。
- `CreatorPlayerViewModel` 和 `TopEarnersCreatorMapper` 未命中 nullable `PagingData.map`、`androidx.paging.filter`、`item!!` 残留。
- `item_creator_video_feed.xml` 未直接引用 `CreatorPlayableEpisode / item.episode / item.workContext`，且新播放器 item 文件未保留 cover 绑定。
- `feature/creatorplay` 没有直接 import 旧 `EpisodeInfo / WorkInfo`；旧 `EpisodeInfo` 桥接只放在 `feature/editor/CreatorStoryboardEditorLauncher.kt`，用于调用现有分镜编辑页。
- 未运行 Gradle；需要实际跑页面验证 TopEarners 点击、上下滑、完播下一条和分页追加。

### 2026-06-04 阶段七进度：MyCreations / 作品详情播放入口切新播放器

本次先推进播放入口切换，未直接改 Room 表结构：

- 新增 `WorkSeriesCreatorFeedRepository`，按 `CreatorPlaySource + workId` 保存当前作品内的 `PagingData<CreatorPlayableEpisode>`。
- `CreatorPlayerActivity.start(...)` 增加 `sourceKey` 参数，用于 `MY_CREATIONS` 和 `WORK_SERIES` 读取当前作品内 feed。
- `CreatorPlayerViewModel.playableFeed()` 已支持：
  - `MY_CREATIONS`：读取当前作品自己的可播放剧集。
  - `WORK_SERIES`：读取作品详情当前作品的可播放剧集。
- `MyWorksFragment` 和 `MyCreationsFragment` 的单集完成作品播放入口已从旧 `FeedRepository + DramasPlayPagerActivity` 切到 `CreatorPlayerActivity(source = MY_CREATIONS, sourceKey = workId)`。
- `SeriesActivity` 的已完成剧集播放入口已从旧 `FeedRepository + DramasPlayPagerActivity` 切到 `CreatorPlayerActivity(source = WORK_SERIES, sourceKey = workId)`。
- 入口处提交给新播放器的是 `CreatorWorkInfo/CreatorEpisodeInfo`；旧 DTO 没有继续传给 `CreatorPlayerActivity` 或播放器 Adapter。

阶段七静态检查：

- `MyWorksFragment`、`MyCreationsFragment`、`SeriesActivity` 和 `feature/creatorplay` 未命中旧播放器启动调用：`DramasPlayPagerActivity.start`、`setPreloadedSeries`。
- 三个入口均命中 `CreatorPlayerActivity`、`WorkSeriesCreatorFeedRepository` 和对应 source：`MY_CREATIONS` / `WORK_SERIES`。
- 新播放器 source 分发已通过 `sourceKey` 区分当前作品，不跨作品切换。
- 未运行 Gradle。

阶段六剩余说明已在后续“阶段六补充：数据库与作品详情 Creator 数据迁移”中处理。

### 2026-06-04 阶段六补充：数据库与作品详情 Creator 数据迁移

本次接着补阶段六的数据结构和数据库操作：

- `WorkInformationInfo` 已从 `@Embedded WorkInfo` 切为 `@Embedded CreatorWorkInfo`，MyCreations 列表缓存开始承载 Creator 作品模型。
- 新增 `CreatorConverters`，用于 `CreatorWorkInfo.workStoryItemList`、`publishPlatformImageList`、`platformLinks` 等列表字段的 Room 转换，避免和旧 `Converters` 的 `EpisodeInfo` 列表转换混用。
- `AppDatabase` 版本升到 `3`，新增 `MIGRATION_2_3`，给 `WorkInformationInfo` 补充 Creator/FastFilm 扩展列：`platformLinks`、`totalEarnings`、`shelfTime`、`generationFailedFlag`、`generationStatusFlag`、`stepProgressSubTitle`。
- `ApiService.getMyWork`、`getWorkInfo`、`episodeList`、`getWorkStatusList` 已切到 `CreatorWorkInfo` / `CreatorEpisodeInfo` 返回类型。
- `MyWorksAdapter`、`ItemMyWorkVM`、`MyWorksEvent`、`MyWorksViewModel`、`MyWorksFragment` 和 `MyCreationsFragment` 的 MyCreations 主链路已改读 `CreatorWorkInfo` / `CreatorEpisodeInfo`。
- `EpisodePagingSource`、`EpisodeAdapter`、`ItemSeriesEpisodeVM`、`SeriesViewModel`、`SeriesActivity`、`WorksManagementViewModel`、`WorksManagementActivity` 已切到 `CreatorEpisodeInfo`；作品详情 `workInfo` 已切到 `CreatorWorkInfo`。
- `SeriesActivity` 的完成剧集播放入口现在直接把 Creator 剧集列表提交给 `WorkSeriesCreatorFeedRepository`，不再通过旧 `EpisodeInfo` 桥接后进入新播放器。
- `CreatorWorkInfo` 补充 `isMine()`，保持作品详情 XML 原有更多菜单显示逻辑。
- 旧编辑入口仍通过 `LegacyCreatorBridgeMapper.toLegacyWorkInfo(...)` 显式桥接给 `DramaInfoEditActivity`；`latestEpisode` 接口仍返回旧 `EpisodeInfo`，在 `SeriesViewModel` 边界转换为 `CreatorEpisodeInfo`，未让旧 DTO 进入播放器或列表主链路。

本次静态检查：

- `rg -n "import com\.kuaigeglobal\.epnova\.core\.network\.apis\.entity\.(WorkInfo|EpisodeInfo)$|\bWorkInfo\.|\bEpisodeInfo\." app/src/main/java/com/kuaigeglobal/epnova/feature/mine app/src/main/java/com/kuaigeglobal/epnova/feature/works app/src/main/java/com/kuaigeglobal/epnova/feature/series`
  - 目标链路未命中旧 `WorkInfo/EpisodeInfo` 主链路引用。
- `rg -n "DramasPlayPagerActivity|setPreloadedSeries|FeedRepository" app/src/main/java/com/kuaigeglobal/epnova/feature/works app/src/main/java/com/kuaigeglobal/epnova/feature/mine/child/MyCreationsFragment.kt app/src/main/java/com/kuaigeglobal/epnova/feature/series`
  - `MyWorksFragment`、`MyCreationsFragment`、`SeriesActivity` 未命中旧播放器启动调用；命中的 `WorkSeriesCreatorFeedRepository` 是新播放器当前作品内 feed。
- `item_my_episode_info.xml`、`activity_series.xml` 已用 PowerShell XML 解析通过。
- `git diff --check` 通过。
- Room `3.json` schema 已生成，`WorkInformationInfo` 中新增 Creator/FastFilm 扩展列和 `MIGRATION_2_3` 对齐；后续如果继续跑编译，重点看 Room 迁移校验和 DataBinding 生成结果。

### 2026-06-04 阶段八：全局隔离复核和收尾

本次按阶段八做全局隔离检查，并补了一个数据库迁移遗漏：

- `AppDatabase` 当前已是 version 3，schema 中已有 `WorkInformationInfo` 扩展列；代码已补回 `.addMigrations(MIGRATION_2_3)`，避免 2 -> 3 升级时因空迁移产生运行期风险。
- `feature/creatorplay` 已清掉旧内容消费字段命中；`LegacyCreatorBridgeMapper` 注释改为只描述“内容消费字段”，避免全局检查被字段名污染。
- `feature/play` 未命中 `CreatorWorkInfo / CreatorEpisodeInfo / CreatorPlayableEpisode / CreatorPlayerActivity`，旧内容播放器没有反向依赖 Creator 模型。
- `feature/create`、`MyCreationsFragment`、`feature/works`、`feature/series` 的目标入口未命中 `HomeWorkDetailActivity / DramasPlayPagerActivity / setPreloadedSeries`。
- `MineChildFragment` 仍命中 `DramasPlayPagerActivity`，但该页面使用 `WorkVOInfo`，属于旧 My List/收藏历史内容入口，不是 MyCreations 创作入口；按“旧内容体系不强切 Creator”保留。
- `CreatorPlaySource` 仅包含 `TOP_EARNERS / MY_CREATIONS / WORK_SERIES`，未出现 `HOME_CONTENT`。
- `item_my_episode_info.xml`、`activity_series.xml` 解析通过。
- `git diff --check` 通过。

阶段八剩余人工验证：

- TopEarners 推荐作品：可跨作品上下切。
- MyCreations 单集作品：只播放自己。
- MyCreations 多集作品：只在当前作品剧集内切。
- 作品详情剧集列表：只在当前作品剧集内切。
- 首页内容区和 My List/收藏历史旧内容入口不受 Creator 播放体系影响。
- 发布审核角标、发行按钮、驳回原因、删除限制仍可用。
