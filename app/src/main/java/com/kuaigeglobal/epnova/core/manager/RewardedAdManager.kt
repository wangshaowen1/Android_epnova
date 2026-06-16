package com.kuaigeglobal.epnova.core.manager

import android.app.Activity
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.manager.ad.AdmobRewardedAdPlatform
import com.kuaigeglobal.epnova.core.manager.ad.IRewardedAdPlatform
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.request.StoryUnlockAdCompleteBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkBody
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Introduce: 激励广告管理器（平台无关）
 *
 * 封装完整的广告解锁流程：
 *   prepare API → load Ad（配置 SSV customData）→ show → complete 接口轮询查询 SSV 处理状态
 *
 * 广告平台通过 [IRewardedAdPlatform] 注入，切换平台只需修改默认参数：
 * ```
 * val adManager = RewardedAdManager(api, scope, ApplovinRewardedAdPlatform())
 * ```
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
 *
 * // 方式3：外部已完成 prepare，直接加载并展示，nonce 会写入 AdMob customData
 * adManager.loadAndShow(context, activity, nonce, adUnitId, onRewarded = { ... })
 * ```
 */
class RewardedAdManager(
    private val api: ApiService,
    private val scope: CoroutineScope,
    // 切换广告平台只需替换此默认参数
    private val platform: IRewardedAdPlatform = AdmobRewardedAdPlatform()
) {

    companion object {
        private const val TAG = "RewardedAd"
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

    /** 广告素材加载成功/失败回调，用于外部控制页面通用 loading。 */
    var onAdLoaded: (() -> Unit)? = null
    var onAdLoadFailed: (() -> Unit)? = null

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
        startPrepare(context, autoShowActivity = null, showFailureToast = false)
    }

    /**
     * 预加载广告，加载完成后立即展示（适合点击按钮直接看广告的场景）
     */
    fun prepareLoadAndShow(context: Context, activity: Activity, workId: String?, storyId: String?) {
        if (isAdLoading.get() || isAdReady.get()) return
        currentWorkId = workId
        currentStoryId = storyId
        startPrepare(context, autoShowActivity = activity, showFailureToast = true)
    }

    /**
     * 展示已预加载好的广告（与 [prepareAndLoad] 搭配使用）
     */
    fun showAd(activity: Activity) {
        if (!isAdReady.get()) return
        platform.show(
            activity = activity,
            onRewarded = { completeAdResult() },
            onDismissed = { isAdReady.set(false) },
            onFailed = {
                isAdReady.set(false)
                showAdNotReadyToast()
            }
        )
    }

    /**
     * 加载并立即展示广告，nonce 由外部提供（调用方已完成 prepare 步骤）。
     * nonce 会写入 AdMob SSV customData。广告看完并关闭广告页后回调 [onRewarded]，
     * 后续 complete 结果查询逻辑由调用方自行处理。
     *
     * @param onLoaded   广告加载成功、即将展示时回调（可用于隐藏 loading）
     * @param onRewarded 用户看完广告并关闭广告页后回调
     * @param onFailed   加载或展示失败时回调
     */
    fun loadAndShow(
        context: Context,
        activity: Activity,
        nonce: String,
        adUnitId: String? = null,
        onLoaded: (() -> Unit)? = null,
        onRewarded: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        if (isAdLoading.get()) return
        isAdLoading.set(true)
        platform.load(
            context = context,
            adUnitId = adUnitId,
            customData = nonce,
            onLoaded = {
                Logger.d(TAG, "loadAndShow: ad loaded")
                isAdLoading.set(false)
                isAdReady.set(true)
                onAdLoaded?.invoke()
                onLoaded?.invoke()
                platform.show(
                    activity = activity,
                    onRewarded = onRewarded,
                    onDismissed = { isAdReady.set(false) },
                    onFailed = {
                        isAdReady.set(false)
                        showAdNotReadyToast()
                        onFailed?.invoke()
                    }
                )
            },
            onFailed = {
                Logger.d(TAG, "loadAndShow: ad failed to load")
                isAdLoading.set(false)
                onAdLoadFailed?.invoke()
                showAdNotReadyToast()
                onFailed?.invoke()
            }
        )
    }

    /** ViewModel.onCleared() 时调用，释放广告对象 */
    fun release() {
        platform.release()
    }

    // ── 内部流程 ──────────────────────────────────────────────────────────────

    private fun startPrepare(context: Context, autoShowActivity: Activity?, showFailureToast: Boolean) {
        isAdLoading.set(true)
        statusText.set(R.string.episode_unlock_ad_loading.getResString())

        scope.launch {
            val prepareRes = withContext(Dispatchers.IO) {
                api.storyUnlockAdPrepare(WorkBody(currentWorkId, currentStoryId))
            }

            if (!prepareRes.isSuccess || prepareRes.data == null) {
                isAdLoading.set(false)
                statusText.set(null)
                onAdLoadFailed?.invoke()
                if (showFailureToast) showAdNotReadyToast()
                return@launch
            }

            val adNonce = prepareRes.data!!.nonce
            val adUnitId = prepareRes.data!!.adUnitId
            if (adNonce.isNullOrBlank()) {
                isAdLoading.set(false)
                statusText.set(null)
                onAdLoadFailed?.invoke()
                if (showFailureToast) showAdNotReadyToast()
                return@launch
            }
            nonce = adNonce

            withContext(Dispatchers.Main) {
                platform.load(
                    context = context,
                    adUnitId = adUnitId,
                    customData = adNonce,
                    onLoaded = {
                        Logger.d(TAG, "startPrepare: ad loaded")
                        isAdLoading.set(false)
                        isAdReady.set(true)
                        statusText.set(null)
                        onAdLoaded?.invoke()
                        autoShowActivity?.let { showAd(it) }
                    },
                    onFailed = {
                        Logger.d(TAG, "startPrepare: ad failed to load")
                        isAdLoading.set(false)
                        isAdReady.set(false)
                        statusText.set(null)
                        onAdLoadFailed?.invoke()
                        if (showFailureToast) showAdNotReadyToast()
                    }
                )
            }
        }
    }

    private fun completeAdResult() {
        val wId = currentWorkId ?: return
        val sId = currentStoryId ?: return
        val n = nonce ?: return

        isPolling.set(true)
        statusText.set(R.string.episode_unlock_ad_verifying.getResString())

        scope.launch {
            repeat(POLL_MAX_RETRY) { attempt ->
                val res = withContext(Dispatchers.IO) {
                    api.storyUnlockAdComplete(
                        StoryUnlockAdCompleteBody(workId = wId, storyId = sId, nonce = n)
                    )
                }

                if (res.isSuccess) {
                    when (res.data?.adFlowStatus) {
                        1 -> {
                            Logger.d(TAG, "completeAdResult: success")
                            isPolling.set(false)
                            statusText.set(null)
                            onUnlockSuccess?.invoke()
                            return@launch
                        }
                        2 -> {
                            Logger.d(TAG, "completeAdResult: failed")
                            isPolling.set(false)
                            statusText.set(null)
                            onUnlockFailed?.invoke()
                            return@launch
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

    private fun showAdNotReadyToast() {
        R.string.episode_unlock_ad_not_ready.getResString().showToast()
    }
}
