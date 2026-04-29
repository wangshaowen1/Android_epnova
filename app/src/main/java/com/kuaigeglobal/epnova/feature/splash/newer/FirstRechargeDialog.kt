package com.kuaigeglobal.epnova.feature.splash.newer

import android.view.Gravity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserSignIn
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeDialogData
import com.kuaigeglobal.epnova.databinding.DialogFirstRechargeBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp


class FirstRechargeDialog : BaseVMDialogFragment<DialogFirstRechargeBinding, FirstRechargeViewModel>() {


    companion object {

        const val SCENE_HOME = 0
        const val SCENE_OTHER = 1

        fun show(scene: Int = SCENE_OTHER, firstRechargeDialogData: FirstRechargeDialogData?): FirstRechargeDialog? {
            if (firstRechargeDialogData?.isNeedShow() == false) return null
            val activity = ActivityUtils.getTopActivity() as? FragmentActivity ?: return null
            val dialog = FirstRechargeDialog().apply {
                arguments = bundleOf(Constants.Extra.DATA to firstRechargeDialogData, Constants.Extra.FROM_PAGE to scene)
            }
            dialog.show(activity.supportFragmentManager)
            return dialog
        }
    }

    var onClose: (() -> Unit)? = null
    var onPaySuccess: (() -> Unit)? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = 293f.dp
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        BillingManager.markActivePage(true)
        val googleProductId = mRealVM.data?.googleProductId
        BillingManager.loadNewerGiftPrice(googleProductId) { map ->
            binding.btnBuy.text = map[googleProductId]?.first
        }
        binding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        // 支付成功：关闭弹窗并通知外部
        mRealVM.onPaySuccess = {
            dismissAllowingStateLoss()
            onPaySuccess?.invoke()
        }
        // 倒计时归零：自动关闭弹窗（用户此时若在看仍可完成支付，但 VM 已停止计时）
        mRealVM.countdownFinished.observe(viewLifecycleOwner) { finished ->
            if (finished) dismissAllowingStateLoss()
        }
        observeEvents {
            onUserSignIn {
                if (User.get().hasPaid()) {
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    override fun onDestroy() {
        BillingManager.markActivePage(false)
        super.onDestroy()
    }
}