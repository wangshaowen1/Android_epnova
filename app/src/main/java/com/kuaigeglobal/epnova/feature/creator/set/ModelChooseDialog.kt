package com.kuaigeglobal.epnova.feature.creator.set

import androidx.core.os.bundleOf
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.CreationModelInfo
import com.kuaigeglobal.epnova.databinding.DialogModelChooseBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.toArrayList

class ModelChooseDialog : BaseBottomPushVMDialogFragment<DialogModelChooseBinding, ModelChooseViewModel>() {

    companion object {
        fun newInstance(selectedCode: String? = null, list: List<CreationModelInfo>?): ModelChooseDialog {
            val fragment = ModelChooseDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.ID to selectedCode,
                Constants.Extra.DATA to list?.toArrayList()
            )
            return fragment
        }
    }

    var onSuccess: (CreationModelInfo?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        binding.close.setOnClickListener { dismissAllowingStateLoss() }
        mRealVM.onModelSelected = {
            onSuccess.invoke(it)
            dismissAllowingStateLoss()
        }
    }
}
