# 作品发布功能逻辑说明

整理时间：2026-05-21  
确认补充：2026-06-01
来源：发布功能逻辑截图与作品详情页状态截图  
关联流程文档：[作品发布流程说明](work-publish-flow.md)

本文用于补充作品发布流程中的状态体系、发布入口判断、可发布集数计算、边界条件和弹窗/toast 文案。发布 H5 页面本身不由 Android 实现；Android 点击 `Publish` 后统一查询 `/work/publish/status`，由接口返回的 `publishEntryType/firstPublishFlag` 决定进入 H5 或原生后续发布。

## 1. 状态体系

### 1.1 剧集状态定义

| 状态 | 中文含义 | 说明 |
| --- | --- | --- |
| `Draft` | 草稿 | 未合成的剧集 |
| `Unpublished` | 待发布 | 已合成，未提交发布 |
| `In Review` | 审核中 | 已提交，审核进行中 |
| `Live` | 已发布 | 审核通过，已上线 |

注意：剧集层面不展示 `Review Failed` 角标。审核失败时，只在作品层面展示 `Review Failed`；对应失败剧集状态回退为 `Unpublished`，允许用户重新提交。

2026-06-01 确认：剧集接口不应返回 `coverBadgeType=40 Review Failed`。如果接口仍返回，视为接口问题，需要接口侧修正；Android 不按剧集层展示 `Review Failed`。

### 1.2 作品状态定义

作品状态包括：

- `Review Failed`：审核失败。
- `Draft`：草稿。
- `Unpublished`：待发布。
- `In Review`：审核中。
- `Published`：已发布。

作品状态有两类计算方式。

#### 1.2.1 审核失败状态

当任意提交审核中的剧集被审核失败时：

1. 后台直接将作品状态置为 `Review Failed`，该状态不参与普通优先级计算。
2. 对应失败剧集状态变更为 `Unpublished`，允许重新提交。
3. 用户可点击作品层面的 `Review Failed` 角标，查看后台返回的拒审理由。
4. 当有任意一集状态发生变更时，`Review Failed` 自动消失，作品状态重新按普通优先级计算。

#### 1.2.2 普通状态优先级

非审核失败状态下，作品状态基于该作品所有剧集状态计算，取优先级最高的状态展示。

优先级从高到低：

```text
Unpublished > Draft > In Review > Live
```

| 优先级 | 剧集状态条件 | 作品状态展示 |
| --- | --- | --- |
| 1（最高） | 含 `Unpublished` 集 | `Unpublished` |
| 2 | 含 `Draft` 集 | `Draft` |
| 3 | 含 `In Review` 集 | `In Review` |
| 4（最低） | 全部为 `Live` | `Published` |

所有剧集均为 `Live` 时，作品状态展示 `Published`。任意一集状态变更时，作品状态需要实时重新计算。

### 1.3 作品状态展示样式

| 场景 | 展示文案示例 |
| --- | --- |
| 审核失败 | `Review Failed` |
| 待发布 | `Unpublished` |
| 草稿 | `Draft` |
| 审核中 | `In Review` |
| 部分集已发布 | `Published · 15 Eps` |
| 全部集已发布 | `Published · All Eps` |

当作品为 `Review Failed` 时，角标可点击，点击后展示被退回理由弹窗。

## 2. 发布入口与路径

点击作品详情页底部 `Publish` 后，先请求 `/work/publish/status`，根据接口返回的 `publishEntryType/firstPublishFlag` 决定原生提交、打开 H5 或展示业务提示。

```mermaid
flowchart TD
    A[点击 Publish] --> B[查询 /work/publish/status]
    B --> C{publishEntryType}
    C -- 0 不可发布 --> D[展示 reasonMsg]
    C -- 1 H5首次发行页 --> F[跳转 H5 发布页]
    F --> G[协议确认/资料填写/提交]
    C -- 2 原生发行页 --> I[正常发布二次确认弹窗]
    I --> J[提交 /work/publish/submit]
```

路径规则：

- Android 点击 `Publish` 后先查 `/work/publish/status`，不依赖本地 Paging 已加载剧集自行计算完整可发布范围，也不再用本地 `publishedEpisodeCnt` 判断是否首次。
- `publishEntryType=0`：直接展示后端 `reasonMsg`，例如无可发布剧集、不可跳集发布等业务原因；`publishEntryType` 缺失时再用 `canApplyFlag=0` 兜底。
- `publishEntryType=1` 或 `firstPublishFlag=1`：跳转 H5 发布页，后续协议确认、资料填写与提交全部由 H5 完成。
- `publishEntryType=2`：Android 原生展示后续发布二次确认弹窗，并提交 `/work/publish/submit`。
- `needLegalInfoFlag=1` 作为旧字段兼容兜底，也跳转 H5 发布页。
- 后端会拼好拒审原因，Android 作品/剧集角标查看拒审原因统一取 `rejectReason`。

### 2.1 发布按钮展示规则

作品详情页底部 `Publish` 按钮按以下规则处理：

