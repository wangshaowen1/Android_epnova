package com.kuaigeglobal.epnova.feature.creator

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.ext.dp

class CreationHotCaseStateAdapter(
    private val retry: () -> Unit
) : RecyclerView.Adapter<CreationHotCaseStateAdapter.StateVH>() {

    private var loadState: LoadState = LoadState.Loading
    private var visible = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StateVH {
        return StateVH(
            LoadStateViewHolder(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    320.dp
                )
                setNoDataPrompt(parent.context.getString(R.string.creation_hot_case_empty))
                setRetryListener { retry() }
            }
        )
    }

    override fun onBindViewHolder(holder: StateVH, position: Int) {
        holder.bind(loadState)
    }

    override fun getItemCount(): Int = if (visible) 1 else 0

    fun update(loadState: LoadState, showState: Boolean) {
        val oldVisible = visible
        val oldLoadState = this.loadState
        visible = showState
        this.loadState = loadState
        when {
            oldVisible != visible -> notifyDataSetChanged()
            visible && oldLoadState != loadState -> notifyItemChanged(0)
        }
    }

    class StateVH(
        private val loadStateView: LoadStateViewHolder
    ) : RecyclerView.ViewHolder(loadStateView) {

        fun bind(loadState: LoadState) {
            when (loadState) {
                is LoadState.Error -> loadStateView.showRetry(loadState.error)
                is LoadState.Loading -> loadStateView.showLoading()
                is LoadState.NotLoading -> loadStateView.showEmpty()
            }
        }
    }
}
