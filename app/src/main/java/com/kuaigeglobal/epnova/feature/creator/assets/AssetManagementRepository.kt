package com.kuaigeglobal.epnova.feature.creator.assets

import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.request.IdsBody
import com.kuaigeglobal.epnova.core.network.apis.request.QueryAssetBody
import com.kuaigeglobal.epnova.core.network.apis.request.SavePropBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveRoleBody
import com.kuaigeglobal.epnova.core.network.apis.request.SaveSceneBody
import com.kuaigeglobal.epnova.core.network.apis.request.TaskIdBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 资产管理
 */
class AssetManagementRepository : AppRepository() {

    /**
     * 轮询查询全部资产
     * @param taskId 任务ID
     * @param bizType 查询单个资产必须要入参业务类型 查询全部资产、分镜描述、分镜视频不用入参业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]70=分镜拆分/文本描述 71=分镜图片 72=分镜视频
     */
    suspend fun workAssetAllStatus(taskId: String?, bizType: Int?) = withContext(Dispatchers.IO) {
        api.workAssetAllStatus(TaskIdBody(taskId, bizType))
    }

    /**
     * 轮询查询单个资产
     * @param taskId 任务ID
     * @param bizType 查询单个资产必须要入参业务类型 查询全部资产、分镜描述、分镜视频不用入参业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]70=分镜拆分/文本描述 71=分镜图片 72=分镜视频
     */
    suspend fun workAssetSingleStatus(taskId: String?, bizType: Int?) = withContext(Dispatchers.IO) {
        api.workAssetSingleStatus(TaskIdBody(taskId, bizType))
    }

    /**
     * 场景列表
     * @param assetId 资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参
     * @param appearanceId 当前资产形象ID 查询单个时入参
     * @param bizType 查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]
     * @param storyId 剧集 id
     * @param workId 作品 ID
     */
    suspend fun workAssetSceneList(assetId: String?, appearanceId: String?, bizType: Int?, storyId: String?, workId: String?) = withContext(Dispatchers.IO) {
        api.workAssetSceneList(QueryAssetBody(assetId, appearanceId, bizType, storyId, workId))
    }

    /**
     * 场景保存
     * @param sceneId 场景主键id
     * @param appearanceId 当前资产形象ID 查询单个时入参
     * @param workId 作品 ID
     * @param storyId 剧集 id
     * @param sceneName 场景名称
     * @param scenePrompt 场景图片描述提示词
     */
    suspend fun workAssetSceneSave(
        sceneId: String?,
        appearanceId: String?,
        workId: String?,
        storyId: String?,
        sceneName: String?,
        scenePrompt: String?
    ) = withContext(Dispatchers.IO) {
        api.workAssetSceneSave(SaveSceneBody(sceneId, appearanceId, workId, storyId, sceneName, scenePrompt))
    }

    /**
     * 场景单个删除
     */
    suspend fun workAssetSceneDelete(ids: List<String>?, id: String?) = withContext(Dispatchers.IO) {
        api.workAssetSceneDelete(IdsBody(ids, id))
    }

    /**
     * 角色列表
     * @param assetId 资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参
     * @param appearanceId 当前资产形象ID 查询单个时入参
     * @param bizType 查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]
     * @param storyId 剧集 id
     * @param workId 作品 ID
     */
    suspend fun workAssetRoleList(assetId: String?, appearanceId: String?, bizType: Int?, storyId: String?, workId: String?) = withContext(Dispatchers.IO) {
        api.workAssetRoleList(QueryAssetBody(assetId, appearanceId, bizType, storyId, workId))
    }

    /**
     * 角色保存
     * @param roleId 角色主键id
     * @param appearanceId 当前资产形象ID 查询单个时入参
     * @param workId 作品 ID
     * @param storyId 剧集 id
     * @param roleName 角色名称
     * @param rolePrompt 角色图片描述提示词
     */
    suspend fun workAssetRoleSave(roleId: String?, appearanceId: String?, workId: String?, storyId: String?, roleName: String?, rolePrompt: String?) = withContext(Dispatchers.IO) {
        api.workAssetRoleSave(SaveRoleBody(roleId, appearanceId, workId, storyId, roleName, rolePrompt))
    }

    /**
     * 角色单个删除
     */
    suspend fun workAssetRoleDelete(ids: List<String>?, id: String?) = withContext(Dispatchers.IO) {
        api.workAssetRoleDelete(IdsBody(ids, id))
    }

    /**
     * 道具列表
     * @param assetId 资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参
     * @param appearanceId 当前资产形象ID 查询单个时入参
     * @param bizType 查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]
     * @param storyId 剧集 id
     * @param workId 作品 ID
     */
    suspend fun workAssetPropList(assetId: String?, appearanceId: String?, bizType: Int?, storyId: String?, workId: String?) = withContext(Dispatchers.IO) {
        api.workAssetPropList(QueryAssetBody(assetId, appearanceId, bizType, storyId, workId))
    }

    /**
     * 道具保存
     * @param propId 道具主键id
     * @param appearanceId 当前资产形象ID 查询单个时入参
     * @param workId 作品 ID
     * @param storyId 剧集 id
     * @param propName 道具名称
     * @param propPrompt 道具图片描述提示词
     */
    suspend fun workAssetPropSave(propId: String?, appearanceId: String?, workId: String?, storyId: String?, propName: String?, propPrompt: String?) = withContext(Dispatchers.IO) {
        api.workAssetPropSave(SavePropBody(propId, appearanceId, workId, storyId, propName, propPrompt))
    }

    /**
     * 道具单个删除
     */
    suspend fun workAssetPropDelete(ids: List<String>?, id: String?) = withContext(Dispatchers.IO) {
        api.workAssetPropDelete(IdsBody(ids, id))
    }
}