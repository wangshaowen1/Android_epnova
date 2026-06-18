# 资产管理 9 个接口重构分步实现文档

生成日期：2026-06-17

## 依据与范围

依据：

- `.agents/project/asset-management-api-refactor-analysis.md`
- Eolink/Apikit 项目 `epnova-APP`，tag `Epnova/v1_2_0/资产管理`
- 本地当前实现中的资产管理链路：`ApiConstants.kt`、`ApiService.kt`、`AssetManagementRepository.kt`、`AssetManagementViewModel.kt`、`ItemRoleVM.kt`、`ItemSceneVM.kt`、`ItemPropVM.kt`、`ReferenceHistoricalImageryViewModel.kt`、`RoleImagePagingSource.kt`

本文件只作为后续 AI 实现依据，不直接代表代码已完成。

实现原则：

- 先接网络层和模型，再改业务调用，最后清理旧接口引用。
- 优先保留当前 `SimpleAsset` + `ItemRoleVM/ItemSceneVM/ItemPropVM` UI 结构，通过统一资产模型做桥接，避免一次性重写列表 UI。
- 轮询接口 `ai/task/query/asset/all`、`ai/task/query/asset/single` 本次不在 9 个新接口内，默认保留旧轮询调用。
- 旧三类 `*_DELETE` 是“资产从当前集解绑/删除”，新 `story/remove-image` 只是移除当前资产图片，不能直接替代删除资产。
- 默认不跑 Gradle；实现完成后先用 `rg` 做静态检查，除非用户明确要求构建。

## 路径修正

原对照文档中 3 个 AI 任务类接口写成 `/new/...`，接口平台详情确认实际路径带 `ai` 前缀。`ApiConstants` 中常量按项目现有风格不写开头 `/`。

| 功能 | 接口平台确认路径 | 建议常量值 |
| --- | --- | --- |
| 生成资产描述 | `POST /ai/new/work/asset/description/generate` | `ai/new/work/asset/description/generate` |
| 一键生成资产图 | `POST /ai/new/work/asset/image/generate/batch` | `ai/new/work/asset/image/generate/batch` |
| 生成单个资产图 | `POST /ai/new/work/asset/image/generate` | `ai/new/work/asset/image/generate` |

## 建议分批顺序

1. 网络层与数据模型：新增 9 个常量、Body、Entity、ApiService 方法、Repository 包装方法。
2. 当前集资产列表：把 `AssetManagementViewModel.getAssets()` 从三接口并发改为 `story/list` 一次请求。
3. 统一保存资产：把 `ItemRoleVM/ItemSceneVM/ItemPropVM.save()` 和生成前保存改为 `asset/save`。
4. 单图生成与一键生成：改 `image/generate`、`image/generate/batch`，保留旧轮询。
5. 参考生图、历史图片、选图、移除图片：改参考弹层、形象库/历史图入口、`saveImage()`、`cancelImage()`。
6. 收尾检查：确认旧接口引用、状态 4、删除资产缺口和事件同步。

## 公共模型建议

优先新增统一模型，放在 `core/network/apis/entity` 与 `request` 包下。

请求体：

- `AssetSaveBody(workId, storyId, assetId, assetType, assetName, assetPrompt)`
- `AssetGenerateBody(workId, storyId, assetId, prompt, referenceAssetImageId, referenceImageInStatus)`
- `AssetImageListBody(workId, storyId, assetId)`
- `AssetImageSelectBody(workId, storyId, assetId, assetImageId)`
- `AssetStoryListBody` 可直接复用 `WorkBody(workId, storyId)`

响应模型：

- `AssetStoryListInfo(workId, storyId, roles, scenes, props)`
- `AssetInfo(usageId, assetId, assetType, assetName, assetPrompt, gender, ageGroup, protagonistFlag, voiceId, sortNo, assetImageId, imageUrl, genStatus, taskId, selectedStatus, actedStatus, sourceStoryId, sourceUsageId, hasHistory)`
- `AssetGenerateResult(assetId, assetImageId, taskId, genStatus)`
- `AssetBatchGenerateResult(submittedCount, skippedCount, items)`
- `AssetImageInfo(assetImageId, assetId, imageUrl, referenceAssetImageId, referenceImageUrl, referenceImageInStatus, currentSelected, acted)`

