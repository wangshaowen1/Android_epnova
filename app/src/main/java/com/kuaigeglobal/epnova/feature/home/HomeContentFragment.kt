package com.kuaigeglobal.epnova.feature.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.network.apis.response.Section
import com.kuaigeglobal.epnova.databinding.FragmentHomeContentBinding
import com.kuaigeglobal.epnova.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfParentFragment
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val REFRESH_COMPLETE_TIMEOUT_MS = 30_000L

class HomeContentFragment :
    BaseViewModelFragment<FragmentHomeContentBinding, HomeViewModel>(true) {

    private val homeAdapter by lazy {
        HomeAdapter()
    }
    private val titleAdapter by lazy {
        WaterfallTitleAdapter()
    }
    private val shelfContainerAdapter by lazy {
        ShelfContainerAdapter()
    }

    private var viewPrepared = false

    override fun createViewModel(): HomeViewModel {
        return getViewModelOfParentFragment<HomeViewModel>()
            ?: throw IllegalStateException("HomeContentFragment must be attached to HomeFragment")
    }

    override fun onPrepareView() {
        super.onPrepareView()
        viewPrepared = true
        initView()
        dataUpdate()
    }

    override fun onDestroy() {
        viewPrepared = false
        super.onDestroy()
    }

    fun refresh(onComplete: (() -> Unit)? = null) {
        if (!viewPrepared) {
            onComplete?.invoke()
            return
        }

        if (onComplete == null) {
            mRealVM.loadData()
            homeAdapter.refresh()
            return
        }

        refreshWithComplete(onComplete)
    }

    private fun refreshWithComplete(onComplete: () -> Unit) {
        val refreshComplete = RefreshCompleteCounter(2, onComplete)

        mRealVM.loadData {
            refreshComplete.finishOne()
            if (mRealVM.currentWaterfallId.value.isEmpty()) {
                refreshComplete.finishPaging()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withTimeoutOrNull(REFRESH_COMPLETE_TIMEOUT_MS) {
                    awaitRefreshComplete()
                }
            } finally {
                refreshComplete.finishPaging()
            }
        }
        homeAdapter.refresh()
    }

    private suspend fun awaitRefreshComplete() {
        var loadingSeen = false
        homeAdapter.loadStateFlow.first { loadStates ->
            when (loadStates.refresh) {
                is LoadState.Loading -> {
                    loadingSeen = true
                    false
                }

                is LoadState.Error,
                is LoadState.NotLoading -> loadingSeen
            }
        }
    }

    private fun initView() {
        val waterfallWithFooter = homeAdapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = homeAdapter,
                noDataButtonDisplay = false,
                requestButtonDisplay = false,
                displayedHeight = 400.dp,
                refresh = {
                    homeAdapter.refresh()
                },
                requestButtonTextAndImg = false,
            )
        )
        val concatAdapter = ConcatAdapter(shelfContainerAdapter, titleAdapter, waterfallWithFooter)

        binding.recyclerView.apply {
            isFocusable = false
            itemAnimator = null
            addItemDecoration(StaggeredGridSpaceItemDecoration(2, 10.dp, 0, 0))
            adapter = concatAdapter
        }
        binding.lsv.startRefreshWithAdapter(homeAdapter)
        binding.lsv.apply {
            setRetryListener {
                retryLoadData()
            }
        }
    }

    private fun dataUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.feedFlow.collectLatest { pagingData ->
                    homeAdapter.submitData(pagingData)
                }
            }
        }

        mRealVM.bookShelfData.observe(viewLifecycleOwner) { data ->
            val sections = data?.sections.orEmpty()
            shelfContainerAdapter.submitList(sections.filter { it.bookshelfType == 1 })
            updateWaterfallSection(sections)
        }
    }

    private fun retryLoadData() {
        if (mRealVM.bannerData.value.isNullOrEmpty()) {
            mRealVM.getData()
        }
        if (mRealVM.bookShelfData.value == null) {
            mRealVM.loadData()
        }
        homeAdapter.refresh()
    }

    private fun updateWaterfallSection(sections: List<Section>) {
        val waterfallSection = sections.find { it.bookshelfType == 2 }
        titleAdapter.isVisible = waterfallSection != null
        mRealVM.currentWaterfallId.value = waterfallSection?.bookshelfId ?: ""
    }

    private class RefreshCompleteCounter(
        private var pendingCount: Int,
        private val onComplete: () -> Unit
    ) {
        private var pagingComplete = false

        fun finishOne() {
            pendingCount--
            if (pendingCount == 0) {
                onComplete()
            }
        }

        fun finishPaging() {
            if (pagingComplete) return
            pagingComplete = true
            finishOne()
        }
    }
}
