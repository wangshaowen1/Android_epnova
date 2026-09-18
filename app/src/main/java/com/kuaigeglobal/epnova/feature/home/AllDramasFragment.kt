package com.kuaigeglobal.epnova.feature.home

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.databinding.FragmentHomeAllDramasBinding
import com.kuaigeglobal.epnova.feature.home.bookshelf.BookShelfListActivity
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableArrayListValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** All Dramas 页：配置驱动的横滑预览 + 独立的全部作品分页。 */
class AllDramasFragment :
    BaseViewModelFragment<FragmentHomeAllDramasBinding, HomeAllDramasViewModel>() {

    companion object {
        private const val ARG_BOOKSHELF_SECTION_LIST = "bookshelfSectionList"
        private const val ARG_TAB_NAME = "tabName"

        /** 创建 All Dramas 子页，并把主页配置的区块列表和 Tab 名称固定在参数中。 */
        fun newInstance(bookshelfSectionList: List<BookShelfSectionInfo>, tabName: String): AllDramasFragment =
            AllDramasFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_BOOKSHELF_SECTION_LIST, ArrayList(bookshelfSectionList))
                    putString(ARG_TAB_NAME, tabName)
                }
            }
    }

    /** 主页传入的横滑书架配置；子页只读取，不从容器反向获取可变状态。 */
    val bookshelfSectionList: List<BookShelfSectionInfo> by lazy {
        arguments?.getParcelableArrayListValue(
            ARG_BOOKSHELF_SECTION_LIST,
            BookShelfSectionInfo::class.java
        ).orEmpty()
    }

    /** 用于作品点击埋点的 Tab 名称，同时传给横滑书架和底部作品适配器。 */
    private val tabName by lazy { arguments?.getString(ARG_TAB_NAME) }

    // 底部 All Dramas 作品分页列表：展示封面、标题和播放量，点击作品进入播放页。
    // 数据来自 /app/bookshelf/v2/all/dramas/page，经 ViewModel.feedFlow 提交给此 Adapter。
    private val homeAdapter by lazy { HomeAdapter(tabName, getString(R.string.tab_all_dramas)) }
    // 底部作品列表上方固定的 All Dramas 标题。
    private val titleAdapter by lazy { WaterfallTitleAdapter() }
    // 上方配置区块：每个区块展示自己的标题和横滑预览作品。
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
    // 统一展示横滑预览和底部分页的加载状态、局部失败及没有更多。
    private val footerAdapter by lazy {
        MyMultiStatePagingFooterAdapter(
            hostAdapter = homeAdapter,
            displayEmptyState = false,
            noMoreThreshold = 1,
            refresh = {
                allowInitialScrollToTop = false
                mRealVM.retrySections()
                // retry 只重试失败的分页请求，保留已经加载的作品。
                homeAdapter.retry()
            }
        )
    }

/*    // 统一展示横滑预览和底部分页的加载状态、局部失败及没有更多。
    private val footerAdapter by lazy {
        AllDramasFooterAdapter {
            allowInitialScrollToTop = false
            mRealVM.retrySections()
            // retry 只重试失败的分页请求，保留已经加载的作品。
            homeAdapter.retry()
        }
    }*/

    private var scrollToTopAfterRefresh = false
    private var allowInitialScrollToTop = true
    private val initialScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                allowInitialScrollToTop = false
            }
        }
    }

    /** 初始化列表结构后加载配置驱动的横滑区块。 */
    override fun onPrepareView() {
        super.onPrepareView()
        initList()
        mRealVM.loadSections(bookshelfSectionList)
    }

    /** 绑定视图生命周期内的数据收集，并在首次展示时处理恢复位置。 */
    override fun onViewLifecycleReady(view: View, savedInstanceState: Bundle?) {
        super.onViewLifecycleReady(view, savedInstanceState)
        // 恢复页面时保留已有位置，仅全新进入时允许首屏自动定位。
        if (savedInstanceState != null) allowInitialScrollToTop = false
        binding.recyclerView.addOnScrollListener(initialScrollListener)
        dataUpdate()
    }

    /** 页面离开后禁止下一次数据回调把列表强制滚回顶部。 */
    override fun onPause() {
        allowInitialScrollToTop = false
        super.onPause()
    }

    /** 清理滚动监听，避免 View 销毁后继续引用旧的 RecyclerView。 */
    override fun onDestroyView() {
        allowInitialScrollToTop = false
        binding.recyclerView.removeOnScrollListener(initialScrollListener)
        super.onDestroyView()
    }

    /** 收集底部分页、横滑区块和合并后的刷新状态，并同步给页面控件。 */
    private fun dataUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mRealVM.feedFlow.collectLatest { pagingData ->
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
            val recyclerView = binding.recyclerView
            shelfSectionAdapter.submitList(sections) {
                // 分页可能先显示，等待顶部区块真正插入后再定位；用户已滚动则保留其位置。
                if (allowInitialScrollToTop && sections.isNotEmpty()) {
                    allowInitialScrollToTop = false
                    recyclerView.scrollToPosition(0)
                }
            }
        }
        mRealVM.footerLoadState.observe(viewLifecycleOwner) { state ->
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

    /** 组合横滑区块、标题、三列作品和页脚，并配置各自的网格占位规则。 */
    private fun initList() {
        titleAdapter.setTitle(getString(R.string.tab_all_dramas))
        titleAdapter.isVisible = true
        // 从上到下拼接：横滑区块 → All Dramas 标题 → 分页作品 → 状态页脚。
        val concatAdapter = ConcatAdapter(shelfSectionAdapter, titleAdapter, homeAdapter, footerAdapter)
        binding.recyclerView.apply {
            isFocusable = false
            itemAnimator = null
            val gridLayoutManager = layoutManager as GridLayoutManager
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val itemAdapter = concatAdapter.getWrappedAdapterAndPosition(position).first
                    // 作品每项占一列，其余书架、标题和页脚占满整行。
                    return if (itemAdapter === homeAdapter) 1 else gridLayoutManager.spanCount
                }
            }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(0, 0, 0, 0)
                    val params = view.layoutParams as? GridLayoutManager.LayoutParams ?: return
                    if (params.spanSize == 1) {
                        val spacing = 8.dp
                        val spanCount = gridLayoutManager.spanCount
                        // 分摊列间距，保持三列卡片等宽且外侧不额外留白。
                        outRect.left = params.spanIndex * spacing / spanCount
                        outRect.right = spacing - (params.spanIndex + 1) * spacing / spanCount
                    }
                }
            })
            adapter = concatAdapter
        }

        binding.srl.setEnableRefresh(true)
        binding.srl.setEnableLoadMore(false)
        binding.srl.setOnRefreshListener { refreshData() }
        binding.lsv.setRetryListener { refreshData() }
    }

    /** 同时刷新横滑区块和底部作品分页，完成后把列表滚回顶部。 */
    private fun refreshData() {
        allowInitialScrollToTop = false
        scrollToTopAfterRefresh = true
        mRealVM.onPagingRefreshStarted()
        mRealVM.refreshSections()
        // refresh 重新加载底部作品列表；AllDramasPagingSource 会从第一页开始请求。
        homeAdapter.refresh()
    }
}
