package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackPageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.FeedbackPageBody
import kotlinx.coroutines.CancellationException

/** 反馈历史分页数据源。 */
class FeedbackHistoryPagingSource(
    private val api: ApiService,
    private val onNoticeSnapshotToken: (String?) -> Unit = {}
) : PagingSource<Int, FeedbackPageInfo>() {

    override fun getRefreshKey(state: PagingState<Int, FeedbackPageInfo>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedbackPageInfo> {
        return try {
            val pageNum = params.key ?: 1
            val pageSize = params.loadSize
            val response = api.feedbackPage(
                FeedbackPageBody(
                    pageNum = pageNum,
                    pageSize = pageSize,
                    withNoticeSnapshot = if (pageNum == 1) 1 else 0
                )
            )
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            val pageData = response.data
            if (pageNum == 1) {
                onNoticeSnapshotToken(pageData?.noticeSnapshotToken)
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
