package com.kuaigeglobal.epnova.feature.recharge

import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.showRechargeDialog
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.databinding.DialogRechargePowerBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, MyBalanceViewModel>() {
    companion object {
        /**
         * 检查是否余额不足，并弹出充值弹窗
         */
        fun checkLowBalance(cost: Int): Boolean {
            if (cost > User.get().computingPower) {
                ioScope.launch(Dispatchers.Main) {
                    showRechargeDialog(true)
                }
//                show(true)
                return true
            }
            return false
        }

        fun show(showToast: Boolean = false) {
            (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
                if (it.fragments.any { fragment -> fragment is PowerRechargeDialog }) return
                PowerRechargeDialog().show(it)
                if (showToast) {
                    showToast(R.string.insufficient_balance_please_recharge)
                }
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        BillingManager.markActivePage(true)
        BillingManager.queryPendingPurchases()
        mRealVM.isDialog = true
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

    override fun onDestroy() {
        BillingManager.markActivePage(false)
        super.onDestroy()
    }
}