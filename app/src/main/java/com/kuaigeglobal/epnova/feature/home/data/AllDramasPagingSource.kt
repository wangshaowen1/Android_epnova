package com.kuaigeglobal.epnova.feature.home.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.PagedBody
import kotlin.coroutines.cancellation.CancellationException

/** All Dramas 全部作品分页，与横滑区块配置、Trending 的 Room 数据独立。 */
class AllDramasPagingSource(
    private val api: ApiService
) : PagingSource<Int, WorkVOInfo>() {

    // 下拉刷新重新从第一页加载，避免保留旧的作品排序。
    override fun getRefreshKey(state: PagingState<Int, WorkVOInfo>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WorkVOInfo> {
        return try {
            val pageNum = params.key ?: 1
            val response = api.bookShelfAllDramasPage(PagedBody(pageNum, params.loadSize))
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            val items = response.data?.items.orEmpty()
            LoadResult.Page(
                data = items.filterNotNull(),
                prevKey = if (pageNum > 1) pageNum - 1 else null,
                nextKey = if (items.size < params.loadSize) null else pageNum + 1
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }
}
