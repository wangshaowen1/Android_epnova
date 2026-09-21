package com.kuaigeglobal.epnova.feature.home

import android.util.Log
import android.view.View
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

        /** 创建主页容器，具体 Tab 内容在配置加载后按书架类型生成。 */
        fun newInstance(): HomeFragment = HomeFragment()
    }

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var lastReportedTabPosition: Int? = null

    /** 初始化固定的搜索栏、Tab 容器和主页状态监听。 */
    override fun onPrepareView() {
        super.onPrepareView()
        initViewPager()
        initListener()
        dataUpdate()
    }

    /** Home 从隐藏状态恢复时刷新奖励未读数，并按当前可见性触发登录引导数据请求。 */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        mRealVM.getRewardUserUncheck()
        triggerGuideLoginIfVisible()
    }

    /** 回到前台时复用已有 Tab 页面，同时刷新顶部奖励入口状态。 */
    override fun onResume() {
        super.onResume()
        initViewPager()
        mRealVM.getRewardUserUncheck()
        triggerGuideLoginIfVisible()
    }

    /** 绑定重试、搜索和奖励入口；页面内容请求仍由 ViewModel 和子 Fragment 负责。 */
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

    /** 根据缓存的书架配置创建固定的 Trending/All Dramas 子页并绑定 Tab。 */
    private fun initViewPager() {
        val viewPager = binding.viewPager
        val tabCount = Config.get().bookShelfConfigList.count {
            it.bookshelfTab == 1 || it.bookshelfTab == 2
        }
        binding.magicIndicator.visibility = if (tabCount > 1) View.VISIBLE else View.GONE
        // 已有可展示的页面时，后台刷新失败也继续使用缓存内容。
        if (viewPager.adapter != null) {
            binding.lsv.showContent()
            return
        }
        // 书架配置中的 tab 类型决定子页；未知类型不参与展示。
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
        viewPager.offscreenPageLimit = 2
        ViewPagerHelper.bind(binding.magicIndicator, viewPager)
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Log.d(TAG, "onPageSelected position=$position")
                pages.getOrNull(position)?.first?.let { tabName ->
                    reportTabExposure(position, tabName)
                }
            }
        }.also { callback ->
            viewPager.registerOnPageChangeCallback(callback)
        }
        viewPager.setCurrentItem(0, false)
        binding.magicIndicator.onPageSelected(0)
        binding.magicIndicator.onPageScrolled(0, 0f, 0)
        reportTabExposure(0, pages.first().first)
        viewPager.removeTheRipples()
        binding.lsv.showContent()
    }

    /** 记录当前 Tab 展示，用于区分用户实际看到的主页内容。 */
    private fun reportTabExposure(position: Int, tabName: String) {
        // ViewPager2 初始化回调与显式首 Tab 上报可能同时到达，按位置去重。
        if (lastReportedTabPosition == position) return
        lastReportedTabPosition = position
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.HOME,
            FirebaseEvent.Action.HOME_TAB_SHOW,
            tabName
        )
    }

    /** 监听配置变化、登录奖励弹窗数据和跨模块配置刷新事件。 */
    private fun dataUpdate() {
        mRealVM.bookShelfConfigLoadState.observe(viewLifecycleOwner) {
            initViewPager()
        }
        observeEvents {
            on<Events.ConfigChangeEvent> { event ->
                // 书架配置变化后重新尝试构建容器，保持 Tab 与本地配置一致。
                if (event.contain(Config.BOOKSHELF_CONFIG_LIST)) {
                    initViewPager()
                }
            }
        }
        mRealVM.loginTaskData.observe(viewLifecycleOwner) {
            // 同一天只展示一次登录奖励欢迎弹窗，日期标记由 Config 持久化。
            val todayDate = com.kuaigeglobal.epnova.common.utils.DateUtil.getDateTime()
            if (Config.get().loginRewardsDialog != todayDate) {
                RewardWelcomeDialog.newInstance(it).show(childFragmentManager)
                Config.get().loginRewardsDialog = todayDate
            }
        }
    }

    /** 只在主页真正可见且访客尚未拿到任务数据时请求登录引导内容。 */
    private fun triggerGuideLoginIfVisible() {
        if (!isResumed || isHidden || !isVisible) return
        if (User.get().isGuest && mRealVM.loginTaskData.value == null) {
            mRealVM.getLoginTaskData()
        }
    }

    /** 注销 ViewPager 回调并释放子页面适配器，避免容器销毁后继续回调。 */
    override fun onDestroy() {
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        binding.viewPager.adapter = null
        super.onDestroy()
    }

}
