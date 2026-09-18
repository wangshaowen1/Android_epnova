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
        // 页脚的失败重试交给页面处理，页面可据此统一重试相关数据源。
        refresh = onRetry
    }

    /** 按分页状态切换加载、失败和结束视图，空列表由页面级状态容器处理。 */
    override fun onBindViewHolder(holder: MultiStateVH, loadState: LoadState) {
        holder.multiStateAdapterViewBinding.apply {
            llLoadMoreLoading.isVisible = loadState is LoadState.Loading
            llLoadMoreLoadFailSmall.isVisible = loadState is LoadState.Error
            loadMoreLoadEnd.isVisible = loadState is LoadState.NotLoading && loadState.endOfPaginationReached
            llNullView.isVisible = false
        }
    }

    /** 让页脚在瀑布流布局中占满整行，避免只占一个网格列。 */
    override fun onViewAttachedToWindow(holder: MultiStateVH) {
        super.onViewAttachedToWindow(holder)
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
    }
}
