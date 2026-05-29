package com.kuaigeglobal.epnova.core.manager.ad

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kuaigeglobal.epnova.BuildConfig
import com.nhsd.basic.utils.Logger
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.metadata.PlayerMetaData

/**
 * @Introduce: Unity Ads 激励广告平台实现
 *
 * 封装所有 Unity Ads SDK 细节，外部仅通过 [IRewardedAdPlatform] 接口交互。
 * 切换广告平台时，本文件保持不动，新建对应的平台实现类即可。
 */
class UnityRewardedAdPlatform : IRewardedAdPlatform {

    companion object {
        private const val TAG = "UnityRewardedAd"
        const val GAME_ID = "6107957"
        private const val AD_UNIT_ID = "Rewarded_Android"
        private const val INIT_TIMEOUT_MS = 20_000L
        private val mainHandler = Handler(Looper.getMainLooper())
        private val initCallbacks = mutableListOf<Pair<() -> Unit, () -> Unit>>()
        private var isInitializing = false
        private var initTimeoutRunnable: Runnable? = null

        fun initialize(context: Context) {
            ensureInitialized(context.applicationContext, onSuccess = {}, onFailed = {})
        }

        private fun ensureInitialized(
            context: Context,
            onSuccess: () -> Unit,
            onFailed: () -> Unit
        ) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                mainHandler.post { ensureInitialized(context.applicationContext, onSuccess, onFailed) }
                return
            }

            if (UnityAds.isInitialized) {
                onSuccess()
                return
            }

            initCallbacks.add(onSuccess to onFailed)
            if (isInitializing) return

            isInitializing = true
            val testMode = BuildConfig.DEBUG
            UnityAds.debugMode = testMode
            Logger.d(
                TAG,
                "init env, sdkVersion=${UnityAds.version}, " +
                    "isSupported=${UnityAds.isSupported}, isInitialized=${UnityAds.isInitialized}"
            )
            initTimeoutRunnable = Runnable {
                if (!isInitializing) return@Runnable
                Logger.d(TAG, "init timeout after ${INIT_TIMEOUT_MS}ms, gameId=$GAME_ID, testMode=$testMode")
                isInitializing = false
                initTimeoutRunnable = null
                val callbacks = initCallbacks.toList()
                initCallbacks.clear()
                callbacks.forEach { it.second.invoke() }
            }.also { mainHandler.postDelayed(it, INIT_TIMEOUT_MS) }

            Logger.d(TAG, "init start, gameId=$GAME_ID, testMode=$testMode, package=${context.packageName}")
            UnityAds.initialize(context.applicationContext, GAME_ID, testMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        Logger.d(TAG, "init success")
                        initTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                        initTimeoutRunnable = null
                        isInitializing = false
                        val callbacks = initCallbacks.toList()
                        initCallbacks.clear()
                        callbacks.forEach { it.first.invoke() }
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError,
                        message: String
                    ) {
                        Logger.d(TAG, "init failed: $error - $message")
                        initTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                        initTimeoutRunnable = null
                        isInitializing = false
                        val callbacks = initCallbacks.toList()
                        initCallbacks.clear()
                        callbacks.forEach { it.second.invoke() }
                    }
                }
            )
        }
    }

    private var isLoaded = false
    private var pendingServerId: String? = null

    override fun load(
        context: Context,
        nonce: String?,
        onLoaded: () -> Unit,
        onFailed: () -> Unit
    ) {
        pendingServerId = nonce
        ensureInitialized(
            context = context,
            onSuccess = {
                loadAd(onLoaded, onFailed)
            },
            onFailed = onFailed
        )
    }

    private fun loadAd(onLoaded: () -> Unit, onFailed: () -> Unit) {
        Logger.d(TAG, "load start, adUnitId=$AD_UNIT_ID, testMode=${BuildConfig.DEBUG}, hasServerId=${!pendingServerId.isNullOrBlank()}")
        UnityAds.load(AD_UNIT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Logger.d(TAG, "onUnityAdsAdLoaded: $placementId")
                isLoaded = true
                onLoaded()
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Logger.d(TAG, "onUnityAdsFailedToLoad: placementId=$placementId, error=$error, message=$message")
                isLoaded = false
                onFailed()
            }
        })
    }

    private fun commitServerId(context: Context) {
        val serverId = pendingServerId?.takeIf { it.isNotBlank() } ?: return
        PlayerMetaData(context.applicationContext).apply {
            setServerId(serverId)
            commit()
        }
    }

    override fun show(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (!isLoaded) {
            Logger.d(TAG, "show: ad is not loaded")
            onFailed()
            return
        }
        commitServerId(activity)
        UnityAds.show(activity, AD_UNIT_ID, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                Logger.d(TAG, "onUnityAdsShowFailure: $error - $message")
                isLoaded = false
                onFailed()
            }

            override fun onUnityAdsShowStart(placementId: String) {
                Logger.d(TAG, "onUnityAdsShowStart")
            }

            override fun onUnityAdsShowClick(placementId: String) {}

            override fun onUnityAdsShowComplete(
                placementId: String,
                state: UnityAds.UnityAdsShowCompletionState
            ) {
                Logger.d(TAG, "onUnityAdsShowComplete: $state")
                isLoaded = false
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onRewarded()
                }
                onDismissed()
            }
        })
    }

    override fun release() {
        isLoaded = false
        pendingServerId = null
    }
}
