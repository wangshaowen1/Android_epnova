package com.kuaigeglobal.epnova.feature.home

import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.core.databinding.loadUri
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.databinding.FragmentHomeBinding
import com.kuaigeglobal.epnova.feature.home.search.SearchActivity
import com.kuaigeglobal.epnova.feature.rewards.RewardsActivity
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.crop.util.ViewUtils
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlin.math.abs

class HomeFragment :
    BaseViewModelFragment<FragmentHomeBinding, HomeViewModel>() {
    companion object {
        fun newInstance(): HomeFragment {
            val fragment = HomeFragment()
            return fragment
        }
    }

    private val homeContentFragment by lazy { HomeContentFragment() }
    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
        initListener()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        mRealVM.getRewardUserUncheck()
    }

    override fun onResume() {
        super.onResume()
        mRealVM.getRewardUserUncheck()
    }

    private fun initListener() {
        binding.btnSearch.setOnSingleClickListener {
            SearchActivity.start(mActivity)
        }
        binding.btnRewards.setOnSingleClickListener {
            RewardsActivity.start(mActivity)
        }
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
                        AppRouter.jump(data.jumpUri)
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

        binding.srl.setOnRefreshListener {
            refreshPageData()
        }
        initViewPager()
    }

    private fun refreshPageData() {
        var pendingCount = 2
        fun finishOne() {
            pendingCount--
            if (pendingCount == 0) {
                binding.srl.finishRefresh()
            }
        }

        mRealVM.getData {
            finishOne()
        }
        homeContentFragment.refresh {
            finishOne()
        }
    }

    private fun dataUpdate() {
        mRealVM.bannerData.observe(this) {
            binding.banner.refreshData(it)
        }
    }

    private fun initViewPager() {
        val fragmentList = arrayListOf<Fragment>(homeContentFragment)
        val viewPagerAdapter = FragmentStateCacheAdapter(
            childFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager2.adapter = viewPagerAdapter
        binding.viewPager2.offscreenPageLimit = fragmentList.size
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.setCurrentItem(0, false)
        binding.viewPager2.removeTheRipples()
    }
}
