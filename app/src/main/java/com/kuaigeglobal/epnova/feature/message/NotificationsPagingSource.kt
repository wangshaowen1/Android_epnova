package com.kuaigeglobal.epnova.feature.message

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * 通知接口接入前的本地分页数据源。
 *
 * key 使用下一次加载的起始下标，而不是页码，这样即使 PagingConfig 的
 * initialLoadSize 大于 pageSize，也不会因为页大小变化而跳过或重复数据。
 */
class NotificationsPagingSource(
    private val items: List<NotificationItem>
) : PagingSource<Int, NotificationItem>() {

    override fun getRefreshKey(state: PagingState<Int, NotificationItem>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationItem> {
        val startIndex = (params.key ?: 0).coerceAtLeast(0)
        if (startIndex >= items.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null
            )
        }

        val loadSize = params.loadSize.coerceAtLeast(1)
        val endIndex = (startIndex + loadSize).coerceAtMost(items.size)
        val data = items.subList(startIndex, endIndex)

        return LoadResult.Page(
            data = data,
            prevKey = if (startIndex == 0) null else (startIndex - loadSize).coerceAtLeast(0),
            nextKey = endIndex.takeIf { it < items.size }
        )
    }
}
