package com.kuaigeglobal.epnova.feature.home

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.FragmentHomeBinding
import com.kuaigeglobal.epnova.feature.home.search.SearchActivity
import com.kuaigeglobal.epnova.feature.rewards.RewardWelcomeDialog
import com.kuaigeglobal.epnova.feature.rewards.RewardsActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
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
        initViewPager()
        mRealVM.getRewardUserUncheck()
        triggerGuideLoginIfVisible()
    }

    private fun initListener() {
        binding.lsv.setRetryListener { mRealVM.loadBookShelfConfig() }
        binding.lsv.setNoDataButton(getString(R.string.retry)) { mRealVM.loadBookShelfConfig() }
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
        // 已有可展示的页面时，后台刷新失败也继续使用缓存内容。
        if (viewPager.adapter != null) {
            binding.lsv.showContent()
            return
        }
        val pages = Config.get().bookShelfConfigList.mapNotNull { config ->
            val sections = config.bookshelfSectionList.orEmpty()
            val tabName = config.bookshelfTabName.orEmpty()
            val fragment: Fragment = when (config.bookshelfTab) {
                1 -> TrendingFragment.newInstance(sections, tabName)
                2 -> AllDramasFragment.newInstance(sections, tabName)
                else -> return@mapNotNull null
            }
            tabName to fragment
        }
        if (pages.isEmpty()) {
            when (val state = mRealVM.bookShelfConfigLoadState.value) {
                null, LoadState.Loading -> binding.lsv.showLoading()
                is LoadState.Error -> binding.lsv.showRetry(state.error)
                else -> binding.lsv.showEmpty()
            }
            return
        }
        val navigator = CommonNavigatorUtils.getCommonNavigatorVp2(
            context = mActivity,
            data = pages.map { it.first },
            isAdjustMode = true,
            vp = viewPager,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(mActivity)?.also { indicator ->
                indicator.lineWidth = (24).dp.toFloat()
                indicator.mode = LinePagerIndicator.MODE_EXACTLY
                indicator.yOffset = (1).dp.toFloat()
            },
            titleHorizontalPaddingDp = 16
        )
        binding.magicIndicator.navigator = navigator

        val fragments = pages.map { it.second }
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
                pages.getOrNull(position)?.first?.let(::reportTabExposure)
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
        reportTabExposure(pages.first().first)
        viewPager.removeTheRipples()
        binding.lsv.showContent()
    }

    private fun reportTabExposure(tabName: String) {
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.HOME,
            FirebaseEvent.Action.HOME_TAB_SHOW,
            tabName
        )
    }

    private fun dataUpdate() {
        mRealVM.bookShelfConfigLoadState.observe(viewLifecycleOwner) {
            initViewPager()
        }
        observeEvents {
            on<Events.ConfigChangeEvent> { event ->
                if (event.contain(Config.BOOKSHELF_CONFIG_LIST)) {
                    initViewPager()
                }
            }
        }
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