ID 类型按本项目现状继续用 `String?`，即使接口平台 schema 是 `int64`。这是当前仓库既有约定，避免连锁修改大量调用点。

桥接策略：

- 短期建议把 `AssetInfo` 映射成现有 `SimpleRoleInfo/SimpleSceneInfo/SimplePropInfo`。
- 映射关系：`assetId -> roleId/sceneId/propId`，`assetName -> *Name`，`assetPrompt -> *Prompt`，`assetImageId -> appearanceId`，`imageUrl -> imageUrl`，`genStatus -> genStatus`，`taskId -> taskId`，`hasHistory -> hasHistory`。
- 若实现中需要区分旧 `appearanceId` 与新 `assetImageId`，再给三类 `Simple*Info` 增加 `assetImageId` 字段；不要把新字段只存在 UI 临时变量里。

## 1. 生成当前集资产描述

接口：`POST /ai/new/work/asset/description/generate`

旧接口：`GENERATE_ALL_ASSET_DESCRIPTION = ai/asset/generation/description`

实现步骤：

1. 将 `ApiConstants.GENERATE_ALL_ASSET_DESCRIPTION` 改为 `ai/new/work/asset/description/generate`，或新增新常量后替换调用点。
2. `ApiService.generateAllAssetDescription(@Body WorkBody)` 可保持签名不变。
3. `AssetManagementViewModel.generateAllAssetDescription()` 可保持流程不变：提交后保存 `taskId`，`genStatus` 为 1/2 时进入轮询。
4. `workAssetAllStatus(TaskIdBody)` 继续走旧轮询接口。

检查点：

- `rg -n "GENERATE_ALL_ASSET_DESCRIPTION|ai/asset/generation/description"` 确认旧路径无遗漏。
- 只改 URL 时不要改 UI 文案和轮询逻辑。

## 2. 当前集资产列表

接口：`POST /new/work/asset/story/list`

旧接口：`WORK_ASSET_ROLE_LIST`、`WORK_ASSET_SCENE_LIST`、`WORK_ASSET_PROP_LIST`

实现步骤：

1. 新增常量 `WORK_ASSET_STORY_LIST = "new/work/asset/story/list"`。
2. `ApiService` 新增 `workAssetStoryList(@Body body: WorkBody): ApiResponse<AssetStoryListInfo?>`。
3. `AssetManagementRepository` 新增 `workAssetStoryList(workId, storyId)`，内部用 `withContext(Dispatchers.IO)`。
4. `AssetManagementViewModel.getAssets()` 改为一次请求 `workAssetStoryList(workId, storyId)`。
5. 将 `roles/scenes/props` 分别映射为 `ItemRoleVM/ItemSceneVM/ItemPropVM`，保留现有 title、`hasRole/hasScene/hasProp`、`adapter.submitList(items)`、`onItemChanged()`。
6. 若后续单图生成或选图需要 `assetImageId`，确保映射时不要丢字段。

检查点：

- `rg -n "workAssetRoleList|workAssetSceneList|workAssetPropList" app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets` 确认资产管理主列表不再并发请求三旧接口。
- 确认空列表状态仍能正确设置 `hasRole/hasScene/hasProp`。
- 确认 `status == 1` 的 item 仍会触发 `loopGenStatus()`。

## 3. 保存资产

接口：`POST /new/work/asset/save`

旧接口：`WORK_ASSET_ROLE_SAVE`、`WORK_ASSET_SCENE_SAVE`、`WORK_ASSET_PROP_SAVE`

接口字段：

- 必填：`workId`、`assetType`
- 可选：`storyId`、`assetId`、`assetName`、`assetPrompt`
- `assetType`：`1=角色`，`2=场景`，`3=道具`

实现步骤：

1. 新增常量 `WORK_ASSET_SAVE = "new/work/asset/save"`。
2. 新增 `AssetSaveBody` 和 `ApiService.workAssetSave()`。
3. `AssetManagementRepository` 新增统一保存方法，例如 `workAssetSave(assetType, assetId, workId, storyId, assetName, assetPrompt)`。
4. 在 `ItemRoleVM.save()`、`ItemSceneVM.save()`、`ItemPropVM.save()` 中保留原敏感词检查，只替换网络请求。
5. 在三个 `generate(...)` 方法中，生成前“描述/名称变更时先保存”的逻辑继续保留，但保存接口改为 `workAssetSave`。
6. 保存成功后用返回 `AssetInfo` 映射回当前 ItemVM 的 `data`，至少更新 `assetId/name/prompt/imageUrl/genStatus/taskId/assetImageId/hasHistory`。

