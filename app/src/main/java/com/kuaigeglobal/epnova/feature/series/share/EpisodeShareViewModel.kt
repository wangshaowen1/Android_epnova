package com.kuaigeglobal.epnova.feature.series.share

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo
import com.kuaigeglobal.epnova.core.network.apis.request.UserShareUrlBody
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast

/**
 * 单集-分享
 */
class EpisodeShareViewModel : AppRepositoryViewModel<AppRepository>() {
    private val drama by lazy { mBundle.getParcelableValue(Constants.Extra.DRAMA, ShortDramaInfo::class.java) }
    val isMine by lazy { User.get().id == drama?.artist?.userToken }
    val loading by lazy { ObservableBoolean(false) }

    //分享链接
    private var shareUrl: String? = null

    fun getShareUrl(callback: ((shareUrl: String) -> Unit)?) {
        val url = shareUrl
        if (!url.isNullOrEmpty()) {
            callback?.invoke(url)
            return
        }
        loading.set(true)
        launch(block = {
            response(
                bean = mRepo.api.getUserShareUrl(UserShareUrlBody(1, drama?.dramasId, drama?.artist?.userToken)),
                result = {
                    loading.set(false)
                    val userShareUrl = data?.userShareUrl
                    if (!userShareUrl.isNullOrEmpty()) {
                        shareUrl = userShareUrl
                        callback?.invoke(userShareUrl)
                    }
                },
                error = {
                    loading.set(false)
                    this?.msg?.showToast()
                }
            )
        }, onError = {
            loading.set(false)
        })
    }

}