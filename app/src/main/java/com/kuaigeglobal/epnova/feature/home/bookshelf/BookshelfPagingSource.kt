package com.kuaigeglobal.epnova.feature.home.bookshelf

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.request.BookShelfSectionCodePagedBody
import kotlin.coroutines.cancellation.CancellationException

/** 首页书架作品分页数据源。 */
class BookshelfPagingSource(
    private val api: ApiService,
    private val bookshelfId: String?,
    private val bookshelfName: String?
) : PagingSource<Int, WorkVOInfo>() {

    override fun getRefreshKey(state: PagingState<Int, WorkVOInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WorkVOInfo> {
        return try {
            val pageNum = params.key ?: 1
            if (!bookshelfId.isNullOrBlank()) {
                loadPagedWorks(bookshelfId, pageNum, params.loadSize)
            } else {
                loadNamedShelfWorks(pageNum)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    private suspend fun loadPagedWorks(
        bookshelfId: String,
        pageNum: Int,
        pageSize: Int
    ): LoadResult<Int, WorkVOInfo> {
        val response = api.homeHorizontalPage(
            BookShelfSectionCodePagedBody(
                bookshelfSectionCode = bookshelfId,
                pageNum = pageNum,
                pageSize = pageSize
            )
        )
        if (!response.isSuccess) {
            return LoadResult.Error(ApiException(response.code, response.msg))
        }

        val items = response.data?.items?.filterNotNull().orEmpty()
        return LoadResult.Page(
            data = items,
            prevKey = if (pageNum > 1) pageNum - 1 else null,
            nextKey = if (items.isEmpty() || items.size < pageSize) null else pageNum + 1
        )
    }

    private suspend fun loadNamedShelfWorks(pageNum: Int): LoadResult<Int, WorkVOInfo> {
        if (pageNum > 1) return LoadResult.Page(emptyList(), pageNum - 1, null)

        val response = api.bookShelfHome()
        if (!response.isSuccess) {
            return LoadResult.Error(ApiException(response.code, response.msg))
        }

        val section = response.data?.sections.orEmpty().firstOrNull { shelf ->
            shelf.bookshelfName.equals(bookshelfName, ignoreCase = true)
        } ?: response.data?.sections.orEmpty().firstOrNull { it.isHorizontal() }
        return LoadResult.Page(
            data = section?.horizontalWorks.orEmpty(),
            prevKey = null,
            nextKey = null
        )
    }
}
