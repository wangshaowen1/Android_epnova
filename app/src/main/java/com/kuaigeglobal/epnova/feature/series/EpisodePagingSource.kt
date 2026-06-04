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
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
