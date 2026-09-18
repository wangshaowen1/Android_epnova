package com.kuaigeglobal.epnova.feature.message

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.MessagePageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.MessagePageBody
import kotlinx.coroutines.CancellationException

/** 消息列表分页数据源，负责把接口页码结果转换为 Paging 数据并传递刷新快照。 */
class NotificationsPagingSource(
    private val api: ApiService,
    private val onReadSnapshotToken: (String?) -> Unit
) : PagingSource<Int, MessagePageInfo>() {

    /** 根据可见位置推算刷新页，避免列表刷新后跳回固定的第一页。 */
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
            // Paging 首次请求没有 key，从接口约定的第 1 页开始；追加请求使用 loadSize 交给接口。
            val pageNum = params.key ?: 1
            val pageSize = params.loadSize
            val response = api.messagePage(
                MessagePageBody(
                    pageNum = pageNum,
                    pageSize = pageSize,
                    withReadSnapshot = 1
                )
            )
            // 业务失败也转换为 Paging 错误，页面才能统一展示错误状态并支持重试。
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            val pageData = response.data
            if (params is LoadParams.Refresh) {
                // 快照只在刷新成功后交给 ViewModel，追加页不重复触发全部已读请求。
                onReadSnapshotToken(pageData?.readSnapshotToken)
            }
            val items = pageData?.items?.filterNotNull().orEmpty()
            LoadResult.Page(
                data = items,
                prevKey = if (pageNum > 1) pageNum - 1 else null,
                // 空页或不足一页表示没有下一页，避免无效的追加请求。
                nextKey = if (items.isEmpty() || items.size < pageSize) null else pageNum + 1
            )
        } catch (error: CancellationException) {
            // 刷新或页面销毁导致的取消必须继续向上抛出，不能伪装成加载失败。
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }
}
