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
        BillingManager.markActivePage(true)
        BillingManager.queryPendingPurchases()
        mRealVM.isDialog = true
        // 充值成功：自动关闭弹窗并触发外部回调（屏蔽 onDismiss 避免双触发）
        mRealVM.onPurchaseSuccess = {
            onDismissCallback = null
            dismissAllowingStateLoss()
            onRechargeSuccessCallback?.invoke()
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

    override fun onDestroy() {
        BillingManager.markActivePage(false)
        super.onDestroy()
    }
}