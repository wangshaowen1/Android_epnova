package com.kuaigeglobal.epnova.feature.rewards

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.manager.RewardedAdManager
import com.kuaigeglobal.epnova.databinding.FragmentRewardsBinding
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RewardsFragment : BaseViewModelFragment<FragmentRewardsBinding, RewardsViewModel>() {

    companion object {
        fun newInstance() = RewardsFragment()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        observeShowAdEvent()
        mRealVM.loadData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) mRealVM.loadData()
    }

    private fun initView() {
        // 签到格子：7列 Grid
        binding.rvCheckIn.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCheckIn.addItemDecoration(GridSpacingItemDecoration(7, 2.dp, 0))
        binding.rvCheckIn.adapter = mRealVM.checkInAdapter

        // 任务列表：垂直 Linear
        binding.rvEarnRewards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEarnRewards.adapter = mRealVM.earnAdapter
    }

    private fun observeShowAdEvent() {
        lifecycleScope.launch {
            mRealVM.showAdEvent.collectLatest { ctx ->
                loadAndShowRewardAd(ctx)
            }
        }
    }

    private fun loadAndShowRewardAd(ctx: RewardsViewModel.AdWatchContext) {
        val activity = activity ?: return
        RewardedAd.load(
            requireContext(),
            RewardedAdManager.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    ad.setServerSideVerificationOptions(
                        ServerSideVerificationOptions.Builder()
                            .setCustomData(ctx.customData)
                            .build()
                    )
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            showToast(R.string.episode_unlock_ad_not_ready)
                        }
                    }
                    ad.show(activity, OnUserEarnedRewardListener {
                        mRealVM.onAdWatched(ctx.vm, ctx.nonce)
                    })
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    showToast(R.string.episode_unlock_ad_not_ready)
                }
            }
        )
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnCheckIn -> mRealVM.doCheckIn()
            R.id.btnRules ->
                RewardRulesDialog.newInstance().show(childFragmentManager)
        }
    }
}
