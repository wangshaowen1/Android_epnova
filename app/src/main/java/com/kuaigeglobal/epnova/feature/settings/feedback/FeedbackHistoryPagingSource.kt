package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.paging.PagingSource
import androidx.paging.PagingState

/** 暂无通知接口，先把本地数据按 Paging3 结构提供给页面。 */
class FeedbackHistoryPagingSource(
    private val items: List<FeedbackHistoryItem>
) : PagingSource<Int, FeedbackHistoryItem>() {

    override fun getRefreshKey(state: PagingState<Int, FeedbackHistoryItem>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedbackHistoryItem> {
        return LoadResult.Page(
            data = items,
            prevKey = null,
            nextKey = null
        )
    }
}
