package com.kuaigeglobal.epnova.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.setOnSingleClickListener
import com.kuaigeglobal.epnova.databinding.LayoutTrailingLoadMoreBinding

open class FooterAdapter(
    private val iconAlpha: Float = 1f,
    private val hostAdapter:RecyclerView.Adapter<*>?=null,
    private val retry: () -> Unit,
) : LoadStateAdapter<FooterAdapter.ViewHolder>() {

    var showReachedSize = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = LayoutTrailingLoadMoreBinding.bind(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
        val itemCount = hostAdapter?.itemCount ?: 0
        if (showReachedSize > 0 && itemCount < showReachedSize) {
            hideFooter(holder.binding)
        } else {
            hideFooter(holder.binding)
            holder.binding.loadMoreLoadingView.visibility =
                if (loadState is LoadState.Loading) View.VISIBLE else View.GONE
            holder.binding.loadMoreLoadEndView.visibility =
                if (loadState is LoadState.NotLoading && loadState.endOfPaginationReached) View.VISIBLE else View.GONE
        }
        holder.binding.loadMoreLoadFailSmallView.visibility = if (loadState is LoadState.Error) View.VISIBLE else View.GONE
        holder.binding.loadMoreLoadEmptyViewIcon.alpha = iconAlpha
        holder.binding.loadMoreLoadFailViewIcon.alpha = iconAlpha
    }


    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val holder = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.layout_trailing_load_more, parent, false)
        )
        holder.binding.loadMoreLoadFailSmallView.setOnSingleClickListener {
            retry.invoke()
        }
        return holder
    }

    //重写该方法，否则底部LoadStatus不展示，闪过
    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading ||
                loadState is LoadState.Error ||
                loadState is LoadState.NotLoading
    }

    private fun hideFooter(footBinding: LayoutTrailingLoadMoreBinding) {
        footBinding.loadMoreLoadEmptyView.visibility = View.GONE
        footBinding.loadMoreLoadFailView.visibility = View.GONE
        footBinding.loadMoreLoadingView.visibility = View.GONE
        footBinding.loadMoreLoadCompleteView.visibility = View.GONE
        footBinding.loadMoreLoadEndView.visibility = View.GONE
        footBinding.loadMoreLoadFailSmallView.visibility = View.GONE
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = true
        }
    }
}