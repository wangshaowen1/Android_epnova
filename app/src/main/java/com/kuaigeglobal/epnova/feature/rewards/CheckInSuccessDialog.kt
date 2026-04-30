package com.kuaigeglobal.epnova.feature.rewards

import android.os.Bundle
import com.kuaigeglobal.epnova.databinding.DialogCheckInSuccessBinding
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener

class CheckInSuccessDialog : BaseDialogFragment<DialogCheckInSuccessBinding>() {

    companion object {
        private const val ARG_REWARD_NUM = "rewardNum"

        fun newInstance(rewardNum: Int): CheckInSuccessDialog {
            return CheckInSuccessDialog().apply {
                arguments = Bundle().apply { putInt(ARG_REWARD_NUM, rewardNum) }
            }
        }
    }

    /** 用户点击"看广告领额外奖励"按钮时回调；关闭弹窗时不会触发 */
    var onWatchAdClicked: (() -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        val rewardNum = arguments?.getInt(ARG_REWARD_NUM, 0) ?: 0
        binding.tvCoinsAmount.text = "+$rewardNum"

        binding.btnClaim.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onWatchAdClicked?.invoke()
        }

        binding.ivClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}
