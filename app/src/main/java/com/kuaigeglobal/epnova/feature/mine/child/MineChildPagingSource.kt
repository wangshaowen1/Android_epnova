package com.kuaigeglobal.epnova.feature.mine.child

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.PagedBody

/**
 * 历史，收藏记录
 */
class MineChildPagingSource(private val type: Int) : PagingSource<Int, WorkVOInfo>() {

    override fun getRefreshKey(state: PagingState<Int, WorkVOInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WorkVOInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = PagedBody(pageNum, params.loadSize)
            val apiResponse =
                if (type == MineChildFragment.TYPE_HISTORY) ApiFactory.apiService.userPlayHistoryPage(body) else ApiFactory.apiService.userCollectionPage(
                    body
                )
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<WorkVOInfo> = ArrayList()
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