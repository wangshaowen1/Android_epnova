# 作品发布相关代码现状说明

整理时间：2026-05-21  
关联流程文档：[作品发布流程说明](work-publish-flow.md)  
关联逻辑文档：[作品发布功能逻辑说明](work-publish-logic.md)

本文记录当前项目内与“作品发布”相关的代码现状。需要特别注意：当前代码里的“发行/去发行/签约审核”不是本次设计稿中的“发布审核”流程，而是从其他项目复制过来的残留逻辑。部分字段可以借鉴，但页面逻辑需要按新发布流程重做。

## 1. 当前涉及范围

本次发布逻辑主要涉及：

| 页面/模块 | 当前类/布局 | 说明 |
| --- | --- | --- |
| 剧集详情页 | `feature/series/SeriesActivity.kt`、`SeriesViewModel.kt`、`activity_series.xml` | 新发布入口应主要落在这里。当前底部按钮仍是旧“发行/签约”逻辑 |
| 剧集列表 item | `ItemSeriesEpisodeVM.kt`、`item_my_episode_info.xml` | 当前只展示创作进度状态，没有按新发布状态展示角标 |
| 作品列表 | `feature/mine/child/MyCreationsFragment.kt` | 当前实际使用的“我的创作”入口 |
| 作品列表 item | `feature/works/MyWorksAdapter.kt`、`ItemMyWorkVM.kt`、`item_my_work.xml` | 当前被 `MyCreationsFragment` 复用，只展示创作状态，没有展示新发布状态 |
| 旧作品页 | `feature/works/MyWorksFragment.kt` | 当前业务已不用，但其 Adapter/ViewModel 仍被 `MyCreationsFragment` 复用 |
| 状态模型 | `core/network/apis/entity/WorkInfo.kt` | 当前 `WorkInfo` 和 `EpisodeInfo` 内已有部分发布字段，但语义需要统一更新 |

## 2. 当前剧集详情页逻辑

### 2.1 数据加载

`SeriesActivity` 启动时只传 `workId`：

```text
SeriesActivity.start(workId)
```

`SeriesViewModel.onResume()` 每次进入页面都会调用：

```text
getWorkDtl()
```

数据来源：

- 作品详情：`ApiService.getWorkInfo`，接口 path 为 `work/detail`。
- 剧集分页：`ApiService.episodeList`，接口 path 为 `work/storyPage`。

`workInfo` 更新后，页面会刷新剧集分页：

```text
mRealVM.workInfo.observe -> episodeAdapter.refresh()
```

### 2.2 当前顶部状态展示

`activity_series.xml` 中存在旧发行状态区域：

- 作品评级图标：`contractLevel -> workLevelIcon`
- 审核失败图标：`contractStatus == 3`
- 状态文案：`publishStatusText(workInfo)`
- 状态颜色：`publishStatusColor(workInfo)`
- 平台图标列表：`publishPlatformImageList`

但当前状态文案 `tvPublishStatus` 在 XML 中固定 `android:visibility="gone"`，所以旧发行状态文本实际不可见。失败图标仍可能显示，点击 `llPublishStatus` 会展示 `rejectReason` 弹窗。

当前 `SeriesViewModel.publishStatusText()` 的状态是旧“签约 + 发行”组合：

| 字段 | 值 | 当前文案 |
| --- | ---: | --- |
| `contractStatus` | 0 | `未发行` |
| `contractStatus` | 1 | `签约审核中` |
| `contractStatus` | 2 且 `publishStatus=0` | `未发行` |
| `contractStatus` | 2 且 `publishStatus=1` | `待签约` |
| `contractStatus` | 2 且 `publishStatus=2` | `发行上架中` |
| `contractStatus` | 2 且 `publishStatus=3` | `已发行` |
| `contractStatus` | 3 | `签约失败` |

这些文案和本次设计中的 `Draft / Unpublished / In Review / Review Failed / Published` 不一致，不能直接复用。

### 2.3 当前底部按钮逻辑

`activity_series.xml` 底部按钮容器显示条件：

```text
showPublishButton == 1 && contractStatus != 2
```

按钮可点击条件：

```text
contractStatus != 1
```

按钮文案来自 `SeriesViewModel.publishButtonText()`：

| `contractStatus` | 当前按钮文案 |
| ---: | --- |
| 0 | `去发行` |
| 1 | `签约审核中` |
| 3 | `申请签约` |
| 其他 | 空 |

点击 `btnPublish` 后：

```text
workContractPublishPageUrl -> SimpleBrowserActivity.launch()
```

这说明当前按钮只是打开旧发行 H5，不会：

- 判断是否首次发布。
- 判断是否有未发布但已完成的剧集。
- 判断是否允许连续发布。
- 判断本次提交范围是否包含被驳回剧集。
- 调用后续发布接口。

