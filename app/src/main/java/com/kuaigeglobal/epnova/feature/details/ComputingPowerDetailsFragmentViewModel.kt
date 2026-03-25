package com.kuaigeglobal.epnova.feature.details

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:余额明细
 * @Data: 2025/9/10/周三  17:50
 * @Version: 1.0
 * *****************************************
 */
class ComputingPowerDetailsFragmentViewModel : AppRepositoryViewModel<AppRepository>() {

    private val typeProtagonist by lazy { mBundle.getInt(ComputingPowerDetailsFragment.TYPE_PROTAGONIST, ComputingPowerDetailsFragment.TYPE_ALL) }

    /**
     * 获取积分消耗明细分页
     * @return Flow<PagingData<RechargePageInfo>>
     */
    fun getComputingPowerDetailsPage(): Flow<PagingData<ComputingPowerDetailsInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                ComputingPowerDetailsPagingSource(typeProtagonist)
            }).flow.cachedIn(viewModelScope)
    }
}