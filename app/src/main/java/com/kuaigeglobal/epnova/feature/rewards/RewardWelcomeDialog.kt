package com.kuaigeglobal.epnova.feature.rewards

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.ext.onUserSignIn
import com.kuaigeglobal.epnova.core.network.apis.response.LoginTaskData
import com.kuaigeglobal.epnova.databinding.DialogRewardWelcomeBinding
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.setOnSingleClickListener


class RewardWelcomeDialog : BaseDialogFragment<DialogRewardWelcomeBinding>() {

    companion object {
        fun newInstance(data: LoginTaskData?): RewardWelcomeDialog {
            val fragment = RewardWelcomeDialog()
            fragment.arguments = Bundle().apply {
                putParcelable(Constants.Extra.DATA, data)
            }
            return fragment
        }
    }

    val data by lazy {
        arguments?.getParcelableValue(Constants.Extra.DATA, LoginTaskData::class.java)
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvCoinsAmount.text = getString(R.string.home_login_reward_coins, data?.rewardLabel)
        binding.btnClaim.setOnSingleClickListener {
            AppNavigator.checkRealLogin(false)
        }

        binding.ivClose.setOnClickListener {
            dismissAllowingStateLoss()
        }

        observeEvents {
            onUserSignIn {
                showRewardToast()
                dismissAllowingStateLoss()
            }
        }
    }

    private fun showRewardToast() {
        val activity = activity ?: return
        val decorView = activity.window.decorView as ViewGroup

        val toastView = LayoutInflater.from(activity)
            .inflate(R.layout.toast_reward_coins, decorView, false)
        toastView.findViewById<TextView>(R.id.tvCoinsAmount).text =
            getString(R.string.home_login_reward_coins, data?.rewardLabel)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        // 进场：缩放 + 淡入
        toastView.scaleX = 0.5f
        toastView.scaleY = 0.5f
        toastView.alpha = 0f
        decorView.addView(toastView, params)
        toastView.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(250)
            .start()

        // 2s 后淡出并移除
        toastView.postDelayed({
            toastView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { decorView.removeView(toastView) }
                .start()
        }, 2000)
    }
}