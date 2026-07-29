package com.kuaigeglobal.epnova.feature.details

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.ActivityComputingPowerDetailsBinding
import com.kuaigeglobal.epnova.feature.details.child.ComputingPowerDetailsTabFragment
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceViewModel
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:算力明细
 * @Data: 2025/9/10/周三  17:49
 * @Version: 1.0
 * *****************************************
 */
class ComputingPowerDetailsActivity :
    BaseViewModelActivity<ActivityComputingPowerDetailsBinding, ComputingPowerDetailsViewModel>() {

    companion object {
        private const val EXTRA_CURRENT_TAB = "extra_current_tab"
        fun start(activity: Activity?, currentTab: Int = MyBalanceViewModel.TAB_COINS) {
            val activity = activity ?: topActivity
            val intent = Intent(activity, ComputingPowerDetailsActivity::class.java)
            intent.putExtra(EXTRA_CURRENT_TAB, currentTab)
            activity?.startActivity(intent)
        }
    }

    private val currentTab by lazy {
        intent.getIntExtra(EXTRA_CURRENT_TAB, MyBalanceViewModel.TAB_COINS)
    }

    private var viewPagerAdapter: FragmentStateCacheAdapter? = null

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        // transaction_history_show：交易历史记录明细页展示时上报。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.STORE,
            FirebaseEvent.Action.TRANSACTION_HISTORY_SHOW
        )
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        val tabs = listOf(getString(R.string.store_coins), getString(R.string.store_credits))
        val navigator = CommonNavigator(this)
        navigator.isAdjustMode = true
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount() = tabs.size

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = SimplePagerTitleView(context)
                titleView.setIsSelectedBold(true)
//                titleView.setPadding(67.dp, 0, 67.dp, 0)
                titleView.setLines(1)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                titleView.gravity = Gravity.CENTER
                titleView.text = tabs[index]
                titleView.normalColor = R.color.text_color_3_dark.getResColor()
                titleView.selectedColor = R.color.text_color_1_dark.getResColor()
                titleView.setOnClickListener {
                    binding.viewPager.currentItem = index
                }
                return titleView
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                return CommonNavigatorUtils.getLinePagerIndicatorGradient(context)!!
                    .also { indicator ->
                        indicator?.setColors(R.color.white.getResColor())
                        indicator?.lineWidth = (11).dp.toFloat()
                        indicator?.lineHeight = (2).dp.toFloat()
                        indicator?.mode = LinePagerIndicator.MODE_EXACTLY
                        indicator?.yOffset = (1).dp.toFloat()
                    }
            }
        }
        binding.magicIndicator.navigator = navigator
        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(
            ComputingPowerDetailsTabFragment.newInstance(
                ComputingPowerDetailsTabFragment.ASSET_TYPE_COINS
            )
        )
        fragmentList.add(
            ComputingPowerDetailsTabFragment.newInstance(
                ComputingPowerDetailsTabFragment.ASSET_TYPE_CREDITS
            )
        )
        viewPagerAdapter = FragmentStateCacheAdapter(
            supportFragmentManager,
            lifecycle
        ).apply {
            setFragments(fragmentList)
        }
        binding.viewPager.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager)

        binding.viewPager.setCurrentItem(currentTab, false)
        binding.magicIndicator.onPageSelected(currentTab)
        binding.magicIndicator.onPageScrolled(currentTab, 0f, 0)
        binding.viewPager.removeTheRipples()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                resetChildFilterToAll(position)
            }
        })
    }

    private fun resetChildFilterToAll(position: Int) {
        viewPagerAdapter?.getFragment(position)?.let { it as? ComputingPowerDetailsTabFragment }
            ?.resetToAll()
    }

    private fun dataUpdate() {

    }
}