package com.kuaigeglobal.epnova.feature.home.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.BookShelfWaterfallPagedBody
import kotlin.coroutines.cancellation.CancellationException

/** Trending 底部配置瀑布流分页。 */
class TrendingWaterfallPagingSource(
    private val api: ApiService,
    private val bookshelfSectionCode: Int
) : PagingSource<Int, WorkVOInfo>() {

    override fun getRefreshKey(state: PagingState<Int, WorkVOInfo>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WorkVOInfo> {
        return try {
            val pageNum = params.key ?: 1
            val response = api.bookShelfWaterfallPage(
                BookShelfWaterfallPagedBody(
                    bookshelfSectionCode = bookshelfSectionCode,
                    pageNum = pageNum,
                    pageSize = params.loadSize
                )
            )
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            val items = response.data?.items?.filterNotNull().orEmpty()
            LoadResult.Page(
                data = items,
                prevKey = if (pageNum > 1) pageNum - 1 else null,
                nextKey = if (items.isEmpty() || items.size < params.loadSize) null else pageNum + 1
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }
}
