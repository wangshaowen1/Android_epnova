package com.kuaigeglobal.epnova.feature.recharge

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showConfirmDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.ActivityBalanceCoinBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import kotlinx.coroutines.launch

/** 金币商城
 * @createtime 2026年08月19日 09:22
 * @author TooWhiteT
 */

class CoinBalanceActivity : BaseViewModelActivity<ActivityBalanceCoinBinding, MyBalanceViewModel>() {

    companion object {
        fun start() {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, CoinBalanceActivity::class.java).apply {
                    putExtra(Constants.Extra.TAB, MyBalanceViewModel.TAB_COINS)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // store_show：商店页面展示时上报。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.STORE,
            FirebaseEvent.Action.STORE_SHOW
        )
        initTitleBar()
        initRecyclerView()
        setupTip(mRealVM.currentTab)
        observePurchaseEvent()
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
    }

    override fun createViewModel(): MyBalanceViewModel {
        return MyBalanceViewModel(MyBalanceViewModel.TAB_COINS)
    }

    private fun initTitleBar() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvHistory.setOnClickListener { mRealVM.navigateToHistory() }
    }

    private fun initRecyclerView() {
        binding.rvRechargeAmount.adapter = if (mRealVM.currentTab == MyBalanceViewModel.TAB_CREDITS) {
            mRealVM.creditsAdapter
        } else {
            mRealVM.coinsAdapter
        }
    }

    private fun observePurchaseEvent() {
        lifecycleScope.launch {
            mRealVM.purchaseEvent.collect { result ->
                if (result == PurchaseResult.Failed.BenefitFailed) {
                    showGooglePaymentExceptionDialog()
                }
            }
        }
    }

    private fun setupTip(tabIndex: Int) {
        val tipResId = if (tabIndex == MyBalanceViewModel.TAB_COINS) {
            R.string.store_tips_content_coins
        } else {
            R.string.store_tips_content_credits
        }
        val rawText = getString(tipResId)
        val spannableString = SpannableString(rawText)

        applyClickableSpan(spannableString, getString(R.string.app_user_agreement)) {
            SimpleBrowserActivity.launch(this, Config.get().serviceUrl, getString(R.string.app_user_agreement))
        }
        applyClickableSpan(spannableString, getString(R.string.app_privacy_policy)) {
            SimpleBrowserActivity.launch(this, Config.get().privacyUrl, getString(R.string.app_privacy_policy))
        }

        binding.tvTip.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTip.highlightColor = Color.TRANSPARENT
        binding.tvTip.text = spannableString
    }

    private fun applyClickableSpan(spannable: SpannableString, target: String, onClick: () -> Unit) {
        val startIndex = spannable.indexOf(target)
        if (startIndex == -1) return
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@CoinBalanceActivity, R.color.text_color_3_dark)
                    ds.isFakeBoldText = true
                }
            },
            startIndex,
            startIndex + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tvBalance -> {
                //Coins/Credits右侧提示符号
                val isCoins = mRealVM.currentTab == MyBalanceViewModel.TAB_COINS
                showConfirmDialog(
                    title = null,
                    tips = getString(if (isCoins) R.string.coins_popup_instructions else R.string.credits_popup_instructions),
                    positiveButtonText = getString(R.string.confirm_ok)
                )
            }
        }
    }
}