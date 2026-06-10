package com.kuaigeglobal.epnova.feature.create.child

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.kuaigeglobal.epnova.databinding.FragmentResourceCenterBinding
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val REFRESH_COMPLETE_TIMEOUT_MS = 30_000L

class ResourceCenterFragment :
    BaseViewModelFragment<FragmentResourceCenterBinding, ResourceCenterViewModel>() {

    private val resourceCenterAdapter by lazy {
        ResourceCenterAdapter()
    }
    private var viewPrepared = false

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

        if (onComplete != null) {
            viewLifecycleOwner.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withTimeoutOrNull(REFRESH_COMPLETE_TIMEOUT_MS) {
                        awaitRefreshComplete()
                    }
                } finally {
                    onComplete()
                }
            }
        }
        resourceCenterAdapter.refresh()
    }

    private suspend fun awaitRefreshComplete() {
        var loadingSeen = false
        resourceCenterAdapter.loadStateFlow.first { loadStates ->
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
        val footerAdapter = MyMultiStatePagingFooterAdapter(
            hostAdapter = resourceCenterAdapter,
            noMoreThreshold = 12
        ) {
            resourceCenterAdapter.retry()
        }
        binding.rvNews.itemAnimator = null
        binding.rvNews.adapter = resourceCenterAdapter.withLoadStateFooter(footerAdapter)
        binding.lsv.startRefreshWithAdapter(resourceCenterAdapter)
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getResourceCenter().collect {
                resourceCenterAdapter.submitData(it)
            }
        }

        // 首次数据加载完成后，确保从第一条开始显示
        // loadStateFlow.first() 只触发一次：refresh 状态变为 NotLoading 且列表有数据时
        lifecycleScope.launch {
            resourceCenterAdapter.loadStateFlow.first { it.refresh is LoadState.NotLoading && resourceCenterAdapter.itemCount > 0 }
            binding.rvNews.scrollToPosition(0)
        }
    }
}
