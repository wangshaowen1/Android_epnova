package com.kuaigeglobal.epnova.feature.play.more

import android.app.Activity
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.manager.RewardedAdManager
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

class AdUnlockViewModel : AppRepositoryViewModel<AppRepository>() {

    // 从 mBundle 读取参数（与 arguments 自动对应）
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val episode by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, EpisodeInfo::class.java) }
    val workCover by lazy { mBundle.getString(Constants.Extra.COVER) }

    /** 委托给 RewardedAdManager，暴露状态给 Dialog 绑定 */
    val adManager: RewardedAdManager by lazy {
        RewardedAdManager(mRepo.api, viewModelScope).also { mgr ->
            mgr.onUnlockSuccess = { onUnlockSuccess?.invoke() }
            mgr.onUnlockFailed  = { onUnlockFailed?.invoke() }
        }
    }

    // 透传状态（供 DataBinding / Dialog 直接使用）
    val isAdLoading get() = adManager.isAdLoading
    val isAdReady   get() = adManager.isAdReady
    val isPolling   get() = adManager.isPolling
    val statusText  get() = adManager.statusText

    var onUnlockSuccess: (() -> Unit)? = null
    var onUnlockFailed: (() -> Unit)? = null

    fun prepareAndLoadAd(context: Context) {
        adManager.prepareAndLoad(context, workId, episode?.storyId)
    }

    fun showAd(activity: Activity) {
        adManager.showAd(activity)
    }

    override fun onCleared() {
        super.onCleared()
        adManager.release()
    }
}
