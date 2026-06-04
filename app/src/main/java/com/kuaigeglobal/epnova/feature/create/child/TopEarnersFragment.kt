package com.kuaigeglobal.epnova.feature.create.child

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.FragmentTopEarnersBinding
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val REFRESH_COMPLETE_TIMEOUT_MS = 30_000L

class TopEarnersFragment : BaseViewModelFragment<FragmentTopEarnersBinding, TopEarnersViewModel>() {

    private val topEarnersAdapter by lazy {
        TopEarnersAdapter()
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
        topEarnersAdapter.refresh()
    }

    private suspend fun awaitRefreshComplete() {
        var loadingSeen = false
        topEarnersAdapter.loadStateFlow.first { loadStates ->
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
            hostAdapter = topEarnersAdapter,
            noMoreThreshold = 2
        ) {
            topEarnersAdapter.retry()
        }
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = topEarnersAdapter.withLoadStateFooter(footerAdapter)
        binding.lsv.startRefreshWithAdapter(topEarnersAdapter)
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getTopEarners().collect {
                topEarnersAdapter.submitData(it)
            }
        }
    }
}
