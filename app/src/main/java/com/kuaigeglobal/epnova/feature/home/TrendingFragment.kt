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

/** Trending 页：配置驱动的横滑书架 + 配置驱动的瀑布流。 */
class TrendingFragment : BaseViewModelFragment<FragmentTrendingBinding, HomeViewModel>() {

    companion object {
        private const val ARG_BOOKSHELF_SECTION_LIST = "bookshelfSectionList"
        private const val ARG_TAB_NAME = "tabName"

        fun newInstance(bookshelfSectionList: List<BookShelfSectionInfo>, tabName: String): TrendingFragment =
            TrendingFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_BOOKSHELF_SECTION_LIST, ArrayList(bookshelfSectionList))
                    putString(ARG_TAB_NAME, tabName)
                }
            }
    }

    val bookshelfSectionList: List<BookShelfSectionInfo> by lazy {
        arguments?.getParcelableArrayListValue(
            ARG_BOOKSHELF_SECTION_LIST,
            BookShelfSectionInfo::class.java
        ).orEmpty()
    }

    private val tabName by lazy { arguments?.getString(ARG_TAB_NAME) }
    private val homeAdapter by lazy { HomeAdapter(tabName) }
    private val titleAdapter by lazy { WaterfallTitleAdapter() }
    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>
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
    private val footerAdapter by lazy {
        MyMultiStatePagingFooterAdapter(
            hostAdapter = homeAdapter,
            displayEmptyState = false,
            requestButtonDisplay = true,
            noMoreThreshold = 1,
            refresh = {
                allowInitialScrollToTop = false
                mRealVM.retrySections()
                homeAdapter.retry()
            }
        )
    }

    private var scrollToTopAfterRefresh = false
    private var allowInitialScrollToTop = true
    private val initialScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                allowInitialScrollToTop = false
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        mRealVM.loadSections(bookshelfSectionList)
    }

    private fun initView() {
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
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

    override fun onViewLifecycleReady(view: View, savedInstanceState: Bundle?) {
        super.onViewLifecycleReady(view, savedInstanceState)
        if (savedInstanceState != null) allowInitialScrollToTop = false
        binding.recyclerView.addOnScrollListener(initialScrollListener)
        dataUpdate()
    }

    override fun onPause() {
        allowInitialScrollToTop = false
        super.onPause()
    }

    override fun onDestroyView() {
        allowInitialScrollToTop = false
        binding.recyclerView.removeOnScrollListener(initialScrollListener)
        super.onDestroyView()
    }

    private fun dataUpdate() {
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
                if (allowInitialScrollToTop && sections.isNotEmpty()) {
                    allowInitialScrollToTop = false
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }
        mRealVM.bannerData.observe(viewLifecycleOwner) { banners ->
            val data = banners.orEmpty()
            updateBannerVisible(data.isNotEmpty())
            binding.banner.refreshData(data)
        }
        mRealVM.waterfallSection.observe(viewLifecycleOwner) { section ->
            homeAdapter.bookshelfName = section?.bookshelfName
            titleAdapter.setTitle(section?.bookshelfName)
            titleAdapter.isVisible = section != null
        }
        mRealVM.footerLoadState.observe(viewLifecycleOwner) { state ->
            footerAdapter.displayEmptyState = state is PagingLoadState.Error
            footerAdapter.loadState = state
        }
        mRealVM.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            if (!refreshing) {
                binding.srl.finishRefresh()
                if (scrollToTopAfterRefresh) {
                    scrollToTopAfterRefresh = false
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun updateBannerVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.banner.visibility = visibility
        binding.ivIndicator.visibility = visibility
    }

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

    private fun refreshData() {
        allowInitialScrollToTop = false
        scrollToTopAfterRefresh = true
        mRealVM.onPagingRefreshStarted()
        mRealVM.refreshSections()
        homeAdapter.refresh()
        if (mRealVM.bannerData.value.isNullOrEmpty()) mRealVM.getData()
    }

    private fun BannerInfo.firebaseBannerId(): String? {
        return id ?: jumpUri ?: title ?: sortNo?.toString()
    }
}
