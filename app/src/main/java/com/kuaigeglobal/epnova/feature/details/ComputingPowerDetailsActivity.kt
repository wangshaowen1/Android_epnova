package com.kuaigeglobal.epnova.feature.details

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.databinding.ActivityComputingPowerDetailsBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator

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
        fun start(activity: Activity?) {
            val activity = activity ?: topActivity
            val intent = Intent(activity, ComputingPowerDetailsActivity::class.java)
            activity?.startActivity(intent)
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.magicIndicator.navigator = CommonNavigatorUtils.getCommonNavigatorVp2(
            this,
            listOf(
                getString(R.string.transaction_history_all),
                getString(R.string.transaction_history_earned),
                getString(R.string.transaction_history_spent)
            ),false,
            binding.viewPager,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(context = this)
                .also { indicator ->
                    indicator?.lineWidth= (20).dp.toFloat()
                    indicator?.mode = LinePagerIndicator.MODE_EXACTLY
                    indicator?.yOffset = (1).dp.toFloat()
                }
        )
        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(ComputingPowerDetailsFragment.Companion.newInstance(typeProtagonist= ComputingPowerDetailsFragment.Companion.TYPE_ALL))
        fragmentList.add(ComputingPowerDetailsFragment.Companion.newInstance(typeProtagonist= ComputingPowerDetailsFragment.Companion.TYPE_RECHARGE))
        fragmentList.add(ComputingPowerDetailsFragment.Companion.newInstance(typeProtagonist= ComputingPowerDetailsFragment.Companion.TYPE_CONSUME))

        val viewPagerAdapter = FragmentStateCacheAdapter(
            supportFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager)

        binding.viewPager.setCurrentItem(0, false)
        binding.viewPager.removeTheRipples()
    }


    private fun dataUpdate() {

    }
}