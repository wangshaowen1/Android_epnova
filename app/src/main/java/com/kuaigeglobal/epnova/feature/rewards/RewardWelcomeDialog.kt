package com.kuaigeglobal.epnova.feature.rewards

import android.os.Bundle
import android.view.Gravity
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.ext.onUserSignIn
import com.kuaigeglobal.epnova.core.network.apis.response.LoginTaskData
import com.kuaigeglobal.epnova.databinding.DialogRewardWelcomeBinding
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = 280.dp
                attr.height = 308.dp
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
        dialog?.setCanceledOnTouchOutside(false)
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
                dismissAllowingStateLoss()
            }
        }
    }


}
