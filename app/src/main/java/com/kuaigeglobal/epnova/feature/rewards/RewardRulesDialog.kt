package com.kuaigeglobal.epnova.feature.rewards

import com.kuaigeglobal.epnova.databinding.DialogRewardRulesBinding
import com.nhsd.basic.BaseDialogFragment


class RewardRulesDialog : BaseDialogFragment<DialogRewardRulesBinding>() {

    companion object {
        fun newInstance(): RewardRulesDialog {
            val fragment = RewardRulesDialog()
            return fragment
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.btnClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }
}