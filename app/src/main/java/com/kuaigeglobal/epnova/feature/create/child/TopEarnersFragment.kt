package com.kuaigeglobal.epnova.feature.create.child

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.FragmentTopEarnersBinding
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.launch

class TopEarnersFragment : BaseViewModelFragment<FragmentTopEarnersBinding, TopEarnersViewModel>() {

    private val topEarnersAdapter by lazy {
        TopEarnersAdapter()
    }
    private var refreshComplete: (() -> Unit)? = null
    private var prepared = false

    override fun onPrepareView() {
        super.onPrepareView()
        prepared = true
        initView()
        dataUpdate()
    }

    override fun onDestroy() {
        prepared = false
        refreshComplete = null
        super.onDestroy()
    }

    fun refresh(onComplete: (() -> Unit)? = null) {
        if (!prepared) {
            onComplete?.invoke()
            return
        }
        refreshComplete = onComplete
        topEarnersAdapter.refresh()
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
        topEarnersAdapter.addLoadStateListener {
            when (it.refresh) {
                is LoadState.Error,
                is LoadState.NotLoading -> {
                    refreshComplete?.invoke()
                    refreshComplete = null
                }

                else -> {}
            }
        }
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getTopEarners().collect {
                topEarnersAdapter.submitData(it)
            }
        }
    }
}
