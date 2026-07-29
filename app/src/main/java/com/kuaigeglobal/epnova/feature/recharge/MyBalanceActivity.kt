package com.kuaigeglobal.epnova.feature.recharge

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showConfirmDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.databinding.ActivityMyBalanceBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ScaleTransitionPagerTitleView
import kotlinx.coroutines.launch

class MyBalanceActivity : BaseViewModelActivity<ActivityMyBalanceBinding, MyBalanceViewModel>() {
    companion object {
        fun start(tab: Int = MyBalanceViewModel.TAB_COINS) {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, MyBalanceActivity::class.java).apply {
                    putExtra(Constants.Extra.TAB, tab)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTitleBar()
        initRecyclerView()
        updateTabUi(mRealVM.currentTab)
        observePurchaseEvent()
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
    }

    override fun createViewModel(): MyBalanceViewModel {
        val initialTab = intent.getIntExtra(Constants.Extra.TAB, MyBalanceViewModel.TAB_COINS)
            .takeIf { it == MyBalanceViewModel.TAB_COINS || it == MyBalanceViewModel.TAB_CREDITS }
            ?: MyBalanceViewModel.TAB_COINS
        return MyBalanceViewModel(R.layout.item_recharge_payment, initialTab)
    }

    private fun initTitleBar() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvHistory.setOnClickListener { mRealVM.navigateToHistory() }
        initMagicIndicator()
    }

    private fun initMagicIndicator() {
        val tabs = listOf(getString(R.string.store_coins), getString(R.string.store_credits))
        val navigator = CommonNavigator(this)
        navigator.isAdjustMode = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount() = tabs.size

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = ScaleTransitionPagerTitleView(context)
                titleView.text = tabs[index]
                titleView.normalColor = R.color.text_color_3_dark.getResColor()
                titleView.selectedColor = R.color.text_color_1_dark.getResColor()
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                titleView.setPadding(20.dp, 0, 20.dp, 0)
                titleView.setIsSelectedBold(true)
                titleView.minScale = 0.8888f
                titleView.setOnClickListener { switchTab(index) }
                return titleView
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                return CommonNavigatorUtils.getLinePagerIndicatorGradient(context)!!.also { indicator ->
                    indicator.setColors(R.color.text_color_1_dark.getResColor())
                    indicator.lineWidth = 16.dp.toFloat()
                    indicator.yOffset = 1.dp.toFloat()
                }
            }
        }
        binding.magicIndicator.navigator = navigator
        binding.magicIndicator.onPageSelected(mRealVM.currentTab)
        binding.magicIndicator.onPageScrolled(mRealVM.currentTab, 0f, 0)
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

    private fun switchTab(index: Int) {
        if (mRealVM.currentTab == index) return
        binding.magicIndicator.onPageSelected(index)
        binding.magicIndicator.onPageScrolled(index, 0f, 0)
        mRealVM.onTabSelected(index)
        updateTabUi(index)
    }

    private fun updateTabUi(index: Int) {
        when (index) {
            MyBalanceViewModel.TAB_COINS -> {
                binding.ivBg.setBackgroundResource(R.drawable.bg_store_coins)
                binding.ivBalanceIcon.setImageResource(R.drawable.ic_coins)
                binding.rvRechargeAmount.adapter = mRealVM.coinsAdapter
                setupTip(index)
            }

            MyBalanceViewModel.TAB_CREDITS -> {
                binding.ivBg.setBackgroundResource(R.drawable.bg_store_credits)
                binding.ivBalanceIcon.setImageResource(R.drawable.icon_power)
                binding.rvRechargeAmount.adapter = mRealVM.creditsAdapter
                setupTip(index)
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
                    ds.color = ContextCompat.getColor(this@MyBalanceActivity, R.color.text_color_3_dark)
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
