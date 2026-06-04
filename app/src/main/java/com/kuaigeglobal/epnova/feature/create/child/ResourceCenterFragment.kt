package com.kuaigeglobal.epnova.feature.create.child

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.kuaigeglobal.epnova.databinding.FragmentResourceCenterBinding
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.CoroutineStart
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
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getResourceCenter().collect {
                resourceCenterAdapter.submitData(it)
            }
        }
    }
}
