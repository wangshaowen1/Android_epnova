package com.kuaigeglobal.epnova.feature.recharge

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.showRechargeDialog
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.databinding.DialogRechargePowerBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, MyBalanceViewModel>() {

    var onDismissCallback: (() -> Unit)? = null
    var onRechargeSuccessCallback: (() -> Unit)? = null

    companion object {
        /**
         * 检查是否余额不足，并弹出充值弹窗
         */
        fun checkLowBalance(cost: Int): Boolean {
            if (cost > User.get().coins) {
                ioScope.launch(Dispatchers.Main) {
                    showRechargeDialog(true)
                }
//                show(true)
                return true
            }
            return false
        }

        fun show(
            thisEpisode: Int? = null,
            showToast: Boolean = false,
            onRechargeSuccess: (() -> Unit)? = null,
            onDismiss: (() -> Unit)? = null
        ) {
            (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
                if (it.fragments.any { fragment -> fragment is PowerRechargeDialog }) return
                PowerRechargeDialog().apply {
                    arguments = Bundle().apply {
                        putInt(Constants.Extra.COST, thisEpisode ?: 0)
                    }
                    onDismissCallback = onDismiss
                    onRechargeSuccessCallback = onRechargeSuccess
                }.show(it)
                if (showToast) {
                    showToast(R.string.insufficient_balance_please_recharge)
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.isDialog = true
        // 支付结果：通过 Flow 观察，生命周期安全
        viewLifecycleOwner.lifecycleScope.launch {
            mRealVM.purchaseEvent.collect { result ->
                if (result is PurchaseResult.Success) {
                    onDismissCallback = null
                    dismissAllowingStateLoss()
                    onRechargeSuccessCallback?.invoke()
                }
            }
        }
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
        EventManager.onAppTrack(AppEvent.SF_SHOW_COMPUTE_RECHARGE_PAYMENT_POPUP)
    }

    override fun createViewModel(): MyBalanceViewModel {
        return MyBalanceViewModel(R.layout.item_recharge_payment_dialog)
    }

}