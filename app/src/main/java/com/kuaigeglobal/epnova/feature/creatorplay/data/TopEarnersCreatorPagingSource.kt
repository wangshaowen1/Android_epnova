package com.kuaigeglobal.epnova.feature.creatorplay.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.PagedBody
import com.kuaigeglobal.epnova.feature.creatorplay.mapper.TopEarnersCreatorMapper

/**
 * TopEarners 推荐作品分页源。
 *
 * 这里的输出保持作品维度 CreatorWorkInfo，不能提前转成 CreatorPlayableEpisode。
 * 播放器播放单元由后续播放器 feed 从同一个 work feed 派生。
 */
class TopEarnersCreatorPagingSource : PagingSource<Int, CreatorWorkInfo>() {

    override fun getRefreshKey(state: PagingState<Int, CreatorWorkInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CreatorWorkInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.recommendWorkList(PagedBody(pageNum, params.loadSize))
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val data = apiResponse.data?.items
                val nextKey = if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
                // 接口层已直接解析为 CreatorWorkInfo；这里只做 TopEarners 单集字段兜底。
                val items = data.orEmpty().mapNotNull { work ->
                    TopEarnersCreatorMapper.normalizeWork(work)
                }
                LoadResult.Page(items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
