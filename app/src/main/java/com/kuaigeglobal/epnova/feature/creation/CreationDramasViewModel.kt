package com.kuaigeglobal.epnova.feature.creation

import androidx.databinding.ObservableFloat
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.EntityCache
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.request.BannerBody
import com.kuaigeglobal.epnova.core.response

/** 
 * @createtime 2026年07月10日 14:36
 * @author TooWhiteT
 */

class CreationDramasViewModel : AppRepositoryViewModel<AppRepository>() {

    val tabIndicatorPosition by lazy { ObservableFloat(0f) }

    val selectedTab by lazy { MutableLiveData<Int>() }

    val bannerData = MutableLiveData<List<BannerInfo>>()

    private val bannerCache: EntityCache<List<BannerInfo>> by lazy {
        object : EntityCache<List<BannerInfo>>("CreationDramasBannerList") {}
    }

    override fun onCreate() {
        super.onCreate()
        bannerCache.cache?.let { bannerData.value = it }
        getBannerData()
    }

    private fun getBannerData() {
        launch {
            response(mRepo.api.getAppBanner(BannerBody(BannerBody.LOCATION_CREATE_PAGE))) {
                bannerData.value = data
                bannerCache.cache(data)
            }
        }
    }

}