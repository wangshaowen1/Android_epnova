package com.kuaigeglobal.epnova.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.loadState.LoadState
import com.chad.library.adapter.base.loadState.trailing.TrailingLoadStateAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.utils.NetworkUtil
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.databinding.LayoutTrailingLoadMoreBinding
import com.nhsd.basic.ext.getResString

fun failState(dataAdapter: RecyclerView.Adapter<*>, error: Throwable) = run {
    val stateError = if (dataAdapter.itemCount > 0) error else DefaultTrailingLoadStateAdapter.NoDataFailException(error)
    LoadState.Error(stateError)
}

/**
 * 加载更多
 * @author xiaoowuu
 */
open class DefaultTrailingLoadStateAdapter(isLoadEndDisplay: Boolean = true) :
    TrailingLoadStateAdapter<DefaultTrailingLoadStateAdapter.TrailingLoadStateVH>(isLoadEndDisplay) {

    var loadEndMsg = R.string.no_more_data.getResString()

    class NoDataException : Throwable()

    class NoDataFailException(val error: Throwable?) : Throwable()

    /**
     * 默认实现的"加载更多" RecommendPlaylistViewHolder
     */
    class TrailingLoadStateVH(
        parent: ViewGroup,
        val viewBinding: LayoutTrailingLoadMoreBinding = LayoutTrailingLoadMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) : RecyclerView.ViewHolder(viewBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): TrailingLoadStateVH {
        return TrailingLoadStateVH(parent).apply {
            viewBinding.loadMoreLoadFailViewRetry.setOnClickListener {
                // 失败重试点击事件
                // retry when loaded failed.
                invokeFailRetry()
            }
            viewBinding.loadMoreLoadFailSmallView.setOnClickListener {
                // 失败重试点击事件
                // retry when loaded failed.
                invokeFailRetry()
            }
            viewBinding.loadMoreLoadCompleteView.setOnClickListener {
                // 加载更多，手动点击事件
                // manual click to load more.
                invokeLoadMore()
            }
        }
    }

    /**
     * bind LoadState
     *
     * 绑定加载状态
     */
    override fun onBindViewHolder(holder: TrailingLoadStateVH, loadState: LoadState) {
        when (loadState) {
            is LoadState.NotLoading -> {
                if (loadState.endOfPaginationReached) {
                    holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadFailSmallView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadEndView.visibility = if (isLoadEndDisplay) View.VISIBLE else View.GONE
                    holder.viewBinding.loadMoreLoadEndViewText.text = loadEndMsg
                    holder.viewBinding.loadMoreLoadEmptyView.visibility = View.GONE
                } else {
                    holder.viewBinding.loadMoreLoadCompleteView.visibility = View.VISIBLE
                    holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadFailSmallView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadEmptyView.visibility = View.GONE
                }
            }

            is LoadState.Loading -> {
                holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadingView.visibility = View.VISIBLE
                holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadFailSmallView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadEmptyView.visibility = View.GONE
            }

            is LoadState.Error -> {
                holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
                when (val error = loadState.error) {
                    is NoDataException -> {
                        holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                        holder.viewBinding.loadMoreLoadFailSmallView.visibility = View.GONE
                        holder.viewBinding.loadMoreLoadEmptyView.visibility = View.VISIBLE
                    }

                    is NoDataFailException -> {
                        holder.viewBinding.loadMoreLoadFailView.visibility = View.VISIBLE
                        holder.viewBinding.loadMoreLoadFailSmallView.visibility = View.GONE
                        holder.viewBinding.loadMoreLoadEmptyView.visibility = View.GONE
                        val msg = if (!NetworkUtil.isNetworkAvailable()) {
                          R.string.page_network_error.getResString()
                        } else {
                            (error.error as? ApiException)?.message
                        }
                        holder.viewBinding.loadMoreLoadFailViewText.text = if (msg.isNullOrEmpty()) R.string.page_load_error.getResString() else msg
                    }

                    else -> {
                        holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                        holder.viewBinding.loadMoreLoadFailSmallView.visibility = View.VISIBLE
                        holder.viewBinding.loadMoreLoadEmptyView.visibility = View.GONE
                    }
                }
            }

            is LoadState.None -> {
                holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadFailSmallView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadEmptyView.visibility = View.GONE
            }
        }
    }

    override fun getStateViewType(loadState: LoadState): Int = R.layout.layout_trailing_load_more
}