package com.kuaigeglobal.epnova.feature.create

import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.EntityCache
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.request.BannerBody
import com.kuaigeglobal.epnova.core.response

class CreateViewModel : AppRepositoryViewModel<AppRepository>() {

    var editModel = MutableLiveData(false)
    val bannerData = MutableLiveData<List<BannerInfo>>()
    private val bannerCache: EntityCache<List<BannerInfo>> by lazy {
        object : EntityCache<List<BannerInfo>>("CreateGuideBannerList") {}
    }

    override fun onCreate() {
        super.onCreate()
        bannerCache.cache?.let { bannerData.value = it }
        getData()
    }

    fun getData(onComplete: (() -> Unit)? = null) {
        launch(
            block = {
                response(mRepo.api.getAppBanner(BannerBody(BannerBody.LOCATION_GUIDE_PAGE)), {
                    bannerData.value = data
                    bannerCache.cache(data)
                })
            },
            onComplete = {
                onComplete?.invoke()
            }
        )
    }
}
