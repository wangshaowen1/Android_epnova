package com.kuaigeglobal.epnova.core.manager.ad

import android.app.Activity
import android.content.Context
import com.kuaigeglobal.epnova.BuildConfig
import com.nhsd.basic.utils.Logger
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
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
        private const val IS_BIDDING_AD_UNIT = true
        private val initCallbacks = mutableListOf<Pair<() -> Unit, () -> Unit>>()
        private var isInitializing = false

        fun initialize(context: Context) {
            ensureInitialized(context.applicationContext, onSuccess = {}, onFailed = {})
        }

        private fun ensureInitialized(
            context: Context,
            onSuccess: () -> Unit,
            onFailed: () -> Unit
        ) {
            if (UnityAds.isInitialized) {
                onSuccess()
                return
            }

            initCallbacks.add(onSuccess to onFailed)
            if (isInitializing) return

            isInitializing = true
            Logger.d(TAG, "init start, gameId=$GAME_ID, testMode=${BuildConfig.DEBUG}")
            UnityAds.initialize(context.applicationContext, GAME_ID, BuildConfig.DEBUG,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        Logger.d(TAG, "init success")
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
    private var pendingCustomData: String? = null
    private var pendingAdMarkup: String? = null
    private var pendingObjectId: String? = null

    override fun load(
        context: Context,
        customData: String?,
        adMarkup: String?,
        objectId: String?,
        onLoaded: () -> Unit,
        onFailed: () -> Unit
    ) {
        pendingCustomData = customData
        pendingAdMarkup = adMarkup
        pendingObjectId = objectId
        ensureInitialized(
            context = context,
            onSuccess = {
                commitServerId(context)
                loadAd(onLoaded, onFailed)
            },
            onFailed = onFailed
        )
    }

    private fun loadAd(onLoaded: () -> Unit, onFailed: () -> Unit) {
        val adMarkup = pendingAdMarkup?.takeIf { it.isNotBlank() }
        val objectId = pendingObjectId?.takeIf { it.isNotBlank() }
        if (IS_BIDDING_AD_UNIT && adMarkup == null) {
            Logger.d(TAG, "bidding load failed: missing adMarkup for adUnitId=$AD_UNIT_ID")
            isLoaded = false
            onFailed()
            return
        }

        val loadOptions = UnityAdsLoadOptions().apply {
            adMarkup?.let { setAdMarkup(it) }
            objectId?.let { setObjectId(it) }
        }
        Logger.d(
            TAG,
            "load start, adUnitId=$AD_UNIT_ID, bidding=$IS_BIDDING_AD_UNIT, " +
                "hasAdMarkup=${adMarkup != null}, hasObjectId=${objectId != null}, testMode=${BuildConfig.DEBUG}"
        )
        UnityAds.load(AD_UNIT_ID, loadOptions, object : IUnityAdsLoadListener {
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
        val serverId = pendingCustomData?.takeIf { it.isNotBlank() } ?: return
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
        val showOptions = UnityAdsShowOptions().apply {
            pendingObjectId?.takeIf { it.isNotBlank() }?.let { setObjectId(it) }
        }
        UnityAds.show(activity, AD_UNIT_ID, showOptions, object : IUnityAdsShowListener {
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
        pendingCustomData = null
        pendingAdMarkup = null
        pendingObjectId = null
    }
}
