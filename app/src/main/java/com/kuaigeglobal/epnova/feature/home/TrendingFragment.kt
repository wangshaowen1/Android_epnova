package com.kuaigeglobal.epnova.feature.home

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.databinding.loadUri
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.BannerInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.databinding.FragmentTrendingBinding
import com.kuaigeglobal.epnova.feature.home.bookshelf.BookShelfListActivity
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableArrayListValue
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.paging.LoadState as PagingLoadState

/**
 * Trending 子页：按主页配置组合 Banner、横滑书架和底部瀑布流。
 *
 * `bookshelfType == 1` 的配置用于加载横滑书架，首个 `bookshelfType == 2` 的配置
 * 作为瀑布流分页入口；页面统一协调两类内容的加载、刷新和状态展示。
 */
class TrendingFragment : BaseViewModelFragment<FragmentTrendingBinding, HomeViewModel>() {

    companion object {
        private const val ARG_BOOKSHELF_SECTION_LIST = "bookshelfSectionList"
        private const val ARG_TAB_NAME = "tabName"

        /** 创建 Trending 子页，并把主页下发的区块配置和 Tab 名称固定在参数中。 */
        fun newInstance(bookshelfSectionList: List<BookShelfSectionInfo>, tabName: String): TrendingFragment =
            TrendingFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_BOOKSHELF_SECTION_LIST, ArrayList(bookshelfSectionList))
                    putString(ARG_TAB_NAME, tabName)
                }
            }
    }

    /** 主页传入的横滑书架和瀑布流配置；子页只读取，不依赖容器页的可变状态。 */
    val bookshelfSectionList: List<BookShelfSectionInfo> by lazy {
        arguments?.getParcelableArrayListValue(
            ARG_BOOKSHELF_SECTION_LIST,
            BookShelfSectionInfo::class.java
        ).orEmpty()
    }

    /** 用于作品点击埋点的 Tab 名称，并透传给横滑书架和瀑布流 Adapter。 */
    private val tabName by lazy { arguments?.getString(ARG_TAB_NAME) }
    // 底部瀑布流分页列表，数据由 ViewModel.trendingFeedFlow 提交。
    private val homeAdapter by lazy { HomeAdapter(tabName) }
    // 瀑布流配置存在时，展示对应的书架标题。
    private val titleAdapter by lazy { WaterfallTitleAdapter() }
    // Banner Adapter 在 initView 中创建并绑定到 BannerView。
    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>
    // 上方配置区块：每个区块展示标题、横滑作品及“查看全部”入口。
    private val shelfSectionAdapter by lazy {
        ShelfSectionAdapter(tabName) { section, entryType ->
            section.bookshelfSectionCode?.let { sectionCode ->
                BookShelfListActivity.start(
                    activity = activity,
                    bookshelfSectionCode = sectionCode.toString(),
                    bookshelfName = section.bookshelfName,
                    entryType = entryType
                )
            }
        }
    }
    // 汇总横滑书架与瀑布流分页状态，统一展示加载、失败重试和没有更多。
    private val footerAdapter by lazy {
        MyMultiStatePagingFooterAdapter(
            hostAdapter = homeAdapter,
            displayEmptyState = false,
            requestButtonDisplay = true,
            noMoreThreshold = 1,
            refresh = {
                allowInitialScrollToTop = false
                // 仅重试加载失败的横滑区块，已成功区块保持不变。
                mRealVM.retrySections()
                // Paging retry 只重试失败的分页请求，不清空已加载作品。
                homeAdapter.retry()
            }
        )
    }

    /** 标记下拉刷新完成后需要把合并列表定位回顶部。 */
    private var scrollToTopAfterRefresh = false
    /** 仅全新进入且用户尚未操作列表时，允许异步区块插入后自动定位顶部。 */
    private var allowInitialScrollToTop = true
    private val initialScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                // 用户已主动浏览内容，后续数据回调不得打断当前位置。
                allowInitialScrollToTop = false
            }
        }
    }

    /** 初始化页面结构，并让 ViewModel 按主页配置拆分横滑区块和瀑布流入口。 */
    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        mRealVM.loadSections(bookshelfSectionList)
    }

    /** 配置 AppBar 渐隐效果、Banner 展示与页面内容列表。 */
    private fun initView() {
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            // AppBar 接近完全折叠时让顶部内容平滑消失，避免与折叠态重叠。
            val totalScrollRange = appBarLayout.totalScrollRange.toFloat()
            val ratio = if (totalScrollRange == 0f) 0f else abs(verticalOffset) / totalScrollRange
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
                        // 路由和点击埋点共用同一 Banner 标识，保证来源口径一致。
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

            override fun getLayoutId(viewType: Int): Int = R.layout.item_message_banner
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
            )
            .setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            .setIndicatorMargin(0, 0, 0, 4.dp)
            .setIndicatorView(binding.ivIndicator)
            .setIndicatorVisibility(View.VISIBLE)
            .create()
        updateBannerVisible(false)
        initList()
    }

    /** 绑定视图生命周期内的数据观察，并处理首次进入时的列表定位。 */
    override fun onViewLifecycleReady(view: View, savedInstanceState: Bundle?) {
        super.onViewLifecycleReady(view, savedInstanceState)
        // 页面恢复时保留 RecyclerView 已保存的位置，不再触发首屏自动定位。
        if (savedInstanceState != null) allowInitialScrollToTop = false
        binding.recyclerView.addOnScrollListener(initialScrollListener)
        dataUpdate()
    }

    /** 页面离开后禁止迟到的数据回调把列表强制滚回顶部。 */
    override fun onPause() {
        allowInitialScrollToTop = false
        super.onPause()
    }

    /** 清理滚动监听，避免 View 销毁后继续引用旧 RecyclerView。 */
    override fun onDestroyView() {
        allowInitialScrollToTop = false
        binding.recyclerView.removeOnScrollListener(initialScrollListener)
        super.onDestroyView()
    }

    /** 收集 Banner、横滑区块、瀑布流分页及合并后的刷新状态并同步到页面。 */
    private fun dataUpdate() {
        // Paging 数据和 LoadState 仅在视图至少处于 STARTED 时收集，离屏后自动挂起。
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mRealVM.trendingFeedFlow.collectLatest { pagingData ->
                        homeAdapter.submitData(pagingData)
                    }
                }
                launch {
                    homeAdapter.loadStateFlow.collect { states ->
                        mRealVM.onPagingLoadStateChanged(states, homeAdapter.itemCount)
                    }
                }
            }
        }
        mRealVM.shelfSections.observe(viewLifecycleOwner) { sections ->
            shelfSectionAdapter.submitList(sections) {
                // 区块异步插入列表头部后仅自动定位一次；用户已拖动则保留当前位置。
                if (allowInitialScrollToTop && sections.isNotEmpty()) {
                    allowInitialScrollToTop = false
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }
        mRealVM.bannerData.observe(viewLifecycleOwner) { banners ->
            val data = banners.orEmpty()
            // Banner 与外置指示器必须同步显隐，空数据不保留占位。
            updateBannerVisible(data.isNotEmpty())
            binding.banner.refreshData(data)
        }
        mRealVM.waterfallSection.observe(viewLifecycleOwner) { section ->
            // 瀑布流配置同时决定作品点击来源名称和标题是否展示。
            homeAdapter.bookshelfName = section?.bookshelfName
            titleAdapter.setTitle(section?.bookshelfName)
            titleAdapter.isVisible = section != null
        }
        mRealVM.footerLoadState.observe(viewLifecycleOwner) { state ->
            // 无内容且加载失败时允许页脚承担空态重试入口。
            footerAdapter.displayEmptyState = state is PagingLoadState.Error
            footerAdapter.loadState = state
        }
        mRealVM.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            if (!refreshing) {
                binding.srl.finishRefresh()
                if (scrollToTopAfterRefresh) {
                    // 等横滑区块和 Paging 刷新都结束后再统一回顶。
                    scrollToTopAfterRefresh = false
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    /** 同步 Banner 容器和外置指示器的可见性。 */
    private fun updateBannerVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.banner.visibility = visibility
        binding.ivIndicator.visibility = visibility
    }

    /** 按“横滑书架 → 瀑布流标题 → 分页作品 → 状态页脚”组装页面列表。 */
    private fun initList() {
        titleAdapter.isVisible = false
        val concatAdapter = ConcatAdapter(shelfSectionAdapter, titleAdapter, homeAdapter, footerAdapter)
        binding.recyclerView.apply {
            isFocusable = false
            itemAnimator = null
            addItemDecoration(StaggeredGridSpaceItemDecoration(2, 10.dp, 0, 0))
            adapter = concatAdapter
        }
        binding.srl.setEnableRefresh(true)
        binding.srl.setEnableLoadMore(false)
        binding.srl.setOnRefreshListener { refreshData() }
        binding.lsv.setRetryListener { refreshData() }
    }

    /** 同时刷新横滑书架和瀑布流分页，全部完成后将列表滚回顶部。 */
    private fun refreshData() {
        allowInitialScrollToTop = false
        scrollToTopAfterRefresh = true
        mRealVM.onPagingRefreshStarted()
        mRealVM.refreshSections()
        homeAdapter.refresh()
        // Banner 独立于书架刷新；仅当前无数据时补拉，避免重复请求。
        if (mRealVM.bannerData.value.isNullOrEmpty()) mRealVM.getData()
    }

    /** 为埋点和路由选择可用的 Banner 标识，按稳定字段依次降级。 */
    private fun BannerInfo.firebaseBannerId(): String? {
        return id ?: jumpUri ?: title ?: sortNo?.toString()
    }
}
