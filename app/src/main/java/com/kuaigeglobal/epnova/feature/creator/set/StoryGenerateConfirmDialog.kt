package com.kuaigeglobal.epnova.feature.creator.set

import androidx.core.os.bundleOf
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.request.UploadStoryBody
import com.kuaigeglobal.epnova.core.network.apis.request.WorkTaskData
import com.kuaigeglobal.epnova.databinding.DialogStoryGenerateConfirmBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener

class StoryGenerateConfirmDialog : BaseBottomPushVMDialogFragment<DialogStoryGenerateConfirmBinding, StoryGenerateConfirmViewModel>() {

    companion object {
        fun newInstance(
            body: UploadStoryBody?,
            powerConsume: Int?
        ): StoryGenerateConfirmDialog {
            val fragment = StoryGenerateConfirmDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.DATA to body,
                Constants.Extra.COST to powerConsume,
            )
            return fragment
        }
    }

    var onConfirm: (WorkTaskData?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        val powerConsume = arguments?.getInt(Constants.Extra.COST)
        binding.tvPower.text = powerConsume?.toString()
        binding.close.setOnSingleClickListener { dismissAllowingStateLoss() }
        binding.btnConfirm.setOnClickListener {
            mRealVM.confirmPowerConsume { data ->
                Config.get().notifyPowerConsumeClose(0)
                onConfirm.invoke(data)
                dismissAllowingStateLoss()
            }
        }
    }
}