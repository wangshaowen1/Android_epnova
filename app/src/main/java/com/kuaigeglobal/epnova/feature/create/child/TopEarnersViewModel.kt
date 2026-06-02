package com.kuaigeglobal.epnova.feature.create.child

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.TopEarnerWorkInfo
import kotlinx.coroutines.flow.Flow

class TopEarnersViewModel : AppRepositoryViewModel<AppRepository>() {
    override fun onCreate() {
        super.onCreate()
    }

    fun getTopEarners(): Flow<PagingData<TopEarnerWorkInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                TopEarnersPagingSource()
            }
        ).flow.cachedIn(viewModelScope)
    }
}
