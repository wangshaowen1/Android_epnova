package com.kuaigeglobal.epnova.feature.recharge

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.databinding.ActivityMyBalanceBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.startActivity

class MyBalanceActivity : BaseViewModelActivity<ActivityMyBalanceBinding, MyBalanceViewModel>() {
    companion object {
        fun start() {
            topActivity?.startActivity(MyBalanceActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventManager.onAppTrack(AppEvent.SF_VIEW_RECHARGE_PAGE)
        BillingManager.markActivePage(true)
        termsOfService()
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
    }

    override fun createViewModel(): MyBalanceViewModel
    {
        return MyBalanceViewModel(R.layout.item_recharge_payment)
    }

    override fun onDestroy() {
        BillingManager.markActivePage(false)
        super.onDestroy()
    }


    private fun termsOfService() {
        val spannableString =
            SpannableString(getString(R.string.store_coins_tips))
        val userAgreement = getString(R.string.app_user_agreement)
        val userAgreementStartIndex = spannableString.indexOf(userAgreement)
        if (userAgreementStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            this@MyBalanceActivity,
                            Config.get().serviceUrl,
                            userAgreement
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(
                            this@MyBalanceActivity,
                            R.color.text_color_3_dark
                        )
                        ds.isFakeBoldText = true
                    }
                },
                userAgreementStartIndex,
                userAgreementStartIndex + userAgreement.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val privacyPolicy = getString(R.string.app_privacy_policy)
        val privacyPolicyStartIndex = spannableString.indexOf(privacyPolicy)
        if (privacyPolicyStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            this@MyBalanceActivity,
                            Config.get().privacyUrl,
                            privacyPolicy
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(
                            this@MyBalanceActivity,
                            R.color.text_color_3_dark
                        )
                        ds.isFakeBoldText = true
                    }
                },
                privacyPolicyStartIndex,
                privacyPolicyStartIndex + privacyPolicy.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.tvTip.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTip.highlightColor = Color.TRANSPARENT
        binding.tvTip.text = spannableString
    }
}