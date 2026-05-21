package com.kuaigeglobal.epnova.feature.recharge

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, MyBalanceViewModel>() {

    var onDismissCallback: ((Boolean) -> Unit)? = null
    var onRechargeSuccessCallback: (() -> Unit)? = null

    companion object {
        const val TAB_COINS = MyBalanceViewModel.TAB_COINS
        const val TAB_CREDITS = MyBalanceViewModel.TAB_CREDITS

        /**
         * 检查是否余额不足，并弹出充值弹窗
         */
        fun checkLowBalance(cost: Int, rechargeTab: Int = MyBalanceViewModel.TAB_COINS): Boolean {
            if (cost > User.get().coins) {
                ioScope.launch(Dispatchers.Main) {
                    showRechargeDialog(true, rechargeTab)
                }
//                show(true)
                return true
            }
            return false
        }

        fun show(
            thisEpisode: Int? = null,
            showToast: Boolean = false,
            toastMessage: String? = null,
            rechargeTab: Int = MyBalanceViewModel.TAB_COINS,
            onRechargeSuccess: (() -> Unit)? = null,
            onDismiss: ((Boolean) -> Unit)? = null
        ) {
            (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
                if (it.fragments.any { fragment -> fragment is PowerRechargeDialog }) return
                PowerRechargeDialog().apply {
                    arguments = Bundle().apply {
                        putInt(Constants.Extra.COST, thisEpisode ?: 0)
                        putInt(Constants.Extra.TAB, rechargeTab)
                    }
                    onDismissCallback = onDismiss
                    onRechargeSuccessCallback = onRechargeSuccess
                }.show(it)
                if (showToast) {
                    val defaultToast = if (rechargeTab == TAB_CREDITS) {
                        R.string.insufficient_balance_please_recharge
                    } else {
                        R.string.episode_unlock_popup_coin_insufficient
                    }
                    toastMessage?.showToast() ?: showToast(defaultToast)
                }
            }
        }
    }

    private var isPaid = false

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke(isPaid)
    }

    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.isDialog = true
        initRechargeUi()
        // 支付结果：通过 Flow 观察，生命周期安全
        viewLifecycleOwner.lifecycleScope.launch {
            mRealVM.purchaseEvent.collect { result ->
                when (result) {
                    is PurchaseResult.Success -> {
                        isPaid = true
                        onDismissCallback = null
                        dismissAllowingStateLoss()
                        onRechargeSuccessCallback?.invoke()
                    }

                    PurchaseResult.Failed.BenefitFailed -> activity?.showGooglePaymentExceptionDialog()
                    else -> Unit
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
        val rechargeTab = arguments?.getInt(
            Constants.Extra.TAB,
            MyBalanceViewModel.TAB_COINS
        ) ?: MyBalanceViewModel.TAB_COINS
        return MyBalanceViewModel(
            paymentItemLayout = R.layout.item_recharge_payment_dialog,
            initialTab = rechargeTab
        )
    }

    private fun initRechargeUi() {
        val isCredits = mRealVM.currentTab == MyBalanceViewModel.TAB_CREDITS
        val iconRes = if (isCredits) R.drawable.icon_power else R.drawable.ic_coins
        binding.tvTitle.setText(if (isCredits) R.string.recharge_credits else R.string.episode_unlock_popup_topup)
        binding.ivTopBg.setBackgroundResource(
            if (isCredits) R.drawable.bg_credits_recharge_top else R.drawable.bg_coin_recharge_top
        )
        binding.ivCostIcon.setImageResource(iconRes)
        binding.ivBalanceIcon.setImageResource(iconRes)
        binding.rvRechargeAmount.adapter = if (isCredits) mRealVM.creditsAdapter else mRealVM.coinsAdapter
        binding.tvPolicy.setText(if (isCredits) R.string.store_credits_tips else R.string.episode_unlock_popup_coin_rule)
    }

}
