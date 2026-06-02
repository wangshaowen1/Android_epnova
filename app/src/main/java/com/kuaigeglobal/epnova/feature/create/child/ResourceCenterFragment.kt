package com.kuaigeglobal.epnova.feature.create.child

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.kuaigeglobal.epnova.databinding.FragmentResourceCenterBinding
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.launch

class ResourceCenterFragment :
    BaseViewModelFragment<FragmentResourceCenterBinding, ResourceCenterViewModel>() {

    private val resourceCenterAdapter by lazy {
        ResourceCenterAdapter()
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
        resourceCenterAdapter.refresh()
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
        resourceCenterAdapter.addLoadStateListener {
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
            mRealVM.getResourceCenter().collect {
                resourceCenterAdapter.submitData(it)
            }
        }
    }
}
