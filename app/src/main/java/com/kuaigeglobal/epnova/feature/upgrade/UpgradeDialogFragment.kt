package com.kuaigeglobal.epnova.feature.upgrade

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.EmptyViewModel
import com.kuaigeglobal.epnova.databinding.DialogUpgradeBinding
import com.nhsd.basic.BaseVMDialogFragment
import kotlin.math.min


class UpgradeDialogFragment : BaseVMDialogFragment<DialogUpgradeBinding, EmptyViewModel>() {


    var onUpgradeClick: (() -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.version.text = arguments?.getString(VERSION_TEXT)
        binding.content.text = arguments?.getString(UPGRADE_DESCRIPTION)
        binding.cancel.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                val screenWidth = min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
                attr.width = (294 / 375f * screenWidth).toInt()
                attr.gravity = Gravity.CENTER
                attr.dimAmount = 0.5f
                dialogWindow.attributes = attr
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.upgrade -> {
                onUpgradeClick?.invoke()
            }

            R.id.cancel, R.id.close -> dismissAllowingStateLoss()
        }
    }

    companion object {

        const val VERSION_TEXT: String = "versionText"
        const val UPGRADE_DESCRIPTION: String = "upgradeDescription"

        fun show(
            manager: FragmentManager,
            versionText: String?,
            upgradeDescription: String?
        ): UpgradeDialogFragment {
            val dialog = UpgradeDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(VERSION_TEXT, versionText)
                    putString(UPGRADE_DESCRIPTION, upgradeDescription)
                }
            }
            dialog.show(manager)
            return dialog
        }
    }
}
