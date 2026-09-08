package com.kuaigeglobal.epnova.feature.creator

import android.view.Gravity
import android.view.View
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.databinding.DialogCreatingEncouragementBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.dp

/** 创作鼓励弹窗
 * @createtime 2026年09月08日 10:59
 * @author TooWhiteT
 */

class CreatingEncouragementDialog : BaseVMDialogFragment<DialogCreatingEncouragementBinding, CreatingEncouragementViewModel>() {

    companion object {
        private const val DESIGN_HEIGHT_DP = 408
        private const val DESIGN_WIDTH_DP = 295

        fun show() {

        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.attributes = window.attributes.apply {
                width = DESIGN_WIDTH_DP.dp
                height = DESIGN_HEIGHT_DP.dp
                gravity = Gravity.CENTER
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.ivClose -> {
                dismissAllowingStateLoss()
            }
            R.id.btnNext -> {
                // 下一步
            }
        }
    }
}