因此它不是本次新发布流程的最终实现，只能作为 H5 容器打开方式的参考。

### 2.4 当前“自己发行”入口

`activity_series.xml` 里有 `btnSelfPublish`：

```text
workSelfPublishProcessUrl -> SimpleBrowserActivity.launch()
```

但该控件 XML 中固定 `android:visibility="gone"`，当前页面默认不展示。它属于旧“发行流程”残留。

## 3. 当前剧集 item 逻辑

剧集详情页列表使用：

- `EpisodeAdapter`
- `ItemSeriesEpisodeVM`
- `item_my_episode_info.xml`

当前 item 展示的主要是创作流程状态：

| 判断 | 当前 UI |
| --- | --- |
| `EpisodeInfo.isNotCreated()` | 占位卡片，显示待创建 |
| `EpisodeInfo.isGenerating()` | 半透明遮罩 + 生成中文案 |
| `EpisodeInfo.isDraft()` | `Draft` 角标 |
| `EpisodeInfo.isWaitingComposing()` | `Pending creation / waiting_to_synthesize` 角标 |
| `EpisodeInfo.isCompleted()` | 显示视频时长 |

当前没有根据 `coverBadgeType` 展示新发布状态角标：

- `Unpublished`
- `In Review`
- `Published`
- `Review Failed`

按新设计，剧集层面不应展示 `Review Failed`，审核失败剧集应回退为 `Unpublished`，失败原因集中在作品层面的 `Review Failed` 弹窗中展示。

### 3.1 当前剧集点击行为

`SeriesActivity.clickToView()` 当前逻辑：

| `stepProgress` | 行为 |
| --- | --- |
| `PROGRESS_COMPLETED` | 过滤有 `videoUrl` 的剧集并跳播放页 |
| `NOT_CREATED` 或 `PROGRESS_SUMMARY_GENERATING` | 校验前序剧集是否完成，允许后进入续写/创建 |
| 其他 | `AppRouter.startStory()` 进入对应创作流程 |

当前点击逻辑不区分发布状态。已发布或审核中的剧集仍可播放或进入查看，但编辑动作会在部分编辑页面被锁定。

### 3.2 当前删除/编辑锁定

`EpisodeInfo.isPublishedOrUnderReview()` 当前判断：

```text
publishStatus in [1, 2]
或 coverBadgeType in [30, 50]
```

含义大致是：

- `publishStatus=1`：审核中。
- `publishStatus=2`：已通过。
- `coverBadgeType=30`：In Review。
- `coverBadgeType=50`：Published。

使用点：

- `ItemSeriesEpisodeVM.isDeleteLocked()`：审核中/已发布剧集不可删除。
- `WorksManagementActivity.currentSelectableIds()`：批量管理时过滤审核中/已发布剧集。
- `EditStoryboardVideoViewModel.updatePublishLocked()`：审核中/已发布剧集禁止部分重生成、字幕、替换等编辑操作。

这个锁定方向与新发布流程基本一致，但需要以后端新状态枚举为准重新收口，尤其要确认 `Review Failed` / `Unpublished` 是否允许编辑和重新提交。

## 4. 当前作品列表逻辑

### 4.1 当前实际入口

当前“我的创作”入口是：

```text
MyListFragment -> MyCreationsFragment
```

`MineFragment` 中的 `MyCreationsFragment` 已被注释掉；`feature/works/MyWorksFragment` 当前业务不作为主入口使用。

### 4.2 列表数据来源

`MyCreationsFragment` 复用 `MyWorksViewModel`：

- 分页来源：`ApiService.getMyWork`
- 本地缓存表：`WorkInformationInfo`
- 生成中轮询：`ApiService.getWorkStatusList`

轮询逻辑只用于创作生成状态：

1. `startChecking()` 每 3 秒调用 `getWorkStatusList`。
2. 如果有生成中的作品，更新 Room 缓存。
3. 如果生成中列表变化，触发刷新。

当前轮询不处理发布审核状态变化。新发布流程提交审核后，作品列表状态刷新需要额外考虑接口刷新或事件通知。

### 4.3 作品 item 当前展示

`item_my_work.xml` 当前展示：

| 判断 | 当前 UI |
| --- | --- |
| `workInfo.isWaitingComposing()` | 半透明遮罩 + 生成中文案 |
| `workInfo.isDraftStatus()` | `Draft` 角标 |
| `workStatus == WAIT_STATE` | `waiting_to_synthesize` 角标 |
| `finishedEpisodeCnt > 0` | 显示集数进度 |

当前没有展示 `coverBadgeType` 对应的新发布状态角标：

- `Unpublished`
- `In Review`
- `Review Failed`
- `Published · N Eps`
- `Published · All Eps`

因此作品列表需要按新设计补充封面角标/状态展示。

