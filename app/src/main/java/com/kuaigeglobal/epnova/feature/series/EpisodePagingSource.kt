package com.kuaigeglobal.epnova.feature.series

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody

class EpisodePagingSource(private val workId: String?) : PagingSource<Int, CreatorEpisodeInfo>() {

    override fun getRefreshKey(state: PagingState<Int, CreatorEpisodeInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CreatorEpisodeInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = WorkIdPagedBody(workId, pageNum, 30)
            val apiResponse = ApiFactory.apiService.episodeList(body)
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items = apiResponse.data?.items ?: emptyList()
                val nextKey = if (items.size < params.loadSize) null else pageNum + 1

                items.forEachIndexed { index, info ->
                    if (info.stepProgress != CreatorEpisodeInfo.NOT_CREATED) {
                        // 已经在流程中的则默认为true
                        info.nextCreate = true
                    } else {
                        // 没在流程中的需要找是否存在阻断点判断能否进入创作流程
                        info.nextCreate = !findEligibleEpisode(items, index)
                    }
                }

                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * 找到阻断点则不能下一步
     */
    private fun findEligibleEpisode(items: List<CreatorEpisodeInfo>, position: Int): Boolean {
        // 找点击之前的item是否存在有未创建 or 未进分镜预览阶段的剧集
        val eligibleIndex = items.take(position).indexOfFirst {
            it.stepProgress == CreatorEpisodeInfo.NOT_CREATED || (it.stepProgress ?: 0) < CreatorEpisodeInfo.PROGRESS_STORYBOARD_GENERATING
        }
        // 存在则返回剧集下标 不存在则返回点击的下标
        val blockerIndex = if (eligibleIndex == -1) position else eligibleIndex
        return blockerIndex < position
    }
}
