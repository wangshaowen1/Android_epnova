package com.kuaigeglobal.epnova.feature.recharge

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.showRechargeDialog
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.DialogRechargePowerBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, MyBalanceViewModel>() {

    var onDismissCallback: ((Boolean) -> Unit)? = null
    var onRechargeSuccessCallback: (() -> Unit)? = null

    companion object {
        const val TAB_COINS = MyBalanceViewModel.TAB_COINS
        const val TAB_CREDITS = MyBalanceViewModel.TAB_CREDITS
        private const val EXTRA_PAID_EPISODE_ANALYTICS = "extra_paid_episode_analytics"

        /**
         * 检查是否余额不足，并弹出充值弹窗
         */
        fun checkLowBalance(cost: Int, rechargeTab: Int = MyBalanceViewModel.TAB_COINS): Boolean {
            val balance = if (rechargeTab == TAB_CREDITS) User.get().points else User.get().coins
            if (cost.toLong() > balance) {
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
            onDismiss: ((Boolean) -> Unit)? = null,
            paidEpisodeAnalytics: Boolean = false,
        ) {
            (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
                if (it.fragments.any { fragment -> fragment is PowerRechargeDialog }) return
                PowerRechargeDialog().apply {
                    arguments = Bundle().apply {
                        putInt(Constants.Extra.COST, thisEpisode ?: 0)
                        putInt(Constants.Extra.TAB, rechargeTab)
                        putBoolean(EXTRA_PAID_EPISODE_ANALYTICS, paidEpisodeAnalytics)
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
        if (mRealVM.isPaidEpisodeAnalytics) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.PAID_EPISODE,
                FirebaseEvent.Action.BUY_COIN_POPUP_SHOW,
            )
        }
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
    }

    override fun createViewModel(): MyBalanceViewModel {
        val rechargeTab = arguments?.getInt(
            Constants.Extra.TAB,
            MyBalanceViewModel.TAB_COINS
        ) ?: MyBalanceViewModel.TAB_COINS
        return MyBalanceViewModel(
            initialTab = rechargeTab,
            isPaidEpisodeAnalytics = arguments?.getBoolean(
                EXTRA_PAID_EPISODE_ANALYTICS,
                false,
            ) == true,
        )
    }

    private fun initRechargeUi() {
        val isCredits = mRealVM.currentTab == MyBalanceViewModel.TAB_CREDITS
        val iconRes = if (isCredits) R.drawable.icon_power else R.drawable.ic_coins
        binding.ivTopBg.setBackgroundResource(
            if (isCredits) R.drawable.bg_credits_recharge_top else R.drawable.bg_coin_recharge_top
        )
        binding.ivBalanceIcon.setImageResource(iconRes)
        binding.rvRechargeAmount.adapter = if (isCredits) mRealVM.creditsAdapter else mRealVM.coinsAdapter
        binding.tvPolicy.setText(if (isCredits) R.string.store_tips_content_credits else R.string.store_tips_content_coins)
        setupTip(isCredits)
    }

    private fun setupTip(isCredits: Boolean) {
        val tipResId = if (isCredits) {
            R.string.store_tips_content_credits
        } else {
            R.string.store_tips_content_coins
        }
        val rawText = getString(tipResId)
        val spannableString = SpannableString(rawText)

        applyClickableSpan(spannableString, getString(R.string.app_user_agreement)) {
            this.context?.let { context ->
                SimpleBrowserActivity.launch(context, Config.get().serviceUrl, getString(R.string.app_user_agreement))
            }
        }
        applyClickableSpan(spannableString, getString(R.string.app_privacy_policy)) {
            this.context?.let { context ->
                SimpleBrowserActivity.launch(context, Config.get().privacyUrl, getString(R.string.app_privacy_policy))
            }
        }

        binding.tvPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPolicy.highlightColor = Color.TRANSPARENT
        binding.tvPolicy.text = spannableString
    }

    private fun applyClickableSpan(spannable: SpannableString, target: String, onClick: () -> Unit) {
        val startIndex = spannable.indexOf(target)
        if (startIndex == -1) return
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = R.color.text_color_3_dark.getResColor()
                    ds.isFakeBoldText = true
                }
            },
            startIndex,
            startIndex + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

}
