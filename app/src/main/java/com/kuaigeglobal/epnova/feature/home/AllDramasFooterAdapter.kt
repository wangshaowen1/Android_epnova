package com.kuaigeglobal.epnova.feature.home

import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.kuaigeglobal.epnova.widget.stateful.paging.MultiStatePagingFooterAdapter

/** 复用公共页脚布局和重试事件，承接横滑区块及分页的局部失败。 */
class AllDramasFooterAdapter(
    onRetry: () -> Unit
) : MultiStatePagingFooterAdapter() {

    init {
        refresh = onRetry
    }

    override fun onBindViewHolder(holder: MultiStateVH, loadState: LoadState) {
        holder.multiStateAdapterViewBinding.apply {
            llLoadMoreLoading.isVisible = loadState is LoadState.Loading
            llLoadMoreLoadFailSmall.isVisible = loadState is LoadState.Error
            loadMoreLoadEnd.isVisible = loadState is LoadState.NotLoading && loadState.endOfPaginationReached
            llNullView.isVisible = false
        }
    }

    override fun onViewAttachedToWindow(holder: MultiStateVH) {
        super.onViewAttachedToWindow(holder)
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
    }
}
