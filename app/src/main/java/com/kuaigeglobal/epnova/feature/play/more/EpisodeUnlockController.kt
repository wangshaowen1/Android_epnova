package com.kuaigeglobal.epnova.feature.play.more

import android.app.Activity
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.RewardedAdManager
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.StoryUnlockInfoData
import com.kuaigeglobal.epnova.core.network.apis.request.StoryUnlockCoinsBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.BaseActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 剧集解锁逻辑控制器，作为嵌入 item_video_feed 中解锁覆盖层的数据绑定对象。
 * 不依赖 Fragment/ViewModel 生命周期，由外部传入 scope 管理协程。
 */
class EpisodeUnlockController(
    private val api: ApiService,
    private val scope: CoroutineScope,
    val workId: String?,
    val episode: EpisodeInfo?
) {

    // --- DataBinding 字段（与 include_episode_unlock.xml 绑定）---

    /** 数据加载完成后才显示动态内容区域 */
    val contentVisible = ObservableBoolean(false)

    /** 余额不足标志 */
    val coinsInsufficient = ObservableBoolean(false)

    /** 今日还有广告次数可用 */
    val hasAdRemaining = ObservableBoolean(false)

    /** "Watched today(X/10)" 文本 */
    val adCountText = ObservableField<String>()

    /** 无广告次数时的倒计时文本 */
    val countdownText = ObservableField<String>()

    /** 自动解锁开关状态 */
    val autoUnlock = ObservableBoolean(Config.get().isEpisodeAutoUnlock())

    /** 广告加载中标志（透传自 RewardedAdManager） */
    val isAdLoading get() = adManager.isAdLoading

    /** 广告已加载完成标志（透传自 RewardedAdManager） */
    val isAdReady get() = adManager.isAdReady

    /** 广告观看完成后的服务端验证中标志（透传自 RewardedAdManager） */
    val isPolling get() = adManager.isPolling

    /** 解锁成功回调（金币/广告共用） */
    var onUnlockSuccess: ((EpisodeInfo) -> Unit)? = null

    /**
     * 余额不足时回调：外部负责展示充值弹窗，并根据 adRemainCount 决定后续流程。
     */
    var onInsufficientCoins: ((adRemainCount: Int) -> Unit)? = null

    private val AD_DAILY_MAX = 10
    private var unlockInfo: StoryUnlockInfoData? = null
    private var countdownJob: Job? = null
    private var loadingActivity: BaseActivity? = null

    private val adManager: RewardedAdManager by lazy {
        RewardedAdManager(api, scope).also { mgr ->
            mgr.onAdLoaded = {
                hideAdLoading()
            }
            mgr.onAdLoadFailed = {
                hideAdLoading()
            }
            mgr.onUnlockSuccess = {
                hideAdLoading()
                episode?.let { ep ->
                    showToast(R.string.episode_unlock_success)
                    onUnlockSuccess?.invoke(ep)
                }
            }
            mgr.onUnlockFailed = {
                hideAdLoading()
                showToast(R.string.episode_unlock_ad_not_ready)
            }
        }
    }

    init {
        loadUnlockInfo()
    }

    // --- 内部逻辑 ---

    private fun loadUnlockInfo() {
        scope.launch {
            response(api.storyUnlockInfo(WorkBody(workId, episode?.storyId))) {
                unlockInfo = data
                val balance = data?.userCoinsBalance ?: 0L
                val cost = data?.costNums ?: 0
                coinsInsufficient.set(balance < cost)

                val adRemain = data?.adUnlockRemainCount ?: 0
                hasAdRemaining.set(adRemain > 0)
                val adWatched = AD_DAILY_MAX - adRemain
                adCountText.set(R.string.episode_unlock_watched_today.getResString(adWatched, AD_DAILY_MAX))

                if (adRemain <= 0) {
                    startCountdown(data?.nextDayRemainSeconds ?: 0L)
                }
                contentVisible.set(true)
            }
        }
    }

    private fun startCountdown(remainSeconds: Long) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
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

    // --- 供 View 层调用的公开方法 ---

    /** 点击"解锁/充值"按钮 */
    fun onClickUnlockNow() {
        if (coinsInsufficient.get()) {
            val adRemain = unlockInfo?.adUnlockRemainCount ?: 0
            onInsufficientCoins?.invoke(adRemain)
        } else {
            unlockWithCoins()
        }
    }

    /** 点击"看广告解锁"按钮 */
    fun onClickAdUnlock(context: Context, activity: Activity) {
        when {
            isPolling.get() -> Unit
            isAdReady.get() -> adManager.showAd(activity)
            isAdLoading.get() -> showAdLoading(activity)
            else -> {
                showAdLoading(activity)
                adManager.prepareLoadAndShow(context, activity, workId, episode?.storyId)
            }
        }
    }

    private fun showAdLoading(activity: Activity) {
        loadingActivity = activity as? BaseActivity
        loadingActivity?.showLoadingDialog()
    }

    private fun hideAdLoading() {
        loadingActivity?.hideLoadingDialog()
        loadingActivity = null
    }

    /** 切换自动解锁开关 */
    fun toggleAutoUnlock() {
        val newVal = !autoUnlock.get()
        autoUnlock.set(newVal)
        Config.get().saveEpisodeAutoUnlock(newVal)
    }

    fun getAdRemainCount(): Int = unlockInfo?.adUnlockRemainCount ?: 0

    // --- 金币解锁（内部） ---

    private fun unlockWithCoins() {
        val wId = workId?.toLongOrNull() ?: return
        val sId = episode?.storyId?.toLongOrNull() ?: return
        scope.launch {
            response(
                api.storyUnlockCoins(StoryUnlockCoinsBody(workId = wId, storyIds = listOf(sId), unlockType = 2))
            ) {
                episode.let { ep ->
                    showToast(R.string.episode_unlock_success)
                    User.get().storeCoins(data?.remainCoinsBalance)
                    onUnlockSuccess?.invoke(ep)
                }
            }
        }
    }

    /** 释放资源，在 ViewHolder 回收或覆盖层隐藏时调用 */
    fun release() {
        hideAdLoading()
        countdownJob?.cancel()
        adManager.release()
        onUnlockSuccess = null
        onInsufficientCoins = null
    }
}
