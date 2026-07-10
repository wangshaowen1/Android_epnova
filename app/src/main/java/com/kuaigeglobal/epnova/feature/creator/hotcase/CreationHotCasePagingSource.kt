package com.kuaigeglobal.epnova.feature.creator.hotcase

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.request.PagedBody
import kotlinx.coroutines.CancellationException

class CreationHotCasePagingSource(
    private val api: ApiService
) : PagingSource<Int, CreationHotCaseInfo>() {

    override fun getRefreshKey(state: PagingState<Int, CreationHotCaseInfo>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CreationHotCaseInfo> {
        return try {
            val pageNum = params.key ?: 1
            val response = api.sameStyleHotCasePage(PagedBody(pageNum, params.loadSize))
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }
            val items = response.data?.items?.filterNotNull().orEmpty()
                .map(CreationHotCaseInfo::from)
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
