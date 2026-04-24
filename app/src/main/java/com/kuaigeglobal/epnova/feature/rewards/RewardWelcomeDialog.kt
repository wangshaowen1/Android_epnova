package com.kuaigeglobal.epnova.feature.rewards

import com.kuaigeglobal.epnova.databinding.DialogRewardWelcomeBinding
import com.nhsd.basic.BaseDialogFragment


class RewardWelcomeDialog : BaseDialogFragment<DialogRewardWelcomeBinding>() {

    companion object {
        fun newInstance(): RewardWelcomeDialog {
            val fragment = RewardWelcomeDialog()
            return fragment
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.ivClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}