package com.kuaigeglobal.epnova.feature.works.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleImageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.RoleIdPagedBody

class RoleImagePagingSource(val roleId: String?, val totalCountBack: (Int) -> Unit) : PagingSource<Int, RoleImageInfo>() {

    override fun getRefreshKey(state: PagingState<Int, RoleImageInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RoleImageInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = RoleIdPagedBody(roleId, pageNum, params.loadSize)
            val apiResponse = ApiFactory.apiService.workAssetRoleImagePage(body)
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<RoleImageInfo> = ArrayList()
                val data = apiResponse.data?.items
                totalCountBack(apiResponse.data?.totalSize ?: 0)
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