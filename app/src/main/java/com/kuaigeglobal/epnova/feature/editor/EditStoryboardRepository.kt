package com.kuaigeglobal.epnova.feature.editor

import com.kuaigeglobal.epnova.core.data.CommonRepository
import com.kuaigeglobal.epnova.core.network.apis.request.DramasIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.DramasTimbreBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageDescOptimizeBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageRegenerateBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageReplaceBody
import com.kuaigeglobal.epnova.core.network.apis.request.SegmentImageRetryBody
import com.kuaigeglobal.epnova.core.network.apis.request.ShotSegmentIdBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 短剧编辑-仓库
 */
class EditStoryboardRepository : CommonRepository() {

    /**
     * 合成短剧
     * @param dramasId 剧本id
     */
    suspend fun shortDramasInfo(dramasId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasInfo(DramasIdBody(dramasId)) }

    /**
     * 合成短剧
     * @param dramasId 剧本id
     */
    suspend fun shortDramasSynthesis(dramasId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasSynthesis(DramasIdBody(dramasId)) }

    suspend fun calculatePrice(dramaId: String?) = withContext(Dispatchers.IO) {
        api.shotDramasCalculatePrice(DramasIdBody(dramaId))
    }

    /**
     * 解锁首次生成的短剧(首次生成的短剧付费)
     * @param dramasId 剧本id
     */
    suspend fun unlockFirstGeneratedShortDrama(dramasId: String?) =
        withContext(Dispatchers.IO) { api.unlockFirstGeneratedShortDrama(DramasIdBody(dramasId)) }

    /**
     * 提交字幕配音
     * @param dramasId 剧本id
     * @param timbreId 字幕音色id
     */
    suspend fun shortDramasDubbing(dramasId: String?, timbreId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasDubbing(DramasTimbreBody(dramasId, timbreId)) }

    /**
     * 字幕配音重试
     * @param dramasId 剧本id
     * @param timbreId 字幕音色id
     */
    suspend fun shortDramasDubbingRetry(dramasId: String?, timbreId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasDubbingRetry(DramasTimbreBody(dramasId, timbreId)) }

    /**
     * 使用配音
     * @param dramasId 剧本id
     * @param timbreId 字幕音色id
     */
    suspend fun shortDramasDubbingUse(dramasId: String?, timbreId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasDubbingUse(DramasTimbreBody(dramasId, timbreId)) }

    /**
     * 字幕配音生成状态
     * @param dramasId 剧本id
     * @param timbreId 字幕音色id
     */
    suspend fun shortDramasDubbingStatus(dramasId: String?, timbreId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasDubbingStatus(DramasTimbreBody(dramasId, timbreId)) }

    /**
     * 使用历史分镜（分镜替换）
     * @param shotSegmentId 短剧分镜片段表id
     * @param shotSegmentImageId 短剧分镜镜片段图片id
     */
    suspend fun shortDramasSegmentImageReplace(shotSegmentId: String?, shotSegmentImageId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasSegmentImageReplace(SegmentImageReplaceBody(shotSegmentId, shotSegmentImageId)) }

    /**
     * 查询历史分镜（分镜替换）
     * @param shotSegmentId 短剧分镜片段表id
     */
    suspend fun shortDramasSegmentImageHistory(shotSegmentId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasSegmentImageHistory(ShotSegmentIdBody(shotSegmentId)) }

    /**
     * 重绘分镜（重新生成分镜的图片）
     * @param shotSegmentId 短剧分镜片段表id
     * @param shotImageDesc 分镜画面描述
     */
    suspend fun shortDramasSegmentImageRegenerate(shotSegmentId: String?, shotImageDesc: String?) =
        withContext(Dispatchers.IO) { api.shortDramasSegmentImageRegenerate(SegmentImageRegenerateBody(shotSegmentId, shotImageDesc)) }

    /**
     * 优化重绘分镜的画面描述
     * @param shotSegmentId 分镜id
     * @param shotImageDesc 分镜画面描述
     */
    suspend fun shortDramasSegmentImageDescOptimize(shotSegmentId: String?, shotImageDesc: String?) =
        withContext(Dispatchers.IO) { api.shortDramasSegmentImageDescOptimize(SegmentImageDescOptimizeBody(shotSegmentId, shotImageDesc)) }

    /**
     * 生成剧本图片(失败重试)
     * @param dramasId 短剧id，接受Long和String类型
     * @param shotSegmentId 短剧分镜片段id
     */
    suspend fun shortDramasSegmentImageGenerateRetry(dramasId: String?, shotSegmentId: String?) =
        withContext(Dispatchers.IO) { api.shortDramasSegmentImageGenerateRetry(SegmentImageRetryBody(dramasId, shotSegmentId)) }
}