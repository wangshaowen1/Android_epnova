package com.kuaigeglobal.epnova.feature.works.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleImageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.AssetStoryImageBody

class RoleImagePagingSource(
    val workId: String?,
    val storyId: String?,
    val assetId: String?,
    val totalCountBack: (Int) -> Unit
) : PagingSource<Int, RoleImageInfo>() {

    override fun getRefreshKey(state: PagingState<Int, RoleImageInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RoleImageInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = AssetStoryImageBody(workId = workId, storyId = storyId, assetId = assetId)
            val apiResponse = ApiFactory.apiService.workAssetHistoryImageList(body)
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<RoleImageInfo> = ArrayList()
                val data = apiResponse.data
                totalCountBack(data?.size ?: 0)
                val nextKey = null
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