检查点：

- `rg -n "workAssetRoleSave|workAssetSceneSave|workAssetPropSave" app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets` 确认创作流程资产页不再用三类 save。
- 不要继续传 `appearanceId/imageUrl/parentAppearanceId/source` 到保存接口；这些字段不属于新版 `save`。
- 删除资产能力不要改成 `remove-image`，旧 delete 接口先保留或等待后端确认。

## 4. 生成单个资产图

接口：`POST /ai/new/work/asset/image/generate`

旧接口：`WORK_ASSET_SINGLE_GENERATE = ai/asset/generation/single/image`

接口字段：

- 必填：`workId`、`storyId`、`assetId`、`prompt`
- 可选：`referenceAssetImageId`、`referenceImageInStatus`
- 响应：`assetId`、`assetImageId`、`taskId`、`genStatus`

实现步骤：

1. 将单图生成常量改为 `ai/new/work/asset/image/generate`。
2. 将 `GenerateAssetBody` 替换或新增为新版 `AssetGenerateBody`。
3. 三个 `Item*VM.generate(...)` 中不再传 `bizType`、`appearanceId`、`referenceImageUrl`。
4. 参考图字段改用 `referenceAssetImageId`，取值优先用新版 `AssetImageInfo.assetImageId`；兼容期可兜底旧 `ReferenceImageInfo.appearanceId`。
5. 响应改为 `AssetGenerateResult`，成功后更新当前 item 的 `taskId/genStatus/assetImageId`，再调用现有 `loopGenStatus()`。
6. 旧 `workAssetSingleStatus(TaskIdBody(taskId, bizType))` 先保留。三类 bizType 仍按角色 41、道具 51、场景 61 传入，除非后端确认轮询也已改造。

检查点：

- `rg -n "bizType = if|referenceImageUrl|referenceImageId|appearanceId =" app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets/Item*VM.kt` 确认新版单图生成不再传旧字段。
- 确认算力不足 code 仍走 `showRechargeDialog(true)`。
- 确认 `genStatus=4` 被纳入失败/待重试统计，见“状态处理”章节。

## 5. 一键生成当前集资产图

接口：`POST /ai/new/work/asset/image/generate/batch`

旧接口：`WORK_ASSET_ONE_KEY_GENERATE = ai/asset/oneClickGeneration`

接口字段：

- 入参：`workId`、`storyId`
- 响应：`submittedCount`、`skippedCount`、`items(assetId, assetImageId, taskId, genStatus)`

实现步骤：

1. 将一键生成常量改为 `ai/new/work/asset/image/generate/batch`。
2. `ApiService.workAssetOneKeyGenerate()` 响应类型改为 `ApiResponse<AssetBatchGenerateResult?>`。
3. `AssetManagementViewModel.generate(cost)` 中先保留现有逐项 `item.save(assetDesc, assetName)`，确保本地编辑先落库。
4. 调用 batch 接口成功后，建议直接调用新版 `getAssets()` 统一刷新列表；刷新后 `status == 1` 的 item 会继续轮询。
5. 如果不立即刷新，也可以用 `items` 局部更新 `taskId/genStatus/assetImageId`，但必须处理 skipped 项和本地 adapter 匹配失败。

检查点：

- `rg -n "workAssetOneKeyGenerate|ai/asset/oneClickGeneration"` 确认路径和响应类型已替换。
- 一键生成成本计算继续按本地 `status < 1 || status == 3 || status == 10 || status == 4` 统计。
- batch 接口返回 `submittedCount=0` 时不要误判成功进入下一步，应刷新列表或提示当前无可生成项。

## 6. 历史形象图列表

接口：`POST /new/work/asset/history/image/list`

旧接口：角色侧 `WORK_ASSET_ROLE_IMAGE_PAGE = v2/work/asset/role/appearance/list`；场景/道具本地没有等价历史图片分页接口。

接口字段：

