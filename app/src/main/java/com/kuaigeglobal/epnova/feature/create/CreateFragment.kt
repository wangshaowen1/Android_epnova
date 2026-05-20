package com.kuaigeglobal.epnova.feature.create

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.fragment.app.Fragment
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.FragmentCreateBinding
import com.kuaigeglobal.epnova.feature.creator.CreationRootActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.feature.create.child.ResourceCenterFragment
import com.kuaigeglobal.epnova.feature.create.child.TopEarnersFragment
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.crop.util.ViewUtils
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlin.math.abs

class CreateFragment : BaseViewModelFragment<FragmentCreateBinding, CreateViewModel>() {

    companion object {
        private const val KEY_VISIT_COUNT = "visit_count_create_fragment"
        fun newInstance(): CreateFragment {
            val fragment = CreateFragment()
            return fragment
        }
    }

    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initView() {
        binding.llTop.minimumHeight = ViewUtils.getStatusBarHeight(activity)
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange.toFloat()
            val ratio = abs(verticalOffset) / totalScrollRange
            val alpha = if (ratio < 0.95f) 1f - ratio / 0.95f else 0f
            binding.llTop.alpha = alpha
        }
        bannerAdapter = object : BaseBannerAdapter<BannerInfo>() {
            override fun bindData(
                holder: BaseViewHolder<BannerInfo>?, data: BannerInfo, position: Int, pageSize: Int
            ) {

            }

            override fun getLayoutId(viewType: Int): Int {
                return R.layout.item_message_banner
            }
        }

        binding.banner.setAdapter(bannerAdapter)
            .setLifecycleRegistry(lifecycle)
            .setAutoPlay(true)
            .setIndicatorSliderRadius(2f.dp)
            .setIndicatorSliderGap(4.dp)
            .setIndicatorHeight(4.dp)
            .setIndicatorSliderWidth(4.dp, 16.dp)
            .setIndicatorSliderColor(
                R.color.color_60_ffffff.getResColor(), R.color.white.getResColor()
            ).setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            .setIndicatorMargin(0, 0, 0, 4.dp)
            .setIndicatorView(binding.ivIndicator)
            .setIndicatorVisibility(View.VISIBLE)
            .create()


        binding.btnCreation.setOnSingleClickListener {
            if (Config.get().isInReview && AppNavigator.checkLoginIntercepted()) return@setOnSingleClickListener
            CreationRootActivity.start(activity)
        }

        initViewpager()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun initViewpager() {
        val titles = listOf(
            getString(R.string.create_page_top_earners),
            getString(R.string.create_page_resource_center)
        )
        val navigator = CommonNavigator(context)
        navigator.isAdjustMode = false
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int {
                return titles.size
            }

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = ColorTransitionPagerTitleView(context)
                titleView.setIsSelectedBold(true)
                titleView.setPadding(0, 0, 40.dp, 0)
                titleView.setLines(1)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                titleView.gravity = Gravity.CENTER

                titleView.text = titles.getOrNull(index)
                titleView.normalColor = topActivity?.getThemeColor(R.attr.textColor4) ?: 0
                titleView.selectedColor = topActivity?.getThemeColor(R.attr.textColor1) ?: 0
                titleView.setOnClickListener {
                    binding.viewPager2.currentItem = index
                }
                return titleView
            }

            override fun getIndicator(context: Context): IPagerIndicator? {
                return null
            }
        }
        binding.magicIndicator.navigator = navigator

        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(TopEarnersFragment())
        fragmentList.add(ResourceCenterFragment())

        val viewPagerAdapter = FragmentStateCacheAdapter(
            childFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager2.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager2)
        binding.viewPager2.setCurrentItem(0, false)
        binding.viewPager2.removeTheRipples()

    }
}