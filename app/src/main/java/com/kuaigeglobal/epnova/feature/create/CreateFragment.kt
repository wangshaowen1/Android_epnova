package com.kuaigeglobal.epnova.feature.create

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.kuaigeglobal.epnova.databinding.FragmentCreateBinding
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.databinding.loadUri
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.feature.create.child.ResourceCenterFragment
import com.kuaigeglobal.epnova.feature.create.child.TopEarnersFragment
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
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

/**
 * 主页 - Featured
 */
class CreateFragment : BaseViewModelFragment<FragmentCreateBinding, CreateViewModel>() {

    companion object {
        private const val KEY_VISIT_COUNT = "visit_count_create_fragment"
        fun newInstance(): CreateFragment {
            val fragment = CreateFragment()
            return fragment
        }
    }

    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>
    private val topEarnersFragment by lazy { TopEarnersFragment() }
    private val resourceCenterFragment by lazy { ResourceCenterFragment() }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        dataUpdate()
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
                holder?.apply {
                    val image = findViewById<ImageView>(R.id.image)
                    loadUri(image, data.imageUrl, R.drawable.img_default_cover_16_9.getResDrawable())
                    image.setOnSingleClickListener {
                        AppRouter.jumpFromBanner(data.jumpUri, data.firebaseBannerId())
                    }
                }
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
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_CREATE))
//            CreationRootActivity.start(activity)
        }
        binding.srl.setOnRefreshListener {
            refreshPageData()
        }

        initViewpager()
    }

    private fun refreshPageData() {
        var pendingCount = 3
        fun finishOne() {
            pendingCount--
            if (pendingCount == 0) {
                binding.srl.finishRefresh()
            }
        }

        mRealVM.getData {
            finishOne()
        }
        topEarnersFragment.refresh {
            finishOne()
        }
        resourceCenterFragment.refresh {
            finishOne()
        }
    }

    private fun dataUpdate() {
        mRealVM.bannerData.observe(this) { banners ->
            binding.banner.refreshData(banners)
        }
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
                val titleView = object : ColorTransitionPagerTitleView(context) {
                    private val normalTypeface = ResourcesCompat.getFont(context, R.font.outfit_regular)
                    private val selectedTypeface = ResourcesCompat.getFont(context, R.font.outfit_medium)

                    override fun onSelected(index: Int, totalCount: Int) {
                        super.onSelected(index, totalCount)
                        typeface = selectedTypeface
                    }

                    override fun onDeselected(index: Int, totalCount: Int) {
                        super.onDeselected(index, totalCount)
                        typeface = normalTypeface
                    }
                }
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
        fragmentList.add(topEarnersFragment)
        fragmentList.add(resourceCenterFragment)

        val viewPagerAdapter = FragmentStateCacheAdapter(
            childFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager2.adapter = viewPagerAdapter
        binding.viewPager2.offscreenPageLimit = fragmentList.size
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager2)
        binding.viewPager2.setCurrentItem(0, false)
        binding.viewPager2.removeTheRipples()

    }

    private fun BannerInfo.firebaseBannerId(): String? {
        return jumpUri ?: title ?: sortNo?.toString()
    }
}
