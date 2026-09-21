package com.kuaigeglobal.epnova.feature.creator.set

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            // 隐藏底部导航栏或手势条
            it.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false) // 设置全屏
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars()) // 隐藏system bars
                windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars()) // 隐藏navigation bars
                // 上滑显示后自动隐藏行为
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.close.setOnClickListener { dismissAllowingStateLoss() }
        mRealVM.onModelSelected = {
            onSuccess.invoke(it)
            dismissAllowingStateLoss()
        }
    }
}
