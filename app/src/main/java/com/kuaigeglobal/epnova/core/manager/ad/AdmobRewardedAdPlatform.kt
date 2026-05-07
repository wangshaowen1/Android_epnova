package com.kuaigeglobal.epnova.core.manager.ad

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.kuaigeglobal.epnova.BuildConfig
import com.nhsd.basic.utils.Logger

/**
 * @Introduce: Google AdMob 激励广告平台实现
 *
 * 封装所有 AdMob SDK 细节，外部仅通过 [IRewardedAdPlatform] 接口交互。
 * 切换广告平台时，本文件保持不动，新建对应的平台实现类即可。
 */
class AdmobRewardedAdPlatform : IRewardedAdPlatform {

    companion object {
        private const val TAG = "AdmobRewardedAd"
        // 广告位 ID 属于平台细节，收敛在实现类中
        private val AD_UNIT_ID: String
            get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917"
                    else "ca-app-pub-9071858947673651/4907921363"
    }

    private var rewardedAd: RewardedAd? = null

    override fun load(
        context: Context,
        customData: String?,
        onLoaded: () -> Unit,
        onFailed: () -> Unit
    ) {
        RewardedAd.load(
            context,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Logger.d(TAG, "onAdLoaded")
                    rewardedAd = ad
                    if (!customData.isNullOrEmpty()) {
                        ad.setServerSideVerificationOptions(
                            ServerSideVerificationOptions.Builder()
                                .setCustomData(customData)
                                .build()
                        )
                    }
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Logger.d(TAG, "onAdFailedToLoad: $error")
                    rewardedAd = null
                    onFailed()
                }
            }
        )
    }

    override fun show(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit
    ) {
        val ad = rewardedAd ?: run {
            Logger.d(TAG, "show: ad is null")
            onFailed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Logger.d(TAG, "onAdDismissedFullScreenContent")
                rewardedAd = null
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Logger.d(TAG, "onAdFailedToShowFullScreenContent: $error")
                rewardedAd = null
                onFailed()
            }
        }
        ad.show(activity, OnUserEarnedRewardListener { onRewarded() })
    }

    override fun release() {
        rewardedAd = null
    }
}