### 4.4 作品点击跳转

`MyCreationsFragment` 点击作品后：

| 条件 | 行为 |
| --- | --- |
| `workInfo.isNotCreatedEpisode()` | 请求详情后，小说上传草稿进入恢复创作或剧集详情 |
| `totalEpisodeCnt == 1 && workStatus == COMPLETED_STATE` | 拉取剧集列表后直接播放 |
| `totalEpisodeCnt == 1 && 未完成` | 进入单集详情/创作流程 |
| 多集作品 | 进入 `SeriesActivity` |

当前点击跳转不受发布状态影响。

## 5. 当前 `WorkInfo` / `EpisodeInfo` 状态字段

### 5.1 创作状态字段，当前仍在使用

`WorkInfo.workStatus` 当前表示作品创作进度：

| 常量 | 值 | 当前含义 |
| --- | ---: | --- |
| `NOT_CREATED_EPISODE` | 0 | 未创建剧集 |
| `DRAFT` | 5 | 草稿 |
| `MIRROR_GENERATION_IN_PROGRESS` | 32 | 分镜生成中 |
| `WAIT_STATE` | 35 | 待合成 |
| `VIDEO_REDRAWING_IN_PROGRESS` | 45 | 视频重绘中 |
| `VIDEO_REDRAW_COMPLETED` | 50 | 视频重绘已完成 |
| `COMPLETED_STATE` | 60 | 已完成 |
| `FAILED_STATE` | 70 | 失败 |

`EpisodeInfo.stepProgress` 当前表示剧集创作进度：

| 常量 | 值 | 当前含义 |
| --- | ---: | --- |
| `NOT_CREATED` | 0 | 待创建 |
| `PROGRESS_SUMMARY_GENERATING` | 10 | 剧本梗概生成 |
| `PROGRESS_STORY_GENERATING` | 15 | 剧本生成中 |
| `PROGRESS_ASSET_GENERATING` | 20 | 资产生成中 |
| `PROGRESS_STORYBOARD_GENERATING_ASSET` | 30 | 分镜描述生成中 |
| `PROGRESS_STORYBOARD_GENERATING` | 32 | 分镜视频生成中 |
| `PROGRESS_WAITING_COMPOSING` | 35 | 待合成 |
| `PROGRESS_COMPLETED` | 40 | 已完成 |
| `EPISODE_REPAINT_STATE` | 45 | 分镜重绘中 |
| `EPISODE_REPAINT_COMPLETED` | 50 | 重绘已完成 |

这些字段是“创作流程状态”，不是新发布状态，但发布条件里的“已完成/已合成”可以继续参考这些字段。

### 5.2 旧发行/签约字段，当前仍在使用

| 字段 | 当前用途 | 问题 |
| --- | --- | --- |
| `contractStatus` | 控制旧“去发行/签约审核中/申请签约”按钮与失败图标 | 不属于新发布状态体系 |
| `contractLevel` | 展示 S/A/B/C 评级图标 | 新设计未体现 |
| `publishPlatformImageList` | 展示发行平台图标 | 新设计未体现 |
| `workSelfPublishProcessUrl` | 自己发行 H5 URL | 当前隐藏，旧流程残留 |
| `workContractPublishPageUrl` | 旧发行 H5 URL | 可作为首次发布 H5 URL 形式参考，但语义需要重命名/确认 |
| `showPublishButton` | 控制旧发布按钮是否显示 | 需要改为新发布按钮显示规则 |

### 5.3 新发布可借鉴字段

`WorkInfo.kt` 中已有一组 20260520 添加的字段，和新发布流程较接近：

| 字段 | 当前注释 | 可借鉴点 |
| --- | --- | --- |
| `publishStatus` | `1=审核中 2=通过 3=审核失败` | 可用于审核中/已发布/审核失败判断，但需要确认是否作品和剧集共用 |
| `rejectReason` | 驳回原因 | 可用于作品层面 `Review Failed` 理由弹窗 |
| `globalPublishStatus` | `0=未发布 1=审核中 2=已发布 3=审核失败` | 可用于作品整体发布状态，但要和新优先级规则统一 |
| `publishedEpisodeCnt` | 已发布剧集数量 | 可用于 `Published · N Eps` |
| `coverBadgeType` | `0=None,10=Draft,20=Unpublished,30=In Review,40=Review Failed,50=Published` | 可直接映射 UI 角标，但剧集层面应避免展示 `Review Failed` |

当前 `WorkInfo.isPublishedOrUnderReview()` 已经用到：

```text
publishStatus in [1, 2]
或 globalPublishStatus in [1, 2]
或 coverBadgeType in [30, 50]
```

当前 `EpisodeInfo.isPublishedOrUnderReview()` 已经用到：

```text
publishStatus in [1, 2]
或 coverBadgeType in [30, 50]
```

