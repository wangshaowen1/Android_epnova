package com.kuaigeglobal.epnova.feature.home

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.core.network.apis.response.Section
import com.kuaigeglobal.epnova.databinding.FragmentHomeAllDramasBinding
import com.kuaigeglobal.epnova.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableArrayListValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** All Dramas 页，沿用 Trending 的书架和瀑布流逻辑，不展示 Banner。 */
class AllDramasFragment :
    BaseViewModelFragment<FragmentHomeAllDramasBinding, HomeAllDramasViewModel>() {

    companion object {
        private const val ARG_BOOKSHELF_SECTION_LIST = "bookshelfSectionList"

        fun newInstance(bookshelfSectionList: List<BookShelfSectionInfo>): AllDramasFragment =
            AllDramasFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_BOOKSHELF_SECTION_LIST, ArrayList(bookshelfSectionList))
                }
            }
    }

    val bookshelfSectionList: List<BookShelfSectionInfo> by lazy {
        arguments?.getParcelableArrayListValue(
            ARG_BOOKSHELF_SECTION_LIST,
            BookShelfSectionInfo::class.java
        ).orEmpty()
    }

    private val homeAdapter by lazy { HomeAdapter() }
    private val titleAdapter by lazy { WaterfallTitleAdapter() }
    private val shelfContainerAdapter by lazy { ShelfContainerAdapter() }

    override fun onPrepareView() {
        super.onPrepareView()
        initList()
        dataUpdate()
    }

    private fun dataUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.feedFlow.collectLatest { pagingData ->
                    homeAdapter.submitData(pagingData)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            homeAdapter.loadStateFlow.first {
                it.refresh is LoadState.NotLoading && homeAdapter.itemCount > 0
            }
            binding.recyclerView.scrollToPosition(0)
        }

        mRealVM.bookShelfData.observe(viewLifecycleOwner) { data ->
            val sections = data?.sections.orEmpty()
            shelfContainerAdapter.submitList(sections.filter { it.bookshelfType == 1 })
            updateWaterfallSection(sections)
        }
        mRealVM.bookShelfError.observe(viewLifecycleOwner) { error ->
            if (error != null && shelfContainerAdapter.itemCount == 0 && homeAdapter.itemCount == 0) {
                binding.lsv.showRetry(error)
            }
        }
    }

    private fun initList() {
        val waterfallWithFooter = homeAdapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = homeAdapter,
                displayEmptyState = false,
                refresh = { homeAdapter.retry() }
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
            isContentEmpty = { shelfContainerAdapter.itemCount == 0 && homeAdapter.itemCount == 0 },
            onRetryListener = { retryLoadData() },
            extraContentAdapters = listOf(shelfContainerAdapter)
        )
    }

    private fun retryLoadData() {
        if (mRealVM.bookShelfData.value == null) mRealVM.loadData()
    }

    private fun updateWaterfallSection(sections: List<Section>) {
        val waterfallSection = sections.find { it.bookshelfType == 2 }
        waterfallSection?.let { titleAdapter.setTitle(it.bookshelfName) }
        titleAdapter.isVisible = waterfallSection != null
        mRealVM.currentWaterfallId.value = waterfallSection?.bookshelfId ?: ""
    }
}
