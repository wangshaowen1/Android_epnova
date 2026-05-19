package com.kuaigeglobal.epnova.feature.works.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleAsset
import com.kuaigeglobal.epnova.core.network.apis.request.QueryAssetBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdPagedBody

class AssetChildPagingSource(val bizType: Int, val workId: String?, val totalCountBack: (Int) -> Unit) : PagingSource<Int, SimpleAsset>() {

    override fun getRefreshKey(state: PagingState<Int, SimpleAsset>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleAsset> {
        return try {
            val pageNum = params.key ?: 1
            val body = WorkIdPagedBody(workId, pageNum, params.loadSize)
            val apiResponse = when (bizType) {
                QueryAssetBody.BIZ_TYPE_ROLE -> ApiFactory.apiService.workAssetRolePage(body)
                QueryAssetBody.BIZ_TYPE_PROP -> ApiFactory.apiService.workAssetPropPage(body)
                else -> ApiFactory.apiService.workAssetScenePage(body)
            }
            if (apiResponse.isSuccess) {
                totalCountBack.invoke(apiResponse.data?.totalSize ?: 0)
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<SimpleAsset> = ArrayList()
                val data = apiResponse.data?.items
                // 加载下一页的键（为 null 表示没有下一页）
                val nextKey =
                    if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
                data?.filterNotNull()?.let { items.addAll(it) }
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}