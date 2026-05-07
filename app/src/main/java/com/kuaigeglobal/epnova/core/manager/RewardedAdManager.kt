package com.kuaigeglobal.epnova.core.manager

import android.app.Activity
import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.manager.ad.AdmobRewardedAdPlatform
import com.kuaigeglobal.epnova.core.manager.ad.IRewardedAdPlatform
import com.kuaigeglobal.epnova.core.network.apis.ApiService
import com.kuaigeglobal.epnova.core.network.apis.request.StoryUnlockAdResultBody
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
 *   prepare API → load Ad（配置 SSV）→ show → poll 服务端结果
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
 * // 方式3：外部已完成 preReport，直接加载并展示
 * adManager.loadAndShow(context, activity, customData, onRewarded = { ... })
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
        if (!isAdReady.get()) return
        platform.show(
            activity = activity,
            onRewarded = { pollAdResult() },
            onDismissed = { isAdReady.set(false) },
            onFailed = {
                isAdReady.set(false)
                showAdNotReadyToast()
            }
        )
    }

    /**
     * 加载并立即展示广告，customData 由外部提供（调用方已完成 preReport 步骤）。
     * 广告看完后回调 [onRewarded]，不触发内部轮询——后续上报逻辑由调用方自行处理。
     *
     * @param onLoaded   广告加载成功、即将展示时回调（可用于隐藏 loading）
     * @param onRewarded 用户看完广告获得奖励时回调
     * @param onFailed   加载或展示失败时回调
     */
    fun loadAndShow(
        context: Context,
        activity: Activity,
        customData: String,
        onLoaded: (() -> Unit)? = null,
        onRewarded: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        if (isAdLoading.get()) return
        isAdLoading.set(true)
        platform.load(
            context = context,
            customData = customData,
            onLoaded = {
                Logger.d(TAG, "loadAndShow: ad loaded")
                isAdLoading.set(false)
                isAdReady.set(true)
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
                showAdNotReadyToast()
                return@launch
            }

            val customData = prepareRes.data!!.customData
            nonce = prepareRes.data!!.nonce

            withContext(Dispatchers.Main) {
                platform.load(
                    context = context,
                    customData = customData,
                    onLoaded = {
                        Logger.d(TAG, "startPrepare: ad loaded")
                        isAdLoading.set(false)
                        isAdReady.set(true)
                        statusText.set(null)
                        autoShowActivity?.let { showAd(it) }
                    },
                    onFailed = {
                        Logger.d(TAG, "startPrepare: ad failed to load")
                        isAdLoading.set(false)
                        isAdReady.set(false)
                        statusText.set(null)
                        showAdNotReadyToast()
                    }
                )
            }
        }
    }

    private fun pollAdResult() {
        val wId = currentWorkId ?: return
        val sId = currentStoryId ?: return
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
                            Logger.d(TAG, "pollAdResult: success")
                            isPolling.set(false)
                            statusText.set(null)
                            onUnlockSuccess?.invoke()
                            return@launch
                        }
                        2 -> {
                            Logger.d(TAG, "pollAdResult: failed")
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
