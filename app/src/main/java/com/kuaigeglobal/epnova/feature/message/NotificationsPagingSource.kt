package com.kuaigeglobal.epnova.feature.message

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.MessagePageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.MessagePageBody
import kotlinx.coroutines.CancellationException

/** 消息列表分页数据源。 */
class NotificationsPagingSource(
    private val api: ApiService,
    private val onReadSnapshotToken: (String?) -> Unit
) : PagingSource<Int, MessagePageInfo>() {

    override fun getRefreshKey(state: PagingState<Int, MessagePageInfo>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, MessagePageInfo> {
        return try {
            val pageNum = params.key ?: 1
            val pageSize = params.loadSize
            val response = api.messagePage(
                MessagePageBody(
                    pageNum = pageNum,
                    pageSize = pageSize,
                    withReadSnapshot = 1
                )
            )
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            val pageData = response.data
            if (params is LoadParams.Refresh) {
                onReadSnapshotToken(pageData?.readSnapshotToken)
            }
            val items = pageData?.items?.filterNotNull().orEmpty()
            LoadResult.Page(
                data = items,
                prevKey = if (pageNum > 1) pageNum - 1 else null,
                nextKey = if (items.isEmpty() || items.size < pageSize) null else pageNum + 1
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }
}
