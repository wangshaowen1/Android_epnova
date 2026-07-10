package com.kuaigeglobal.epnova.feature.creator

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import com.kuaigeglobal.epnova.core.network.apis.entity.UnusedPackageInfo
import com.kuaigeglobal.epnova.databinding.DialogShortDramaCreationPackageBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener

class ShortDramaCreationPackageDialog :
    BaseVMDialogFragment<DialogShortDramaCreationPackageBinding, ShortDramaCreationPackageViewModel>() {

    var onCancelClick: (() -> Unit)? = null
    var onUseClick: ((UnusedPackageInfo) -> Unit)? = null
    var onPackageSelected: ((UnusedPackageInfo) -> Unit)? = null

    companion object {
        const val ARG_PACKAGES = "packages"
        const val ARG_SELECTED_PACKAGE_INSTANCE_ID = "selectedPackageInstanceId"
        const val ARG_DISPLAY_SUBTITLE = "displaySubtitle"

        fun newInstance(
            packages: List<UnusedPackageInfo>,
            selectedPackageInstanceId: String? = null,
            displaySubtitle: String? = null
        ) = ShortDramaCreationPackageDialog().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_PACKAGES, ArrayList(packages))
                putString(ARG_SELECTED_PACKAGE_INSTANCE_ID, selectedPackageInstanceId)
                putString(ARG_DISPLAY_SUBTITLE, displaySubtitle)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(true)
        dialog?.window?.let { window ->
            window.attributes = window.attributes.apply {
                width = 310.dp
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        if (!mRealVM.hasPackages()) {
            dismissAllowingStateLoss()
            return
        }
        binding.basicPackageCard.setOnSingleClickListener {
            mRealVM.selectFirstPackage()?.let {
                onPackageSelected?.invoke(it)
                binding.executePendingBindings()
            }
        }
        binding.boutiquePackageCard.setOnSingleClickListener {
            mRealVM.selectSecondPackage()?.let {
                onPackageSelected?.invoke(it)
                binding.executePendingBindings()
            }
        }
        binding.btnCancel.setOnSingleClickListener {
            onCancelClick?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnUse.setOnSingleClickListener {
            mRealVM.selectedPackageInfo()?.let { onUseClick?.invoke(it) }
            dismissAllowingStateLoss()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelClick?.invoke()
    }
}
