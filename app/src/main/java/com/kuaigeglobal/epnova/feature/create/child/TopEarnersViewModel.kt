package com.kuaigeglobal.epnova.feature.create.child

import androidx.paging.PagingData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.feature.creatorplay.data.TopEarnersCreatorFeedRepository
import kotlinx.coroutines.flow.Flow

class TopEarnersViewModel : AppRepositoryViewModel<AppRepository>() {
    override fun onCreate() {
        super.onCreate()
    }

    /**TopEarners 首页列表使用共享 CreatorWorkInfo feed，后续播放器从同一个 feed 派生播放单元。*/
    fun getTopEarners(): Flow<PagingData<CreatorWorkInfo>> {
        return TopEarnersCreatorFeedRepository.getWorkFeed()
    }
}
