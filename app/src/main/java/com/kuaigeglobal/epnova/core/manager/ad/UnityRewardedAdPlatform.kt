package com.kuaigeglobal.epnova.core.manager.ad

import android.app.Activity
import android.content.Context
import com.kuaigeglobal.epnova.BuildConfig
import com.nhsd.basic.utils.Logger
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

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
    }

    private var isLoaded = false
    private var pendingCustomData: String? = null

    override fun load(
        context: Context,
        customData: String?,
        onLoaded: () -> Unit,
        onFailed: () -> Unit
    ) {
        pendingCustomData = customData
        if (UnityAds.isInitialized) {
            loadAd(onLoaded, onFailed)
        } else {
            UnityAds.initialize(context, GAME_ID, BuildConfig.DEBUG,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        loadAd(onLoaded, onFailed)
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError,
                        message: String
                    ) {
                        Logger.d(TAG, "init failed: $error - $message")
                        onFailed()
                    }
                }
            )
        }
    }

    private fun loadAd(onLoaded: () -> Unit, onFailed: () -> Unit) {
        UnityAds.load(AD_UNIT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Logger.d(TAG, "onUnityAdsAdLoaded")
                isLoaded = true
                onLoaded()
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Logger.d(TAG, "onUnityAdsFailedToLoad: $error - $message")
                isLoaded = false
                onFailed()
            }
        })
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
        UnityAds.show(activity, AD_UNIT_ID, UnityAdsShowOptions(), object : IUnityAdsShowListener {
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
    }
}
