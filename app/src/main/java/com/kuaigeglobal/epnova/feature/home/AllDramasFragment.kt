package com.kuaigeglobal.epnova.feature.home

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.databinding.FragmentHomeAllDramasBinding
import com.kuaigeglobal.epnova.feature.home.bookshelf.BookShelfListActivity
import com.kuaigeglobal.epnova.widget.decoration.StaggeredGridSpaceItemDecoration
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

        fun newInstance(bookshelfSectionList: List<BookShelfSectionInfo>, tabName: String): AllDramasFragment =
            AllDramasFragment().apply {
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
        AllDramasFooterAdapter {
            allowInitialScrollToTop = false
            mRealVM.retrySections()
            // retry 只重试失败的分页请求，保留已经加载的作品。
            homeAdapter.retry()
        }
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
        initList()
        mRealVM.loadSections(bookshelfSectionList)
    }

    override fun onViewLifecycleReady(view: View, savedInstanceState: Bundle?) {
        super.onViewLifecycleReady(view, savedInstanceState)
        // 恢复页面时保留已有位置，仅全新进入时允许首屏自动定位。
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

    private fun initList() {
        titleAdapter.setTitle(getString(R.string.tab_all_dramas))
        titleAdapter.isVisible = true
        // 从上到下拼接：横滑区块 → All Dramas 标题 → 分页作品 → 状态页脚。
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
        // refresh 重新加载底部作品列表；AllDramasPagingSource 会从第一页开始请求。
        homeAdapter.refresh()
    }
}
