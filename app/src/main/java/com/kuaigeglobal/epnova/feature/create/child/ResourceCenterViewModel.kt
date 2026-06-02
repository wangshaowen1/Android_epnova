package com.kuaigeglobal.epnova.feature.create.child

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.OfficialConsultationInfo
import kotlinx.coroutines.flow.Flow

class ResourceCenterViewModel : AppRepositoryViewModel<AppRepository>() {
    override fun onCreate() {
        super.onCreate()
    }

    fun getResourceCenter(): Flow<PagingData<OfficialConsultationInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                ResourceCenterPagingSource()
            }
        ).flow.cachedIn(viewModelScope)
    }
}
