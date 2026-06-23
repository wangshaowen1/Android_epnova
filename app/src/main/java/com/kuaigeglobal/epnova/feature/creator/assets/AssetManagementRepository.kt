package com.kuaigeglobal.epnova.feature.creator.assets

import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.request.TaskIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
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
     * 当前集资产列表
     */
    suspend fun workAssetStoryList(workId: String?, storyId: String?) = withContext(Dispatchers.IO) {
        api.workAssetStoryList(WorkBody(workId, storyId))
    }
}
