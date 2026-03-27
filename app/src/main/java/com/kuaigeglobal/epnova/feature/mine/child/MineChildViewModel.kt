package com.kuaigeglobal.epnova.feature.mine.child

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import com.kuaigeglobal.epnova.feature.details.ComputingPowerDetailsPagingSource
import kotlinx.coroutines.flow.Flow


class MineChildViewModel : AppRepositoryViewModel<AppRepository>() {

    private val type by lazy { mBundle.getInt(Constants.Extra.TYPE, MineChildFragment.TYPE_HISTORY) }

    fun isCollect(): Boolean {
        return type== MineChildFragment.TYPE_COLLECTION
    }

    fun getWorkInfo(): Flow<PagingData<WorkVOInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                MineChildPagingSource(type)
            }).flow.cachedIn(viewModelScope)
    }
}