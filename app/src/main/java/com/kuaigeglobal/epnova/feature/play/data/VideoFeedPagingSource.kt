package com.kuaigeglobal.epnova.feature.play.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody

class VideoFeedPagingSource(val workId: String?) : PagingSource<Int, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<Int, EpisodeInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EpisodeInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.episodeDirectoryPage(WorkIdPagedBody(workId, pageNum, 30))
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<EpisodeInfo> = ArrayList()
                val data = apiResponse.data?.items
                // 加载下一页的键（为 null 表示没有下一页）
                val nextKey =
                    if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
                data?.filterNotNull()?.let { items.addAll(it) }
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}