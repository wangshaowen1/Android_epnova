package com.kuaigeglobal.epnova.feature.play.more

import android.app.Activity
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.manager.RewardedAdManager
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryUnlockInfoData
import com.kuaigeglobal.epnova.core.network.apis.request.StoryUnlockCoinsBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class EpisodeUnlockViewModel : AppRepositoryViewModel<AppRepository>() {

    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val episode by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, EpisodeInfo::class.java) }

    val unlockInfo = ObservableField<StoryUnlockInfoData>()

    /** 数据加载完成，动态内容区域可见 */
    val contentVisible = ObservableBoolean(false)

    /** 余额不足 */
    val coinsInsufficient = ObservableBoolean(false)

    /** 今天还有广告次数可用 */
    val hasAdRemaining = ObservableBoolean(false)

    /** "Watched today(X/10)" 文本 */
    val adCountText = ObservableField<String>()

    /** 无广告次数时的倒计时文本 */
    val countdownText = ObservableField<String>()

    /** 自动解锁开关 */
    val autoUnlock = ObservableBoolean(Config.get().isEpisodeAutoUnlock())

    /** 广告加载中（绑定到 Manager，供 Dialog 显示 loading 态） */
    val isAdLoading get() = adManager.isAdLoading

    private val AD_DAILY_MAX = 10

    /** 解锁成功回调（金币解锁和广告解锁共用） */
    var onUnlockSuccess: ((EpisodeInfo) -> Unit)? = null

    private var countdownJob: Job? = null

    /** 复用 RewardedAdManager 处理广告逻辑 */
    private val adManager: RewardedAdManager by lazy {
        RewardedAdManager(mRepo.api, viewModelScope).also { mgr ->
            mgr.onUnlockSuccess = {
                episode?.let { onUnlockSuccess?.invoke(it) }
            }
            mgr.onUnlockFailed = {
                R.string.episode_unlock_ad_not_ready
                    .let { showToast(it) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        loadUnlockInfo()
    }

    private fun loadUnlockInfo() {
        launchWithLoading {
            response(mRepo.api.storyUnlockInfo(WorkBody(workId, episode?.storyId))) {
                unlockInfo.set(data)
                val balance = data?.userCoinsBalance ?: 0L
                val cost = data?.costNums ?: 0
                coinsInsufficient.set(balance < cost)

                val adRemain = data?.adUnlockRemainCount ?: 0
                hasAdRemaining.set(adRemain > 0)
                val adWatched = AD_DAILY_MAX - adRemain
                adCountText.set(R.string.episode_unlock_watched_today.getResString("$adWatched", "$AD_DAILY_MAX"))

                if (adRemain <= 0) {
                    val nextDaySec = data?.nextDayRemainSeconds ?: 0L
                    startCountdown(nextDaySec)
                }
                contentVisible.set(true)
            }
        }
    }

    private fun startCountdown(remainSeconds: Long) {
        countdownJob?.cancel()
        countdownJob = launch {
            var remaining = remainSeconds
            while (remaining > 0) {
                val h = remaining / 3600
                val m = (remaining % 3600) / 60
                val s = remaining % 60
                countdownText.set(R.string.episode_unlock_come_back.getResString("%02d:%02d:%02d".format(h, m, s)))
                delay(1000)
                remaining--
            }
            countdownText.set(R.string.episode_unlock_come_back.getResString("00:00:00"))
        }
    }

    fun toggleAutoUnlock() {
        val newVal = !autoUnlock.get()
        autoUnlock.set(newVal)
        Config.get().saveEpisodeAutoUnlock(newVal)
    }

    /** 金币手动解锁（unlockType=2） */
    fun unlockWithCoins() {
        val wId = workId?.toLongOrNull() ?: return
        val sId = episode?.storyId?.toLongOrNull() ?: return
        launchWithLoading {
            response(mRepo.api.storyUnlockCoins(
                StoryUnlockCoinsBody(workId = wId, storyIds = listOf(sId), unlockType = 2)
            )) {
                episode?.let { onUnlockSuccess?.invoke(it) }
            }
        }
    }

    /**
     * 点击"Watch Ads"时调用：prepare + load + 广告加载完立即展示
     * 无需跳转到 AdUnlockDialog，直接在当前 Dialog 中完成全流程
     */
    fun startAdUnlock(context: Context, activity: Activity) {
        adManager.prepareLoadAndShow(context, activity, workId, episode?.storyId)
    }

    /** 获取当前广告剩余次数 */
    fun getAdRemainCount(): Int = unlockInfo.get()?.adUnlockRemainCount ?: 0

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        adManager.release()
    }
}
