package com.kuaigeglobal.epnova.feature.create.child

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.TopEarnerWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.PagedBody

class TopEarnersPagingSource : PagingSource<Int, TopEarnerWorkInfo>() {

    override fun getRefreshKey(state: PagingState<Int, TopEarnerWorkInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TopEarnerWorkInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.recommendWorkList(PagedBody(pageNum, params.loadSize))
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val data = apiResponse.data?.items
                val items = ArrayList<TopEarnerWorkInfo>()
                val nextKey = if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
                data?.filterNotNull()?.let { items.addAll(it) }
                LoadResult.Page(items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
