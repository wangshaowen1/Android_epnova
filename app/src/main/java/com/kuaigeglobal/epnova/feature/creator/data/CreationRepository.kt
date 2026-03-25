package com.kuaigeglobal.epnova.feature.creator.data

import com.kuaigeglobal.epnova.core.data.CommonRepository
import com.kuaigeglobal.epnova.core.network.apis.request.ContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreationGeneratePreBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramaContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramaGenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramaGenerateSourceReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramasIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.SSEContentBody
import com.kuaigeglobal.epnova.core.network.apis.request.OptimizeRoleImageDescBody
import com.kuaigeglobal.epnova.core.network.apis.request.PagedBody
import com.kuaigeglobal.epnova.core.network.apis.request.RoleIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.RoleImageRegenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.RoleSwitchBody
import com.kuaigeglobal.epnova.core.network.apis.request.SceneContentEditBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CreationRepository : CommonRepository() {

    suspend fun getStoryBoardList(dramaId: String?) = withContext(Dispatchers.IO) {
        api.getStoryBoardList(DramasIdBody(dramaId))
    }

    suspend fun getStoryBoardListRetry(dramaId: String?) = withContext(Dispatchers.IO) {
        api.getStoryBoardListRetry(DramasIdBody(dramaId))
    }

    suspend fun dramaContentEdit(dramaId: String?, content: String?) = withContext(Dispatchers.IO) {
        api.dramaContentEdit(DramaContentBody(dramaId, content))
    }

    suspend fun sceneContentEdit(body: SceneContentEditBody) = withContext(Dispatchers.IO) {
        api.sceneContentEdit(body)
    }

    suspend fun calculatePrice(dramaId: String?) = withContext(Dispatchers.IO) {
        api.shotDramasCalculatePrice(DramasIdBody(dramaId))
    }

    suspend fun shotDramaGenerate(dramaId: String?) = withContext(Dispatchers.IO) {
        api.shotDramaGenerate(DramasIdBody(dramaId))
    }

    /**
     * 预生成短剧剧本大纲
     */
    suspend fun creationGeneratePre(body: CreationGeneratePreBody) = withContext(Dispatchers.IO) {
        api.creationGeneratePre(body)
    }

    suspend fun getInspirationQuickList() = withContext(Dispatchers.IO) {
        api.getInspirationQuickList(UserTokenBody())
    }

    suspend fun videoSetConfig() = withContext(Dispatchers.IO) {
        api.videoSetConfig(UserTokenBody())
    }

    /**
     *优化生成剧本的角色的画面描述
     */
    suspend fun optimizeRoleImageDesc(body: OptimizeRoleImageDescBody) = withContext(Dispatchers.IO) {
        api.optimizeRoleImageDesc(body)
    }

    /**
     * 重新生成剧本的角色
     */
    suspend fun roleImageRegenerate(body: RoleImageRegenerateBody) = withContext(Dispatchers.IO) {
        api.roleImageRegenerate(body)
    }

    /**
     * 新增剧本的角色
     */
    suspend fun roleAdd(body: RoleImageRegenerateBody) = withContext(Dispatchers.IO) {
        api.dramasAddRole(body)
    }

    /**
     * 删除剧本的角色
     */
    suspend fun roleDel(body: RoleIdBody) = withContext(Dispatchers.IO) {
        api.dramasDelRole(body)
    }

    suspend fun getHistoryRoles(body: RoleIdBody) = withContext(Dispatchers.IO) {
        api.getHistoryRoles(body)
    }

    suspend fun historyRoleSwitch(body: RoleSwitchBody) = withContext(Dispatchers.IO) {
        api.historyRoleSwitch(body)
    }

    /**
     * 查询剧本的角色标签列表
     */
    suspend fun roleTagList() = withContext(Dispatchers.IO) {
        api.roleTagList(UserTokenBody())
    }

    /**
     * 预生成短剧剧本(流式输出完之后调)
     */
    suspend fun dramaGenerate(body: DramaGenerateBody) = withContext(Dispatchers.IO) {
        api.dramaGenerate(body)
    }

    /**
     * 生成短剧来源上报(调完生成短剧剧本接口后调用)
     */
    suspend fun dramaGenerateSourceReport(dramaId: String?, type: Int?, sourceId: String?) = withContext(Dispatchers.IO) {
        api.dramaGenerateSourceReport(DramaGenerateSourceReportBody(dramaId, sourceId, type))
    }

    /**
     * 查询剧本的角色列表(轮询使用)
     */
    suspend fun dramaRolesPolling(dramaId: String?) = withContext(Dispatchers.IO) {
        api.dramaRolesPolling(DramasIdBody(dramaId))
    }


    /**
     * 敏感词校验
     */
    suspend fun sensitiveWordCheck(content: String?) = withContext(Dispatchers.IO) {
        api.sensitiveWordCheck(ContentBody(content))
    }
}

