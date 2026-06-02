package com.kuaigeglobal.epnova.feature.income

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeDetailInfo
import com.kuaigeglobal.epnova.core.network.apis.request.IncomeDetailPageBody
import kotlin.coroutines.cancellation.CancellationException

/**
 * 用户收益明细分页数据源。
 *
 * @param yearMonth 收益筛选年月，格式 yyyy-MM；为空时由服务端按当前结算年月兜底。
 */
class IncomeDetailsPagingSource(
    private val yearMonth: String?
) : PagingSource<Int, IncomeDetailInfo>() {

    override fun getRefreshKey(state: PagingState<Int, IncomeDetailInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, IncomeDetailInfo> {
        return try {
            val pageNum = params.key ?: 1
            val response = ApiFactory.apiService.userIncomeDetailPage(
                IncomeDetailPageBody(
                    month = yearMonth,
                    pageNum = pageNum,
                    pageSize = params.loadSize
                )
            )
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            // 分页排序由后端按收益金额倒序、创作时间倒序保证，客户端不重排，避免破坏分页边界。
            val items = response.data?.items?.filterNotNull().orEmpty()
            val prevKey = if (pageNum > 1) pageNum - 1 else null
            val nextKey = if (items.isEmpty() || items.size < params.loadSize) null else pageNum + 1
            LoadResult.Page(data = items, prevKey = prevKey, nextKey = nextKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
