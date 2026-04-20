package com.kuaigeglobal.epnova.core.manager

import android.app.Activity
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.kuaigeglobal.epnova.BuildConfig
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.request.StoryUnlockAdResultBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AdMob 激励广告管理器（可复用）
 *
 * 封装完整的广告解锁流程：
 *   prepare API → load RewardedAd（配置 SSV）→ show → poll 服务端结果
 *
 * 使用方式：
 * ```
 * val adManager = RewardedAdManager(mRepo.api, viewModelScope)
 * adManager.onUnlockSuccess = { ... }
 * adManager.onUnlockFailed  = { ... }
 *
 * // 方式1：预加载，用户点击时再展示（AdUnlockDialog 模式）
 * adManager.prepareAndLoad(context, workId, storyId)
 * adManager.showAd(activity)
 *
 * // 方式2：加载完成后立即展示（EpisodeUnlockDialog Watch Ads 模式）
 * adManager.prepareLoadAndShow(context, activity, workId, storyId)
 * ```
 */
class RewardedAdManager(
    private val api: ApiService,
    private val scope: CoroutineScope
) {

    companion object {
        /**
         * 激励广告位 ID
         */
        val REWARDED_AD_UNIT_ID: String
            get() = "ca-app-pub-9071858947673651~9374017668"

        private const val POLL_MAX_RETRY = 6
        private const val POLL_INTERVAL_MS = 2_000L
    }

    val isAdLoading = ObservableBoolean(false)
    val isAdReady = ObservableBoolean(false)
    val isPolling = ObservableBoolean(false)
    val statusText = ObservableField<String>()

    /** 广告解锁成功（服务端 SSV 验证通过）回调 */
    var onUnlockSuccess: (() -> Unit)? = null

    /** 广告解锁失败回调 */
    var onUnlockFailed: (() -> Unit)? = null

    private var rewardedAd: RewardedAd? = null
    private var nonce: String? = null
    private var currentWorkId: String? = null
    private var currentStoryId: String? = null

    // ── 公开方法 ─────────────────────────────────────────────────────────────

    /**
     * 预加载广告，加载完成后等待 [showAd] 调用（适合 AdUnlockDialog 模式）
     */
    fun prepareAndLoad(context: Context, workId: String?, storyId: String?) {
        if (isAdLoading.get() || isAdReady.get()) return
        currentWorkId = workId
        currentStoryId = storyId
        startPrepare(context, autoShowActivity = null)
    }

    /**
     * 预加载广告，加载完成后立即展示（适合点击按钮直接看广告的场景）
     */
    fun prepareLoadAndShow(context: Context, activity: Activity, workId: String?, storyId: String?) {
        if (isAdLoading.get() || isAdReady.get()) return
        currentWorkId = workId
        currentStoryId = storyId
        startPrepare(context, autoShowActivity = activity)
    }

    /**
     * 展示已预加载好的广告（与 [prepareAndLoad] 搭配使用）
     */
    fun showAd(activity: Activity) {
        rewardedAd?.show(activity, OnUserEarnedRewardListener { pollAdResult() })
    }

    /** ViewModel.onCleared 时调用，释放广告对象 */
    fun release() {
        rewardedAd = null
    }

    // ── 内部流程 ──────────────────────────────────────────────────────────────

    private fun startPrepare(context: Context, autoShowActivity: Activity?) {
        isAdLoading.set(true)
        statusText.set(R.string.episode_unlock_ad_loading.getResString())

        scope.launch {
            val prepareRes = withContext(Dispatchers.IO) {
                api.storyUnlockAdPrepare(WorkBody(currentWorkId, currentStoryId))
            }

            if (!prepareRes.isSuccess || prepareRes.data == null) {
                isAdLoading.set(false)
                statusText.set(null)
                R.string.episode_unlock_ad_not_ready.getResString().showToast()
                return@launch
            }

            val customData = prepareRes.data!!.customData
            nonce = prepareRes.data!!.nonce

            withContext(Dispatchers.Main) {
                loadRewardedAd(context, customData, autoShowActivity)
            }
        }
    }

    private fun loadRewardedAd(context: Context, customData: String?, autoShowActivity: Activity?) {
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading.set(false)
                    isAdReady.set(true)
                    statusText.set(null)

                    if (!customData.isNullOrEmpty()) {
                        ad.setServerSideVerificationOptions(
                            ServerSideVerificationOptions.Builder()
                                .setCustomData(customData)
                                .build()
                        )
                    }

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            isAdReady.set(false)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            rewardedAd = null
                            isAdReady.set(false)
                            R.string.episode_unlock_ad_not_ready.getResString().showToast()
                        }
                    }

                    // 如果调用方希望自动展示，广告加载完立即 show
                    autoShowActivity?.let { showAd(it) }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoading.set(false)
                    isAdReady.set(false)
                    statusText.set(null)
                    R.string.episode_unlock_ad_not_ready.getResString().showToast()
                }
            }
        )
    }

    private fun pollAdResult() {
        val wId = currentWorkId?.toLongOrNull() ?: return
        val sId = currentStoryId?.toLongOrNull() ?: return
        val n = nonce ?: return

        isPolling.set(true)
        statusText.set(R.string.episode_unlock_ad_verifying.getResString())

        scope.launch {
            repeat(POLL_MAX_RETRY) { attempt ->
                val res = withContext(Dispatchers.IO) {
                    api.storyUnlockAdResult(
                        StoryUnlockAdResultBody(workId = wId, storyId = sId, nonce = n)
                    )
                }

                if (res.isSuccess) {
                    when (res.data?.status) {
                        1 -> {
                            isPolling.set(false); statusText.set(null)
                            onUnlockSuccess?.invoke(); return@launch
                        }
                        2 -> {
                            isPolling.set(false); statusText.set(null)
                            onUnlockFailed?.invoke(); return@launch
                        }
                    }
                }

                if (attempt < POLL_MAX_RETRY - 1) delay(POLL_INTERVAL_MS)
            }

            isPolling.set(false)
            statusText.set(null)
            onUnlockFailed?.invoke()
        }
    }
}
