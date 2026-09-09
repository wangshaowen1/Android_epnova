package com.kuaigeglobal.epnova.feature.home

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.FragmentHomeBinding
import com.kuaigeglobal.epnova.feature.home.search.SearchActivity
import com.kuaigeglobal.epnova.feature.rewards.RewardWelcomeDialog
import com.kuaigeglobal.epnova.feature.rewards.RewardsActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator

/**
 * 主页容器：固定搜索栏和 Tab，具体内容由两个子 Fragment 负责。
 */
class HomeFragment : BaseViewModelFragment<FragmentHomeBinding, HomeTabsViewModel>() {

    companion object {
        private const val TAG = "HomePagerTouch"

        fun newInstance(): HomeFragment = HomeFragment()
    }

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initViewPager()
        initListener()
        dataUpdate()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        mRealVM.getRewardUserUncheck()
        triggerGuideLoginIfVisible()
    }

    override fun onResume() {
        super.onResume()
        mRealVM.getRewardUserUncheck()
        triggerGuideLoginIfVisible()
    }

    private fun initListener() {
        binding.btnSearch.setOnSingleClickListener {
            SearchActivity.start(mActivity)
        }
        binding.btnRewards.setOnSingleClickListener {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.REWARDS,
                FirebaseEvent.Action.HOME_REWARDS_BTN_CLICK
            )
            RewardsActivity.start(mActivity)
        }
    }

    private fun initViewPager() {
        val viewPager = binding.viewPager
        val navigator = CommonNavigatorUtils.getCommonNavigatorVp2(
            context = mActivity,
            data = listOf(
                getString(R.string.tab_hot),
                getString(R.string.tab_all_dramas)
            ),
            isAdjustMode = true,
            vp = viewPager,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(mActivity)?.also {
                it.lineWidth = 24.dp.toFloat()
                it.lineHeight = 2.dp.toFloat()
                it.mode = LinePagerIndicator.MODE_EXACTLY
            },
            titleHorizontalPaddingDp = 16
        )
        binding.magicIndicator.navigator = navigator

        val fragments = listOf<Fragment>(
            TrendingFragment.newInstance(),
            AllDramasFragment.newInstance()
        )
        viewPager.adapter = FragmentStateCacheAdapter(childFragmentManager, lifecycle).apply {
            setFragments(fragments)
        }
        viewPager.offscreenPageLimit = fragments.size
        ViewPagerHelper.bind(binding.magicIndicator, viewPager)
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                Log.d(
                    TAG,
                    "onPageScrolled position=$position offset=$positionOffset " +
                        "offsetPx=$positionOffsetPixels current=${viewPager.currentItem}"
                )
            }

            override fun onPageSelected(position: Int) {
                Log.d(TAG, "onPageSelected position=$position")
            }

            override fun onPageScrollStateChanged(state: Int) {
                Log.d(
                    TAG,
                    "onPageScrollStateChanged state=${pageScrollStateName(state)} " +
                        "current=${viewPager.currentItem}"
                )
            }
        }.also { callback ->
            viewPager.registerOnPageChangeCallback(callback)
        }
        viewPager.setCurrentItem(0, false)
        binding.magicIndicator.onPageSelected(0)
        binding.magicIndicator.onPageScrolled(0, 0f, 0)
        viewPager.removeTheRipples()

    }

    private fun dataUpdate() {
        mRealVM.loginTaskData.observe(viewLifecycleOwner) {
            val todayDate = com.kuaigeglobal.epnova.common.utils.DateUtil.getDateTime()
            if (Config.get().loginRewardsDialog != todayDate) {
                RewardWelcomeDialog.newInstance(it).show(childFragmentManager)
                Config.get().loginRewardsDialog = todayDate
            }
        }
    }

    private fun triggerGuideLoginIfVisible() {
        if (!isResumed || isHidden || !isVisible) return
        if (User.get().isGuest && mRealVM.loginTaskData.value == null) {
            mRealVM.getLoginTaskData()
        }
    }

    override fun onDestroy() {
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        binding.viewPager.adapter = null
        super.onDestroy()
    }

    private fun pageScrollStateName(state: Int): String {
        return when (state) {
            ViewPager2.SCROLL_STATE_IDLE -> "IDLE"
            ViewPager2.SCROLL_STATE_DRAGGING -> "DRAGGING"
            ViewPager2.SCROLL_STATE_SETTLING -> "SETTLING"
            else -> state.toString()
        }
    }
}
