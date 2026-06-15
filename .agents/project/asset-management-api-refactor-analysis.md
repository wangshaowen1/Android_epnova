# 资产管理 9 个接口重构对照

生成日期：2026-06-13

## 范围

来源：Eolink MCP 项目 `epnova-APP`，目录 tag 为 `Epnova/v1_2_0/资产管理`。本次只分析接口和本地已有功能对应关系，不修改代码。

本地旧实现主要在：

- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiConstants.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/core/network/apis/ApiService.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/AssetManagementViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/ItemRoleVM.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/ItemSceneVM.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/ItemPropVM.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/edit/ReferenceHistoricalImageryViewModel.kt`
- `app/src/main/java/com/kuaigeglobal/epnova/feature/works/data/RoleImagePagingSource.kt`

## 总体变化

后台这次不是简单改 URL，而是把旧版按角色、场景、道具拆开的接口收束成统一资产模型：

- 旧版：角色、场景、道具分别有 list/save/delete，字段分别是 `roleId/sceneId/propId`、`roleName/sceneName/propName`、`rolePrompt/scenePrompt/propPrompt`。
- 新版：统一为 `assetId`、`assetType`、`assetName`、`assetPrompt`，角色/场景/道具通过 `assetType` 区分。
- 旧版：当前图片 ID 使用 `appearanceId`，参考图用 `referenceImageId/referenceImageUrl`。
- 新版：图片 ID 改为 `assetImageId`，参考图改为 `referenceAssetImageId`，并新增 `usageId` 表示当前集资产使用记录。
- 旧版：当前集角色、场景、道具列表需要并发请求 3 个接口。
- 新版：`/new/work/asset/story/list` 一次返回 `roles/scenes/props` 三组。
- 旧版：选中历史图片复用保存接口传 `appearanceId/imageUrl`，移除图片走三类 `appearance/unset`。
- 新版：图片选择和移除拆成 `/new/work/asset/story/select-image` 与 `/new/work/asset/story/remove-image`。

## 9 个新接口与本地旧接口对应

| 新接口 | 功能 | 本地旧接口/功能 | 主要变化 |
| --- | --- | --- | --- |
| `POST /new/work/asset/description/generate` | 生成当前集资产描述 | `GENERATE_ALL_ASSET_DESCRIPTION = ai/asset/generation/description`，`generateAllAssetDescription(WorkBody)` | 入参仍是 `workId/storyId`；响应仍有 `taskId/genStatus/result`，URL 变化最小。 |
| `POST /new/work/asset/story/list` | 当前集资产列表 | `WORK_ASSET_ROLE_LIST`、`WORK_ASSET_SCENE_LIST`、`WORK_ASSET_PROP_LIST` 三个接口 | 三个列表合并为一个接口，返回 `roles/scenes/props`；字段统一为 `asset*`，新增 `usageId/selectedStatus/actedStatus/sourceStoryId/sourceUsageId/assetImageId`。 |
| `POST /new/work/asset/save` | 新增/编辑资产 | `WORK_ASSET_ROLE_SAVE`、`WORK_ASSET_SCENE_SAVE`、`WORK_ASSET_PROP_SAVE` 三个接口 | 三个保存接口合并；用 `assetType` 区分类型；旧 `appearanceId/imageUrl/parentAppearanceId/source` 不再属于保存资产接口。 |
| `POST /new/work/asset/image/generate` | 生成/重新生成单个资产图 | `WORK_ASSET_SINGLE_GENERATE = ai/asset/generation/single/image` | 旧 `bizType` 移除；旧 `appearanceId` 移除；`referenceImageId` 改为 `referenceAssetImageId`；响应新增 `assetImageId`，不再只返回通用 `TaskStatusInfo`。 |
| `POST /new/work/asset/image/generate/batch` | 一键生成当前集资产图 | `WORK_ASSET_ONE_KEY_GENERATE = ai/asset/oneClickGeneration` | 入参仍是 `workId/storyId`；响应从单个 `TaskStatusInfo` 变为 `submittedCount/skippedCount/items`，可直接知道哪些资产提交成功。 |
| `POST /new/work/asset/history/image/list` | 当前资产历史图片/形象库 | 角色侧对应 `WORK_ASSET_ROLE_IMAGE_PAGE = v2/work/asset/role/appearance/list`；场景/道具在本地当前代码里没有独立历史图片分页入口 | 新接口支持统一资产图片，并返回 `assetImageId/prompt/currentSelected/acted/usedStoryIds/usedStoryNos/createTime` 等完整图片状态。 |
| `POST /new/work/asset/reference/image/list` | 参考生图图片列表 | `GET_HISTORY_REFERENCE_IMAGE = work/asset/getReferenceImage` | 旧 `ReferenceImageInfo` 只有 `appearanceId/imageUrl`；新版返回完整资产图片对象，只展示已经最终出演过的成功图片。 |
| `POST /new/work/asset/story/select-image` | 从历史图片选择/替换当前资产图 | 旧 `AssetManagementViewModel.saveImage(...)` 复用三类 save 接口传 `appearanceId/imageUrl` | 新版明确拆出选择图片接口；入参为 `workId/storyId/assetId/assetImageId`，不再靠 save 接口副作用。 |
| `POST /new/work/asset/story/remove-image` | 移除当前集资产图片 | 旧 `cancelRoleImage/cancelSceneImage/cancelPropImage`，路径分别是 `v2/work/asset/*/appearance/unset` | 新版统一移除接口；入参从旧 `AppearanceIdBody(appearanceId)` 改为 `workId/storyId/assetId/assetImageId?`。 |

## 字段映射

| 旧字段/概念 | 新字段/概念 | 说明 |
| --- | --- | --- |
| `roleId` / `sceneId` / `propId` | `assetId` | 统一作品级资产 ID。 |
| `roleName` / `sceneName` / `propName` | `assetName` | 统一资产名称。 |
| `rolePrompt` / `scenePrompt` / `propPrompt` | `assetPrompt` 或生成接口 `prompt` | 统一资产描述/生图提示词。 |
| `appearanceId` | `assetImageId` | 旧代码注释称“当前资产形象ID/参考图id”，新版语义更明确为资产图片 ID。 |
| `bizType` 41/51/61/42/52/62 | `assetType` + 是否传 `referenceAssetImageId` | 新接口不再要求前端传生图业务类型。 |
| `referenceImageId` | `referenceAssetImageId` | 参考图 ID 字段改名。 |
| `referenceImageUrl` | 不再入参 | 后端根据 `referenceAssetImageId` 查图。 |
| 无 | `usageId` | 当前集资产使用记录 ID，后续如果有当前集维度操作，应优先使用这个概念理解。 |
| 无 | `selectedStatus` / `actedStatus` | 区分当前集是否选图、是否最终出演。 |
| 无 | `sourceStoryId` / `sourceUsageId` | 标识复用图片来源剧集和来源使用记录。 |

## 对现有代码的影响判断

1. `AssetManagementViewModel.getAssets()` 当前并发请求角色、场景、道具三套列表；新版应改为一次请求 `/new/work/asset/story/list`，再把 `roles/scenes/props` 映射到现有 `ItemRoleVM/ItemSceneVM/ItemPropVM` 或新增统一 UI model。

2. `ItemRoleVM/ItemSceneVM/ItemPropVM.generate(...)` 当前先按类型 save，再调用 `workAssetSingleGenerate(...)`，并传 `bizType/appearanceId/referenceImageUrl`；新版生成接口只需要统一 `assetId/prompt/referenceAssetImageId/referenceImageInStatus`。

3. `AssetManagementViewModel.generateAll()` 当前一键生成后重新拉三类列表；新版一键生成返回 `items`，但为了保持 UI 状态可靠，仍可以在成功后调用新版 `story/list` 统一刷新。

4. `ReferenceHistoricalImageryViewModel` 当前使用 `work/asset/getReferenceImage`，响应只有 `appearanceId/imageUrl`；新版 `reference/image/list` 响应字段明显更完整，前端模型如果继续只保留两个字段，会丢失 `acted/currentSelected/referenceImageInStatus` 等状态。

5. `saveImage(...)` 当前通过旧 save 接口保存 `appearanceId/imageUrl` 来选用历史图片；新版应改走 `story/select-image`。

6. `cancelImage(...)` 当前根据类型调用三类 `appearance/unset`；新版应改走 `story/remove-image`，并按接口要求传 `workId/storyId/assetId`，如果有当前图片则传 `assetImageId`。

7. 旧的 `workAssetRoleDelete/workAssetSceneDelete/workAssetPropDelete` 是资产从当前集解绑/删除，不等同于新版 `story/remove-image`。后端本次 9 个接口里没有单独的“删除资产”接口，若 UI 仍保留删除资产能力，需要向后端确认新版替代路径。

## 补充说明

- 没出现在本次目录里的接口按未变动处理，例如本地 `ai/task/query/asset/all` 和 `ai/task/query/asset/single` 轮询接口不纳入本次重构范围。
- ID 统一仍按本项目现有 `String?` 请求体处理，后台接受字符串 ID。
- header 的 `userToken` 和 `Accept` 仍由网络层统一处理；即使 body 里保留 token 字段也不影响后台。
- `genStatus=4` 已检查：`item_asset_management.xml` 已将 `status == 4` 展示为失败图标；`AssetManagementViewModel` 的失败统计、Generate All 提交条件和费用计算当前只把 `3`/`10` 等状态纳入失败或待生成逻辑，没有单独处理 `4`。如果后台通过接口 code 返回算力不足，当前代码已有 `RESPONSE_CODE_POWER_NOT_ENOUGH` 充值弹窗逻辑；如果后台通过数据状态 `genStatus=4` 返回，现有流程不会把它计入失败统计。
- `save` 字段变化说明：旧代码里 `saveImage(...)` 通过三类 save 接口传 `appearanceId/imageUrl/parentAppearanceId` 来复用历史图；新版已经拆出 `story/select-image`，参考生成使用 `image/generate` 的 `referenceAssetImageId/referenceImageInStatus`，所以这些字段不再属于新版 `save` 接口。

## 对应关系确定性

确定：目录内 9 个接口都属于 `Epnova/v1_2_0/资产管理`，与本地创作流程资产页的生成描述、当前集资产列表、保存资产、单图生成、一键生成、参考图、形象库、选图、移除图片功能逐项对应。

需要注意：`history/image/list` 是新版统一资产图片历史列表，本地旧代码里明确存在角色形象库分页 `workAssetRoleImagePage`，但没有看到场景/道具独立历史图片分页入口；因此它是对“形象库/历史图片能力”的升级合并，不是某一个旧接口的完全一对一改名。旧的三类 `*_DELETE` 是资产从当前集解绑/删除，本次 9 个接口里没有直接替代项，不能把 `story/remove-image` 当成资产删除接口。
