package com.kuaigeglobal.epnova.feature.recharge

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.DialogPowerConsumeConfirmBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 预计消耗算力
 */
class PowerConsumeConfirmDialog : BaseBottomPushVMDialogFragment<DialogPowerConsumeConfirmBinding, PowerConsumeConfirmViewModel>() {

    companion object {
        /**
         * 生成资产图片
         */
        const val SCENE_GENERATE_ASSETS_IMAGE = 3

        /**
         * 生成分镜视频
         */
        const val SCENE_GENERATE_STORYBOARD_VIDEO = 4

        /**
         * 重绘分镜视频
         */
        const val SCENE_REGENERATE_STORYBOARD_VIDEO = 5

        fun show(cost: Int): PowerConsumeConfirmDialog? {
            val manager = (topActivity as? FragmentActivity)?.supportFragmentManager ?: return null
            val fragment = PowerConsumeConfirmDialog()
            fragment.arguments = Bundle().apply {
                putInt(Constants.Extra.COST, cost)
            }
            fragment.show(manager)
            return fragment
        }
    }

    var onConfirm: () -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        binding.btnConfirm.setOnSingleClickListener {
            if (PowerRechargeDialog.checkLowBalance(mRealVM.cost)) return@setOnSingleClickListener
            Config.get().notifyPowerConsumeClose(1)
            onConfirm.invoke()
            dismissAllowingStateLoss()
        }
    }
}