| 条件 | 前端表现 |
| --- | --- |
| `showPublishButton == 0` | 隐藏按钮 |
| `showPublishButton == 1` 且未全部发行 | 展示并可点击 |
| `showPublishButton == 1` 且全部剧集已完结并全部发行 | 展示但置灰，不再提交 |

全部发行判断建议使用：

```text
totalEpisodeCnt > 0 && publishedEpisodeCnt >= totalEpisodeCnt
```

如果 `totalEpisodeCnt` 为空或小于等于 0，不应误判为全部发行。

## 3. 发布条件与集数计算

### 3.1 首次发布可发布集数规则

首次发布必须满足：

- 前 15 集全部已合成。

满足后，可发布集数不是固定 15 集，而是从第 1 集开始连续已合成的全部集数。

示例：

- 第 1-15 集已合成，第 16 集未合成：首次提交范围为 `1-15`。
- 第 1-16 集已合成，第 17 集未合成：首次提交范围为 `1-16`，确认弹窗标题展示 `Publish Episodes 1-16?`。

Android 不在原生侧实现首次发布内的前 15 集校验、协议、法定姓名、邮箱和首次提交。是否首次发行由 `/work/publish/status` 返回的 `publishEntryType/firstPublishFlag` 判断；首次发布失败后重新进入仍由接口返回 H5 入口。

### 3.2 续集发布可发布集数规则

续集发布时，业务规则仍然是从已发布/审核中剧集之后继续计算可提交的连续剧集范围；实际 Android 实现以 `/work/publish/status` 返回的 `publishEntryType/startEpisode/endEpisode/canApplyFlag/reasonMsg` 为准。

规则：

1. 只从已合成且未发布的剧集中取可发布范围。
2. 取与已发布/审核中集数连续的部分。
3. 从已发布/审核中的最大集数 + 1 开始计算，连续已合成则可发布，遇到未合成或断集时截止。
4. 审核中剧集视为连续性占位，计算连续性时纳入；但对外展示的已发布集数仍以 `Live` 剧集为准。
5. 曾被驳回的剧集状态回退为 `Unpublished`。如果它处于本次连续可发布范围内，需要计入本次发布，并在提交前触发驳回提醒二次确认弹窗。

示例：

```text
已发布第 1-15 集
第 16 集审核中，占位
第 17、18 集已合成
第 19 集未合成
```

此时可提交第 17、18 集。若第 17 集曾经被驳回过，则触发含驳回提示的二次确认弹窗。

2026-06-01 确认：后续发布提交接口提交连续剧集范围，不提交剧集 ID 列表。Android 提交 `workId/auditBatchId/startEpisode/endEpisode`。

## 4. 边界条件

### 4.1 审核中剧集审核失败

当审核中剧集审核失败：

1. 作品状态更新为 `Review Failed`。
2. 被退回的剧集角标变更为 `Unpublished`，允许重新提交。
3. 作品层面的 `Review Failed` 角标可点击。
4. 点击后展示后台返回的拒审理由弹窗。

拒审理由弹窗按剧集或连续剧集范围展示，例如：

```text
EP.1
Episode publication failed. Please verify your content and retry.

EP.12
Episode publication failed. Please verify your content and retry.

EP.19
Episode publication failed. Please verify your content and retry.

EP.20 - EP.25
Episode publication failed. Please verify your content and retry.
```

### 4.2 重新提交审核失败剧集

重新提交审核失败剧集后：

1. 作品状态重新按普通优先级计算。
2. `Review Failed` 角标消失。
3. 被重新提交的剧集进入 `In Review` 或接口返回的目标状态。

### 4.3 所有剧集均已发布

当所有剧集均为 `Live`：

1. 作品角标展示 `Published · All Eps`。
2. 若 `showPublishButton=1`，底部 `Publish` 按钮展示但置灰，不允许再次提交；若 `showPublishButton=0`，按钮隐藏。
3. 若仍点击或触发发布入口，应按无可发布内容处理。

### 4.4 续集无可发布剧集

续集发布时，如果没有未发布且已合成的剧集，点击 `Publish` 后展示 toast：

```text
No episodes available to publish!
```

实际 Android 实现中，该提示优先使用 `/work/publish/status` 返回的 `reasonMsg`，不要本地硬编码覆盖后端文案。

## 5. 弹窗与 Toast 文案

### 5.1 首次发布

#### 5.1.1 未完成前 15 集提示弹窗

触发条件：首次发布，前 15 集未全部合成，点击 `Publish`。

形式：弹窗，单按钮。

正文：

```text
Please complete episodes 1-15 before publishing.
```

操作：

```text
OK -> 关闭弹窗
```

#### 5.1.2 发布协议确认弹窗

触发条件：首次发布，前 15 集已全部合成，且用户未勾选协议选择框。

形式：弹窗。

正文：

```text
Publishing this drama means you agree to the Distribution Service Agreement. Do you want to continue?
```

操作：

```text
Cancel / Publish
点击 Publish -> 进入集数二次确认弹窗
```

#### 5.1.3 首次发布集数确认弹窗

