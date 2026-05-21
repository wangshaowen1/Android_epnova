package com.kuaigeglobal.epnova.feature.splash.newer

import android.view.Gravity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserSignIn
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.network.apis.response.FirstRechargeCreditsDialogData
import com.kuaigeglobal.epnova.databinding.DialogFirstRechargeCreditsBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import kotlinx.coroutines.launch

class FirstRechargeCreditsDialog :
    BaseVMDialogFragment<DialogFirstRechargeCreditsBinding, FirstRechargeCreditsViewModel>() {

    companion object {

        const val SCENE_HOME = FirstRechargeDialog.SCENE_HOME
        const val SCENE_OTHER = FirstRechargeDialog.SCENE_OTHER

        fun show(
            scene: Int = SCENE_OTHER,
            firstRechargeDialogData: FirstRechargeCreditsDialogData?
        ): FirstRechargeCreditsDialog? {
            if (firstRechargeDialogData?.isNeedShow() == false) return null
            val activity = ActivityUtils.getTopActivity() as? FragmentActivity ?: return null
            val dialog = FirstRechargeCreditsDialog().apply {
                arguments = bundleOf(
                    Constants.Extra.DATA to firstRechargeDialogData,
                    Constants.Extra.FROM_PAGE to scene
                )
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
                attr.width = 290f.dp
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initPrice()
        initEvents()
    }

    private fun initPrice() {
        val googleProductId = mRealVM.data?.pointConfigVO?.googleProductId
        BillingManager.loadNewerGiftPrice(googleProductId) { map ->
            viewLifecycleOwner.lifecycleScope.launch {
                mRealVM.updateGooglePrice(map[googleProductId]?.first)
            }
        }
    }

    private fun initEvents() {
        binding.ivClose.setOnSingleClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            mRealVM.purchaseEvent.collect { result ->
                if (result is PurchaseResult.Success) {
                    dismissAllowingStateLoss()
                    onPaySuccess?.invoke()
                }
            }
        }
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
}
