package com.kuaigeglobal.epnova.feature.rewards

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.databinding.FragmentRewardsBinding
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RewardsFragment : BaseViewModelFragment<FragmentRewardsBinding, RewardsViewModel>() {

    companion object {
        fun newInstance() = RewardsFragment()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        observeEvents()
        mRealVM.loadData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) mRealVM.loadData()
    }

    private fun initView() {
        binding.rvCheckIn.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCheckIn.addItemDecoration(GridSpacingItemDecoration(7, 2.dp, 0))
        binding.rvCheckIn.adapter = mRealVM.checkInAdapter

        binding.rvEarnRewards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEarnRewards.adapter = mRealVM.earnAdapter
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            mRealVM.showAdEvent.collectLatest { ctx ->
                val activity = activity ?: return@collectLatest
                showLoadingDialog()
                mRealVM.adManager.loadAndShow(
                    context = requireContext(),
                    activity = activity,
                    customData = ctx.customData,
                    onLoaded = { hideLoadingDialog() },
                    onRewarded = {
                        if (ctx.vm != null) mRealVM.onAdWatched(ctx.vm, ctx.nonce)
                        else mRealVM.onCheckInAdWatched(ctx.nonce)
                    },
                    onFailed = { hideLoadingDialog() }
                )
            }
        }
        lifecycleScope.launch {
            mRealVM.showCheckInSuccessEvent.collectLatest { (rewardNum, adRewardCoins) ->
                showCheckInSuccessDialog(rewardNum, adRewardCoins)
            }
        }
        lifecycleScope.launch {
            mRealVM.showCheckInAdRewardEvent.collectLatest { coins ->
                showRewardToast(getString(R.string.rewards_50_reward_coins, coins.toString()))
            }
        }
        observeEvents {
            onUserStatusChanged { mRealVM.loadData() }
        }
    }

    // ─── 签到成功弹窗 ─────────────────────────────────────────────────────────

    private fun showCheckInSuccessDialog(rewardNum: Int, adRewardCoins: Int) {
        val dialog = CheckInSuccessDialog.newInstance(rewardNum, adRewardCoins)
        dialog.onWatchAdClicked = { mRealVM.preReportCheckInAd() }
        dialog.show(childFragmentManager)
    }

    // ─── 获得金币 Toast ───────────────────────────────────────────────────────

    private fun showRewardToast(message: String) {
        val activity = activity ?: return
        val decorView = activity.window.decorView as ViewGroup
        val toastView = LayoutInflater.from(activity)
            .inflate(R.layout.toast_reward_coins, decorView, false)
        toastView.findViewById<TextView>(R.id.tvCoinsAmount).text = message

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        toastView.scaleX = 0.5f
        toastView.scaleY = 0.5f
        toastView.alpha = 0f
        decorView.addView(toastView, params)
        toastView.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(250)
            .start()

        toastView.postDelayed({
            toastView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { decorView.removeView(toastView) }
                .start()
        }, 2000)
    }

    // ─── 点击事件 ─────────────────────────────────────────────────────────────

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnCheckIn      -> mRealVM.doCheckIn()
            R.id.btnCheckInAd    -> mRealVM.preReportCheckInAd()
            R.id.btnRules        -> RewardRulesDialog.newInstance().show(childFragmentManager)
        }
    }
}