触发条件：首次发布，前 15 集已全部合成，且用户已完成协议确认。

形式：弹窗。

标题：

```text
Publish Episodes 1-16?
```

标题中的集数范围按本次实际提交集数动态展示，不固定为 `1-16`。

正文：

```text
These episodes will be submitted for review and made available to viewers once approved. This action cannot be undone.
```

操作：

```text
Cancel / Publish
点击 Publish -> 提交审核 -> 触发发布成功 toast
```

### 5.2 续集发布

#### 5.2.1 无可发布集 Toast

触发条件：续集发布，`/work/publish/status` 返回 `canApplyFlag=0` 且 `reasonMsg` 为无可发布剧集提示。

形式：toast。

文案：

```text
No episodes available to publish!
```

#### 5.2.2 不可跳集发布 Toast

触发条件：续集发布，`/work/publish/status` 返回 `canApplyFlag=0` 且 `reasonMsg` 为不可跳集发布提示。

形式：toast。

文案：

```text
Episodes must be published in order. You cannot skip episodes.
```

#### 5.2.3 含驳回剧集二次确认弹窗

触发条件：续集发布，满足连续发布条件，但本次提交范围内包含曾被驳回的剧集。

2026-06-01 确认：接口侧会把拒审原因拼好放在 `rejectReason`。后续发布确认是否单独展示驳回提醒，如果接口不再返回“本次范围内包含曾被驳回剧集”的专用字段，则 Android 先按普通二次确认处理；作品层 `Review Failed` 原因展示仍取 `rejectReason`。

形式：弹窗。

标题示例：

```text
Publish Episodes 17-20?
```

正文：

```text
This submission contains rejected episodes. Please ensure they are revised based on the feedback before resubmitting.
```

操作：

```text
Cancel / Publish
点击 Publish -> 提交审核 -> 触发发布成功 toast
```

#### 5.2.4 正常续集发布确认弹窗

触发条件：续集发布，满足连续发布条件，且本次提交范围内没有曾被驳回的剧集。

形式：弹窗。

标题示例：

```text
Publish Episodes 17-20?
```

正文：

```text
These episodes will be submitted for review and made available to viewers once approved. This action cannot be undone.
```

操作：

```text
Cancel / Publish
点击 Publish -> 提交审核 -> 触发发布成功 toast
```

### 5.3 发布成功 Toast

触发条件：首次发布或续集发布，用户在确认弹窗中点击 `Publish` 且提交成功。

形式：深色背景 toast。

标题：

```text
You're all set!
```

正文：

```text
Your application has been submitted.
```

说明：所有弹窗标题中的集数范围均为动态填充，需要根据本次实际提交集数展示。

## 6. 已确认接口字段

### 6.1 查询作品发行状态

```text
POST /work/publish/status
```

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `workId` | long | 是 | 作品 ID |

返回 `data`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `workId` | long | 作品 ID |
| `auditBatchId` | long | 正在审核中的批次号，若存在 |
| `needLegalInfoFlag` | int | 是否需要提交法定姓名/邮箱，`0=否`，`1=是` |
| `startEpisode` | int | 当前可提交剧集区间起始集数，包含 |
| `endEpisode` | int | 当前可提交剧集区间结束集数，包含 |
| `canApplyFlag` | int | 是否可以继续提交申请，`0=否`，`1=是` |
| `reasonMsg` | string | 业务提示文案，`canApplyFlag=0` 时直接展示 |
| `publishEntryType` | int | 发行入口类型：`0=不可发布`，`1=H5 首次发行页`，`2=原生发行页` |
| `firstPublishFlag` | int | 是否首次发行：`0=否`，`1=是` |

### 6.2 提交作品剧集集中审核

```text
POST /work/publish/submit
```

请求体：

| 字段 | 类型 | 必填 | Android 是否传 | 说明 |
| --- | --- | --- | --- | --- |
| `workId` | long | 是 | 是 | 作品 ID |
| `auditBatchId` | long | 否 | 是，有值则传 | 为空表示新建批次，不为空表示提交到指定批次 |
| `startEpisode` | int | 是 | 是 | 提交审核起始集数，包含 |
| `endEpisode` | int | 是 | 是 | 提交审核结束集数，包含 |
| `legalName` | string | 否 | 否 | 仅第一次提交时 H5 填写 |
| `emailAddress` | string | 否 | 否 | 仅第一次提交时 H5 填写 |

返回体沿用项目 `ApiResponse`：`code/msg/data`。

## 7. 与发布流程文档的关系

[作品发布流程说明](work-publish-flow.md) 描述完整业务路径和 Android/H5 职责边界；本文补充状态计算、可发布集数计算和文案细节。

实现时建议先按本文计算出：

1. 当前作品状态。
2. `showPublishButton` 和是否全部发行置灰。
3. `/work/publish/status` 返回的是否可继续申请。
4. `publishEntryType/firstPublishFlag` 决定的 H5 或原生入口。
5. 本次可提交剧集范围和审核批次。

再按 [作品发布流程说明](work-publish-flow.md) 选择 H5 或原生发布路径。
