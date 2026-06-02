package com.kuaigeglobal.epnova.feature.details.child

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.databinding.FragmentComputingPowerDetailsTabBinding
import com.kuaigeglobal.epnova.feature.details.ComputingPowerDetailsFragment
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView

class ComputingPowerDetailsTabFragment :
    BaseViewModelFragment<FragmentComputingPowerDetailsTabBinding, ComputingPowerDetailsTabViewModel>() {

    companion object {
        private const val KEY_ASSET_TYPE = "assetType"

        const val ASSET_TYPE_COINS = 0
        const val ASSET_TYPE_CREDITS = 1

        fun newInstance(assetType: Int): ComputingPowerDetailsTabFragment {
            return ComputingPowerDetailsTabFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_ASSET_TYPE, assetType)
                }
            }
        }
    }

    private val assetType by lazy {
        arguments?.getInt(KEY_ASSET_TYPE, ASSET_TYPE_COINS) ?: ASSET_TYPE_COINS
    }

    private var viewPagerAdapter: FragmentStateCacheAdapter? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
    }

    private fun initView() {
        val tabs = listOf(
            getString(R.string.transaction_history_all),
            getString(R.string.transaction_received),
            getString(R.string.transaction_history_spent)
        )
        val navigator = CommonNavigator(mActivity)
        navigator.isAdjustMode = false
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount() = tabs.size

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = SimplePagerTitleView(context)
                titleView.setIsSelectedBold(true)
                titleView.setPadding(16.dp, 0, 16.dp, 0)
                titleView.setLines(1)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
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
                return CommonNavigatorUtils.getWrapPagerIndicator(context = mActivity)!!
            }
        }
        binding.magicIndicator.navigator = navigator
        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(
            ComputingPowerDetailsFragment.newInstance(
                typeProtagonist = ComputingPowerDetailsFragment.TYPE_ALL,
                assetType = assetType
            )
        )
        fragmentList.add(
            ComputingPowerDetailsFragment.newInstance(
                typeProtagonist = ComputingPowerDetailsFragment.TYPE_RECEIVED,
                assetType = assetType
            )
        )
        fragmentList.add(
            ComputingPowerDetailsFragment.newInstance(
                typeProtagonist = ComputingPowerDetailsFragment.TYPE_SPENT,
                assetType = assetType
            )
        )
        viewPagerAdapter = FragmentStateCacheAdapter(childFragmentManager, lifecycle).apply {
            setFragments(fragmentList)
        }
        binding.viewPager.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager)

        binding.viewPager.setCurrentItem(ComputingPowerDetailsFragment.TYPE_ALL, false)
        binding.magicIndicator.onPageSelected(ComputingPowerDetailsFragment.TYPE_ALL)
        binding.magicIndicator.onPageScrolled(ComputingPowerDetailsFragment.TYPE_ALL, 0f, 0)

        binding.viewPager.removeTheRipples()
    }

    //重置到All
    fun resetToAll() {
        if (view == null) return
        binding.viewPager.setCurrentItem(ComputingPowerDetailsFragment.TYPE_ALL, false)
        binding.magicIndicator.onPageSelected(ComputingPowerDetailsFragment.TYPE_ALL)
        binding.magicIndicator.onPageScrolled(ComputingPowerDetailsFragment.TYPE_ALL, 0f, 0)
        binding.viewPager.post {
            scrollAllListToTop()
        }
    }

    /**
     * 滚动All-Tab项列表至顶部
     */
    private fun scrollAllListToTop() {
        (viewPagerAdapter?.getFragment(ComputingPowerDetailsFragment.TYPE_ALL) as? ComputingPowerDetailsFragment)
            ?.scrollToTop()
    }
}
