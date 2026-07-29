package com.kuaigeglobal.epnova.feature.home

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.databinding.loadUri
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.response.Section
import com.kuaigeglobal.epnova.databinding.FragmentHomeBinding
import com.kuaigeglobal.epnova.feature.home.search.SearchActivity
import com.kuaigeglobal.epnova.feature.rewards.RewardsActivity
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.crop.util.ViewUtils
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 主页 - Home
 */
class HomeFragment :
    BaseViewModelFragment<FragmentHomeBinding, HomeViewModel>() {
    companion object {
        fun newInstance(): HomeFragment {
            val fragment = HomeFragment()
            return fragment
        }
    }

    private val homeAdapter by lazy {
        HomeAdapter()
    }
    private val titleAdapter by lazy {
        WaterfallTitleAdapter()
    }
    private val shelfContainerAdapter by lazy {
        ShelfContainerAdapter()
    }
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
            // 首页右上角福利中心入口点击。
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.REWARDS,
                FirebaseEvent.Action.HOME_REWARDS_BTN_CLICK
            )
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
                        val bannerId = data.firebaseBannerId()
                        EventManager.onFirebaseTrack(
                            FirebaseEvent.Category.HOME,
                            FirebaseEvent.Action.BANNER_CLICK,
                            bannerId
                        )
                        AppRouter.jumpFromBanner(data.jumpUri, bannerId)
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
        updateBannerVisible(false)

        initList()
    }

    private fun dataUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.feedFlow.collectLatest { pagingData ->
                    homeAdapter.submitData(pagingData)
                }
            }
        }

        // 首次数据加载完成后，确保从第一条开始显示
        // loadStateFlow.first() 只触发一次：refresh 状态变为 NotLoading 且列表有数据时
        lifecycleScope.launch {
            homeAdapter.loadStateFlow.first { it.refresh is LoadState.NotLoading && homeAdapter.itemCount > 0 }
            binding.recyclerView.scrollToPosition(0)
        }

        mRealVM.bannerData.observe(this) { banners ->
            val data = banners.orEmpty()
            updateBannerVisible(data.isNotEmpty())
            binding.banner.refreshData(data)
//            data.forEach { banner ->
//            }
        }

        mRealVM.bookShelfData.observe(viewLifecycleOwner) { data ->
            val sections = data?.sections.orEmpty()
            val horizontalSections = sections.filter { it.bookshelfType == 1 }
            shelfContainerAdapter.submitList(horizontalSections)
            updateWaterfallSection(sections)
        }

        mRealVM.bookShelfError.observe(viewLifecycleOwner) { error ->
            if (error != null && shelfContainerAdapter.itemCount == 0 && homeAdapter.itemCount == 0) {
                updateBannerVisible(false)
                binding.lsv.showRetry(error)
            }
        }
    }

    private fun updateBannerVisible(visible: Boolean) {
        if (visible) {
            binding.lsv.showContent()
        }
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.banner.visibility = visibility
        binding.ivIndicator.visibility = visibility
    }

    private fun initList() {
        val waterfallWithFooter = homeAdapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = homeAdapter,
                displayEmptyState = false,
                refresh = {
                    homeAdapter.retry()
                },
            )
        )
        val concatAdapter = ConcatAdapter(shelfContainerAdapter, titleAdapter, waterfallWithFooter)

        binding.recyclerView.apply {
            isFocusable = false
            itemAnimator = null
            addItemDecoration(StaggeredGridSpaceItemDecoration(2, 10.dp, 0, 0))
            adapter = concatAdapter
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = homeAdapter,
            loadStateHolder = binding.lsv,
            isContentEmpty = {
                shelfContainerAdapter.itemCount == 0 && homeAdapter.itemCount == 0
            },
            onRetryListener = {
                retryLoadData()
            },
            extraContentAdapters = listOf(shelfContainerAdapter)
        )
    }

    private fun retryLoadData() {
        if (mRealVM.bannerData.value.isNullOrEmpty()) {
            mRealVM.getData()
        }
        if (mRealVM.bookShelfData.value == null) {
            mRealVM.loadData()
        }
    }

    private fun updateWaterfallSection(sections: List<Section>) {
        val waterfallSection = sections.find { it.bookshelfType == 2 }
        waterfallSection?.let { title ->
            titleAdapter.setTitle(title.bookshelfName)
        }
        titleAdapter.isVisible = waterfallSection != null
        mRealVM.currentWaterfallId.value = waterfallSection?.bookshelfId ?: ""
    }

    private fun BannerInfo.firebaseBannerId(): String? {
        return id ?: jumpUri ?: title ?: sortNo?.toString()
    }

}
