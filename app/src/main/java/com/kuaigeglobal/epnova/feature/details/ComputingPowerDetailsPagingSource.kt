package com.kuaigeglobal.epnova.feature.details

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.DetailTypeBody
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import com.kuaigeglobal.epnova.feature.details.child.ComputingPowerDetailsTabFragment

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取充值明细分页
 * @Data: 2025/9/10/周三  17:51
 * @Version: 1.0
 * *****************************************
 * @param type 明细类型（0-全部、1-收入明细、2-消耗明细）
 * @param assetType 资产类型（Coins/Credits）
 */
class ComputingPowerDetailsPagingSource(
    private val type: Int,
    private val assetType: Int
) : PagingSource<Int, ComputingPowerDetailsInfo>() {

    override fun getRefreshKey(state: PagingState<Int, ComputingPowerDetailsInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComputingPowerDetailsInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = DetailTypeBody(type, pageNum, params.loadSize)
            val apiResponse = if (assetType == ComputingPowerDetailsTabFragment.ASSET_TYPE_CREDITS) {
                ApiFactory.apiService.queryPointDetailList(body)
            } else {
                ApiFactory.apiService.queryCoinsDetailList(body)
            }
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<ComputingPowerDetailsInfo> = ArrayList()
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