- 必填：`workId`、`assetId`
- 可选：`storyId`
- 响应：`assetImageId`、`assetId`、`imageUrl`、`referenceAssetImageId`、`referenceImageUrl`、`referenceImageInStatus`、`currentSelected`、`acted`

实现步骤：

1. 新增常量 `WORK_ASSET_HISTORY_IMAGE_LIST = "new/work/asset/history/image/list"`。
2. 新增 `AssetImageListBody`、`AssetImageInfo`、`ApiService.workAssetHistoryImageList()`。
3. 该接口当前是普通数组，不是分页结构；若继续复用 `RoleImageSelectActivity` 的 Paging UI，需要把 PagingSource 改成一次性加载一页，或改为普通 RecyclerView 列表。
4. 角色形象库 `RoleImagePagingSource` 可切换到新接口；`RoleImageInfo` 建议替换为或映射自 `AssetImageInfo`。
5. 场景/道具旧 `AssetSelectActivity` 当前选的是作品级资产列表，不是当前资产历史图片。是否改成历史图片列表，需要产品/后端确认；不能直接等价替换。

检查点：

- `rg -n "workAssetRoleImagePage|WORK_ASSET_ROLE_IMAGE_PAGE|RoleImagePagingSource"` 确认角色形象库替换范围。
- 历史列表选中态应使用 `currentSelected`，已出演态使用 `acted`。
- 如果继续用旧 `appearanceId` 作为选中 key，需要映射为 `assetImageId`。

## 7. 参考生图图片列表

接口：`POST /new/work/asset/reference/image/list`

旧接口：`GET_HISTORY_REFERENCE_IMAGE = work/asset/getReferenceImage`

接口字段：

- 必填：`workId`、`assetId`
- 可选：`storyId`
- 响应：`assetImageId`、`assetId`、`imageUrl`、`referenceAssetImageId`、`referenceImageUrl`、`referenceImageInStatus`

实现步骤：

1. 新增常量 `WORK_ASSET_REFERENCE_IMAGE_LIST = "new/work/asset/reference/image/list"`。
2. `ReferenceHistoricalImageryViewModel.getReferenceHistoricalImagery()` 改为调用新接口。
3. `ReferenceHistoricalImageryDialog` 入参不再需要 `bizType`；可先保留 bundle 字段但不再传给 body，减少调用侧改动。
4. `ItemReferenceImageVm` 数据类型改为 `AssetImageInfo`，或扩展 `ReferenceImageInfo` 增加新版字段。
5. `Item*VM.generate(..., reference)` 使用 `reference.assetImageId` 作为 `referenceAssetImageId`。

检查点：

- `rg -n "GET_HISTORY_REFERENCE_IMAGE|getHistoryReferenceImage|ReferenceImageBody|BIZ_TYPE_.*IMAGE"` 确认参考弹层不再依赖旧 bizType。
- 取消参考仍传 `referenceImageInStatus = 0` 或不传参考字段，按实现统一处理。

## 8. 从历史图片选择/替换当前资产图

接口：`POST /new/work/asset/story/select-image`

旧功能：`AssetManagementViewModel.saveImage(...)` 复用三类 save 接口传 `appearanceId/imageUrl`

接口字段：

- 必填：`workId`、`storyId`、`assetId`
- 可选：`assetImageId`；选择要替换的图片时必填

实现步骤：

1. 新增常量 `WORK_ASSET_STORY_SELECT_IMAGE = "new/work/asset/story/select-image"`。
2. 新增 `ApiService.workAssetStorySelectImage(@Body AssetImageSelectBody)`。
3. `AssetManagementViewModel.saveImage(...)` 不再调用三类 save。
4. 从选择页返回后取当前资产 `assetId` 与被选图片 `assetImageId`，调用 `story/select-image`。
5. 接口成功后建议调用 `getAssets()` 统一刷新；如果局部更新，至少更新 `status=2`、`assetImage=imageUrl`、`assetImageId`。

检查点：

- `rg -n "fun saveImage|imageUrl =|parentAppearanceId|workAsset.*Save"` 确认选图不再靠 save 副作用。
- 场景/道具旧选择页当前返回 `SimpleAsset`，不是 `AssetImageInfo`；接入前必须先明确新选择页数据来源。