这两个方法可以作为“已发布/审核中不可删除、不可编辑”的基础，但需要把字段语义改成新发布状态后再统一命名。

## 6. 当前接口情况

当前与页面相关的接口：

| 接口 | 当前用途 |
| --- | --- |
| `work/detail` | `SeriesActivity` 获取作品详情 |
| `work/storyPage` | `SeriesActivity` 获取剧集分页 |
| `user/getMyWork` | `MyCreationsFragment` 获取作品列表 |
| `work/getWorkStatusList` | 作品列表轮询生成中作品 |

当前存在旧 `seriesPublish`：

```text
ApiConstants.SERIES_PUBLISH
SeriesRepository.seriesPublish(SeriesSetPriceBody)
```

但它的请求体是：

```text
dramasSeriesId
unlockPrice
startChargeWorkNo
freeStatus
```

这明显是旧短剧上架/价格体系接口，不适合本次“提交剧集审核发布”的新逻辑。当前 `SeriesActivity` 也没有调用它。

后续需要以后端确认的新发布接口为准，替换或新增 Repository 方法。

## 7. 与新发布流程的差距

### 7.1 `SeriesActivity` 需要重做发布入口

当前：

```text
点击 Publish -> 直接打开 workContractPublishPageUrl
```

新流程应为：

```text
点击 Publish
-> Android 判断是否首次发布
-> 首次发布：打开 H5
-> 非首次发布：原生判断可发布集、连续性、是否含驳回集
-> 弹窗确认
-> 调用后续发布接口
-> 成功后刷新作品详情和剧集列表
```

### 7.2 `SeriesActivity` 状态展示需要替换

当前展示基于：

```text
contractStatus + publishStatus
```

新状态展示应基于：

```text
Draft / Unpublished / In Review / Review Failed / Published
```

建议以后端最终枚举为准，把 `publishStatusText()`、`publishStatusColor()`、`publishFailed()` 替换成新发布状态映射。

### 7.3 `MyCreationsFragment` 作品列表需要补状态角标

当前作品列表只展示创作状态，未展示发布状态。新设计需要：

- 作品层面 `Review Failed` 角标。
- `Unpublished` / `Draft` / `In Review` / `Published` 角标。
- `Published · N Eps` 和 `Published · All Eps` 文案。

### 7.4 `EpisodeInfo` item 需要补发布角标

当前剧集 item 只展示创作状态。新设计需要按剧集状态展示：

- `Draft`
- `Unpublished`
- `In Review`
- `Live` / `Published`

同时要遵守设计规则：剧集层面不展示 `Review Failed`，审核失败剧集回退为 `Unpublished`。

### 7.5 删除和编辑锁定逻辑需要按新状态统一

当前删除/编辑锁定已经存在，但基于旧字段：

- `publishStatus`
- `globalPublishStatus`
- `coverBadgeType`

后续需要统一为：

- `In Review`：不可删除，不可编辑。
- `Published/Live`：不可删除，不可编辑或按业务限制编辑。
- `Review Failed`：作品显示失败；失败剧集回退 `Unpublished`，允许修改后重新提交。
- `Unpublished`：允许重新提交，是否允许删除/编辑按产品规则确认。

## 8. 建议改造顺序

1. 先和后端确认 `WorkInfo`、`EpisodeInfo` 的新状态枚举和字段命名。
2. 在 `WorkInfo.kt` 内收口发布状态方法，避免 UI 到处直接判断原始 int。
3. 更新 `SeriesActivity` 顶部状态、底部 `Publish` 按钮显示和点击逻辑。
4. 更新 `item_my_episode_info.xml` 和 `ItemSeriesEpisodeVM`，补剧集发布角标。
5. 更新 `item_my_work.xml` 和 `ItemMyWorkVM`，补作品列表发布角标。
6. 新增或替换后续发布接口调用，旧 `seriesPublish(SeriesSetPriceBody)` 不建议继续用于新发布。
7. 提交成功后通过事件或刷新机制同步 `SeriesActivity` 与 `MyCreationsFragment`。
8. 最后清理旧“发行/签约”残留字段和文案，避免与新“发布”混用。

## 9. 待后端确认

以下内容仍依赖后端最终接口：

- 后续发布接口是提交连续剧集范围，还是提交剧集 ID 列表。
- `WorkInfo` / `EpisodeInfo` 中“完成、待发布、审核中、已发布、审核失败/拒审”的状态枚举。
- H5 首次发布提交成功后，是自动关闭页面，还是只通知 Android 并由用户手动返回。
- 首次发布 H5 URL 是否继续复用 `workContractPublishPageUrl`，还是新增更准确字段。
- `coverBadgeType` 是否由后端直接按新优先级算好；如果不是，Android 是否需要本地按剧集状态计算作品角标。
