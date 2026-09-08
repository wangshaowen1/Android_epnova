package com.kuaigeglobal.epnova.feature.message

import androidx.paging.PagingSource
import androidx.paging.PagingState

/** 暂无通知接口，先把本地数据按 Paging3 结构提供给页面。 */
class NotificationsPagingSource(
    private val items: List<NotificationItem>
) : PagingSource<Int, NotificationItem>() {

    override fun getRefreshKey(state: PagingState<Int, NotificationItem>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationItem> {
        return LoadResult.Page(
            data = items,
            prevKey = null,
            nextKey = null
        )
    }
}
