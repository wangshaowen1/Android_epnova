package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackPageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.FeedbackPageBody
import kotlinx.coroutines.CancellationException

/** 按页码加载反馈历史，并将第 1 页返回的通知快照令牌交给上层处理。 */
class FeedbackHistoryPagingSource(
    private val api: ApiService,
    private val onNoticeSnapshotToken: (String?) -> Unit = {}
) : PagingSource<Int, FeedbackPageInfo>() {

    /** 根据当前列表锚点推算刷新页码；没有可用页码时由 load 从第 1 页开始。 */
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
                    // 仅第 1 页请求通知快照，后续加载更多不重复生成快照。
                    withNoticeSnapshot = if (pageNum == 1) 1 else 0
                )
            )
            // 业务失败同样转换为 Paging 错误状态，供页面展示错误并支持重试。
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
                // 空页或不足一页时结束追加加载，避免继续请求不存在的下一页。
                nextKey = if (items.isEmpty() || items.size < pageSize) null else pageNum + 1
            )
        } catch (error: CancellationException) {
            // 保留协程取消语义，刷新或销毁导致的取消不应被当作分页加载错误。
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }
}
