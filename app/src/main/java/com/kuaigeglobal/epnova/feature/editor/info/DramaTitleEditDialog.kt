package com.kuaigeglobal.epnova.feature.editor.info

import android.content.DialogInterface
import androidx.core.os.bundleOf
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.databinding.DialogDramaTitleEditBinding
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.showToast

/**
 * 修改标题
 */
class DramaTitleEditDialog() :
    BaseBottomPushDialogFragment<DialogDramaTitleEditBinding>() {

    companion object {
        @JvmStatic
        fun newInstance(title: String?): DramaTitleEditDialog {
            val dialog = DramaTitleEditDialog()
            dialog.arguments = bundleOf(Constants.Extra.TITLE to title)
            return dialog
        }
    }

    var onSave: ((String) -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        binding.nameInputer.setTextNoSelection(arguments?.getString(Constants.Extra.TITLE))
        binding.close.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnCancel.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnConfirm.setOnClickListener {
            if (binding.nameInputer.text.trim().isEmpty()) {
                getString(R.string.enter_update_content).showToast()
                return@setOnClickListener
            }
            onSave?.invoke(binding.nameInputer.text.trim())
            dismissAllowingStateLoss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        hideSoftInput()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        hideSoftInput()
        super.onDestroy()
    }

    private fun hideSoftInput() {
        ThreadUtils.runOnUiThreadDelayed({ KeyboardUtils.hideSoftInput(ActivityUtils.getTopActivity()) }, 100)
    }
}