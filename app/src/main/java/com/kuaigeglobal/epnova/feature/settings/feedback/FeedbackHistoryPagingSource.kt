package com.kuaigeglobal.epnova.feature.settings.feedback

import androidx.paging.PagingSource
import androidx.paging.PagingState

/** 临时使用本地假数据，后续接入反馈历史接口时替换数据来源。 */
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
