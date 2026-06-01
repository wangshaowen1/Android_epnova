package com.kuaigeglobal.epnova.feature.series

import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.ApiConstants
import com.kuaigeglobal.epnova.core.network.apis.request.SeriesFollowBody
import com.kuaigeglobal.epnova.core.network.apis.request.SeriesModifyInfoBody
import com.kuaigeglobal.epnova.core.network.apis.request.SeriesSetPriceBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkPublishStatusBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkPublishSubmitBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.POST

/**
 * 剧集
 */
class SeriesRepository : AppRepository() {
    /**
     * 作品详情
     * @param workId 剧集ID
     */
    suspend fun getWorkInfo(workId: String?) = withContext(Dispatchers.IO) {
        api.getWorkInfo(WorkIdBody(workId))
    }

    /**
     * 追剧或取消追剧
     * @param seriesId 剧集ID
     * @param followStatus 追剧状态 0-取消追剧 1-追剧
     */
    suspend fun seriesFollow(seriesId: String?, followStatus: Int) = withContext(Dispatchers.IO) {
        api.seriesFollow(SeriesFollowBody(seriesId, followStatus))
    }

    /**
     * 修改剧集价格
     */
    suspend fun seriesSetPrice(body: SeriesSetPriceBody) = withContext(Dispatchers.IO) {
        api.seriesSetPrice(body)
    }

    /**
     * 获取剧集价格配置
     * @param seriesId 剧集ID
     */
    suspend fun seriesPriceConfig(seriesId: String?) = withContext(Dispatchers.IO) {
        api.seriesPriceConfig(WorkIdBody(seriesId))
    }

    /**
     * 修改剧集信息
     * @param seriesId 剧集ID
     * @param seriesCover 短剧剧集封面
     * @param seriesName 短剧剧集名称
     *  @param seriesIntro 短剧剧集简介
     */
    suspend fun seriesModifyInfo(
        seriesId: String?,
        seriesCover: String?,
        seriesName: String?,
        seriesIntro: String?
    ) = withContext(Dispatchers.IO) {
        api.seriesModifyInfo(SeriesModifyInfoBody(seriesId, seriesCover, seriesName, seriesIntro))
    }

    /**
     * 上架剧集
     */
    @POST(ApiConstants.SERIES_PUBLISH)
    suspend fun seriesPublish(body: SeriesSetPriceBody) = withContext(Dispatchers.IO) {
        api.seriesPublish(body)
    }

    /**
     * 查询作品发行状态
     */
    suspend fun getWorkPublishStatus(workId: String?) = withContext(Dispatchers.IO) {
        api.workPublishStatus(WorkPublishStatusBody(workId))
    }

    /**
     * 提交作品剧集集中审核
     */
    suspend fun submitWorkPublish(
        workId: String?,
        auditBatchId: Long?,
        startEpisode: Int?,
        endEpisode: Int?
    ) = withContext(Dispatchers.IO) {
        api.workPublishSubmit(
            WorkPublishSubmitBody(
                workId = workId,
                auditBatchId = auditBatchId,
                startEpisode = startEpisode,
                endEpisode = endEpisode
            )
        )
    }
}
