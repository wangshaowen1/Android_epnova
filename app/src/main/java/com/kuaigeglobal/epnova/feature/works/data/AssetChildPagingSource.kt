package com.kuaigeglobal.epnova.feature.works.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.AssetInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.RoleImageInfo
import com.kuaigeglobal.epnova.core.network.apis.request.AssetStoryImageBody

class AssetChildPagingSource(
    val bizType: Int,
    val workId: String?,
    val storyId: String?,
    val assetId: String?,
    val totalCountBack: (Int) -> Unit
) : PagingSource<Int, AssetInfo>() {

    override fun getRefreshKey(state: PagingState<Int, AssetInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AssetInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = AssetStoryImageBody(workId = workId, storyId = storyId, assetId = assetId)
            val apiResponse = ApiFactory.apiService.workAssetHistoryImageList(body)
            if (apiResponse.isSuccess) {
                val data = apiResponse.data
                totalCountBack.invoke(data?.size ?: 0)
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<AssetInfo> = ArrayList()
                val nextKey = null
                data?.filterNotNull()?.map { it.toAssetInfo() }?.let { items.addAll(it) }
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun RoleImageInfo.toAssetInfo(): AssetInfo {
        val assetType = when (bizType) {
            AssetInfo.IMAGE_BIZ_TYPE_ROLE -> AssetInfo.TYPE_ROLE
            AssetInfo.IMAGE_BIZ_TYPE_SCENE -> AssetInfo.TYPE_SCENE
            else -> AssetInfo.TYPE_PROP
        }
        return AssetInfo(
            assetId = assetId,
            assetType = assetType,
            assetImageId = assetImageId,
            imageUrl = imageUrl,
            genStatus = 2,
            hasHistory = true
        )
    }
}