## 9. 移除当前集资产图片

接口：`POST /new/work/asset/story/remove-image`

旧接口：`CANCEL_ROLE_IMAGE`、`CANCEL_SCENE_IMAGE`、`CANCEL_PROP_IMAGE`

接口字段：

- 必填：`workId`、`storyId`、`assetId`
- 可选：`assetImageId`

实现步骤：

1. 新增常量 `WORK_ASSET_STORY_REMOVE_IMAGE = "new/work/asset/story/remove-image"`。
2. 新增 `ApiService.workAssetStoryRemoveImage(@Body AssetImageSelectBody)`。
3. `AssetManagementViewModel.cancelImage(vm)` 改为统一传 `workId/storyId/assetId/assetImageId`。
4. 成功后建议调用 `vm.updateStatus(0, null)` 并清空本地 `assetImageId`；更稳妥是调用 `getAssets()` 刷新。
5. 该接口不能替代删除资产按钮里的 `item.delete()`。

检查点：

- `rg -n "cancelRoleImage|cancelSceneImage|cancelPropImage|AppearanceIdBody"` 确认创作流程移除图片已改为新接口。
- `rg -n "workAssetRoleDelete|workAssetSceneDelete|workAssetPropDelete|deleteAsset"` 确认删除资产逻辑未被误替换。

## 状态处理

新接口明确 `genStatus` 包含：

- `0`：待生成
- `1`：生成中
- `2`：成功
- `3`：失败
- `4`：算力不足
- `10`：本地旧逻辑表示取消设置，继续按待生成处理

需要调整：

- `AssetManagementViewModel.onItemChanged()`：`4` 应纳入失败或可重试状态，建议和 `3` 一样处理。
- `AssetManagementViewModel.calcCost()`：`4` 应计入需要重新生成的数量。
- `AssetManagementViewModel.generate(cost)`：`4` 应纳入 `notGenerateItems`。
- `item_asset_management.xml` 已展示 `status == 4` 失败图标，但业务统计仍需补齐。

## 删除资产缺口

本次 9 个新接口没有“删除资产/从当前集解绑资产”的直接替代接口。当前代码里的：

- `workAssetRoleDelete`
- `workAssetSceneDelete`
- `workAssetPropDelete`

仍对应 `ItemAssetVM.delete()` 和删除按钮。后续实现时有三种选择：

1. 后端补充新版删除/解绑接口后再迁移。
2. 临时保留旧三类 delete 接口，只迁移本次 9 个明确接口。
3. 若产品决定取消删除资产能力，再改 UI 交互；这属于 UI/产品变更，不应在接口重构里擅自处理。

默认采用第 2 种，除非用户或后端明确要求。

## 收尾静态检查清单

实现完成后至少执行：

```powershell
rg -n "ai/asset/generation/description|ai/asset/generation/single/image|ai/asset/oneClickGeneration" app/src/main/java
rg -n "v2/work/asset/(role|scene|prop)/(list/story|save|appearance/unset)" app/src/main/java
rg -n "workAssetRoleList|workAssetSceneList|workAssetPropList|workAssetRoleSave|workAssetSceneSave|workAssetPropSave" app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets
rg -n "GET_HISTORY_REFERENCE_IMAGE|getHistoryReferenceImage|ReferenceImageBody" app/src/main/java
rg -n "referenceImageUrl|referenceImageId|bizType = if" app/src/main/java/com/kuaigeglobal/epnova/feature/creator/assets
rg -n "status.get\\(\\) == 4|status == 4|genStatus.*4" app/src/main/java app/src/main/res
```

预期：

- 资产管理创作流程不再调用旧 list/save/单图/一键/参考图/移除图片接口。
- 旧 delete 接口可能仍存在，这是本次明确保留的风险点。
- 轮询接口旧路径仍存在，这是本次范围外接口。

## 实现前必须复查的问题

1. 接口平台路径是否仍是 `ai/new/...` 和 `new/...`，不要只按旧对照文档复制。
2. `history/image/list` 是否后续会补分页；当前接口详情是普通数组。
3. 场景/道具“选择图片”是否应从作品级资产选择改为当前资产历史图片选择。
4. 删除资产是否有新版替代接口。
5. 单图/一键生成后旧轮询接口是否继续要求 `bizType`。
