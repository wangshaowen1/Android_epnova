package com.kuaigeglobal.epnova.common.ext

import android.view.ViewGroup
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.shizhefei.view.coolrefreshview.CoolRefreshView
import com.shizhefei.view.coolrefreshview.SimpleOnPullListener

fun SmartRefreshLayout.startRefreshWithAdapter(
    pagingDataAdapter: PagingDataAdapter<*, *>,
    loadStateHolder: LoadStateViewHolder? = null,
    onRefreshListener: (() -> Unit)? = null,
    addExtraLoadStateListener: ((CombinedLoadStates) -> Unit)? = null
) {
    val contentSwitcher = loadStateHolder ?: this.findLoadStateView()
    setOnRefreshListener {
        onRefreshListener?.invoke()
        pagingDataAdapter.refresh()
    }
    //是否启用下拉刷新
    setEnableRefresh(true)
    //是否启用上拉加载功能
    setEnableLoadMore(false)

    pagingDataAdapter.addLoadStateListener {
        addExtraLoadStateListener?.invoke(it)
        when (it.refresh) {
            is LoadState.Error -> {
                val errorState = it.refresh as? LoadState.Error
                contentSwitcher?.showErrorWithToast(errorState?.error,pagingDataAdapter.itemCount>0)
                finishRefresh()
            }

            is LoadState.NotLoading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    contentSwitcher?.showEmpty()
                } else {
                    contentSwitcher?.showContent()
                }
                if (this.isRefreshing) {
                    contentSwitcher?.findRecyclerChild()?.smoothScrollToPosition(0)
                }
                finishRefresh()
            }

            is LoadState.Loading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    if (!isRefreshing) {
                        //如果正在刷新中，就不需要显示加载的动画了
                        contentSwitcher?.showLoading()
                    }
                } else {
                    contentSwitcher?.showContent()
                }
            }
        }
    }
    contentSwitcher?.setRetryListener {
        pagingDataAdapter.refresh()
    }
    contentSwitcher?.showLoading()
}

fun ViewGroup.findRecyclerChild(): RecyclerView? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is RecyclerView) {
            return child
        }
    }
    return null
}

fun ViewGroup.findLoadStateView(): LoadStateViewHolder? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        // 检查当前子视图是否是目标类型
        if (child is LoadStateViewHolder) {
            return child
        }
        // 若子视图是 ViewGroup 则递归查找
        if (child is ViewGroup) {
            val nestedView = child.findLoadStateView()
            if (nestedView != null) {
                return nestedView
            }
        }
    }
    return null
}

fun SwipeRefreshLayout.startRefreshWithAdapter(pagingDataAdapter: PagingDataAdapter<*, *>) {
    var contentSwitcher: LoadStateViewHolder? = null
    if (childCount > 1 && getChildAt(1) is LoadStateViewHolder) {
        contentSwitcher = getChildAt(1) as LoadStateViewHolder
    }
    setOnRefreshListener {
        pagingDataAdapter.refresh()
    }
    pagingDataAdapter.addLoadStateListener {
        //binding.swipeRefreshLayout.isRefreshing = it.refresh is LoadState.Loading
        when (it.refresh) {
            is LoadState.Error -> {
                this.isRefreshing = false
                val errorState=it.refresh as? LoadState.Error
                contentSwitcher?.showErrorWithToast(errorState?.error,pagingDataAdapter.itemCount>0)
            }

            is LoadState.NotLoading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    contentSwitcher?.showEmpty()
                } else {
                    contentSwitcher?.showContent()
                }
                if (this.isRefreshing) {
                    contentSwitcher?.findRecyclerChild()?.smoothScrollToPosition(0)
                }
                this.isRefreshing = false
            }

            is LoadState.Loading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    contentSwitcher?.showLoading()
                } else {
                    contentSwitcher?.showContent()
                }
            }


            else -> {
                this.isRefreshing = false
            }
        }
    }
    contentSwitcher?.setRetryListener {
        pagingDataAdapter.refresh()
    }
//    pagingDataAdapter.refresh()
    contentSwitcher?.showLoading()
}

fun LoadStateViewHolder?.startRefreshWithAdapter(
    pagingDataAdapter: PagingDataAdapter<*, *>,
) {
    val contentSwitcher = this
    pagingDataAdapter.addLoadStateListener {
        when (it.refresh) {
            is LoadState.Error -> {
                val errorState=it.refresh as? LoadState.Error
                contentSwitcher?.showErrorWithToast(errorState?.error,pagingDataAdapter.itemCount>0)
            }

            is LoadState.NotLoading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    contentSwitcher?.showEmpty()
                } else {
                    contentSwitcher?.showContent()
                }
            }

            is LoadState.Loading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    contentSwitcher?.showLoading()
                } else {
                    contentSwitcher?.showContent()
                }
            }

        }
    }
    contentSwitcher?.setRetryListener {
        pagingDataAdapter.refresh()
    }
    contentSwitcher?.showLoading()
}

fun CoolRefreshView.startRefreshWithAdapter(
    pagingDataAdapter: PagingDataAdapter<*, *>,
    addExtraLoadStateListener: ((CombinedLoadStates) -> Unit)? = null,
    loadStateHolder: LoadStateViewHolder? = null
) {
    val contentSwitcher = loadStateHolder ?: this.findLoadStateView()
    addOnPullListener(object : SimpleOnPullListener() {
        override fun onRefreshing(p0: CoolRefreshView?) {
            pagingDataAdapter.refresh()
        }
    })
    pagingDataAdapter.addLoadStateListener {
        addExtraLoadStateListener?.invoke(it)
        Logger.i("loadState change :${it.refresh}")
        when (it.refresh) {
            is LoadState.Error -> {
                this.isRefreshing = false
                val errorState=it.refresh as? LoadState.Error
                contentSwitcher?.showErrorWithToast(errorState?.error,pagingDataAdapter.itemCount>0)
            }

            is LoadState.NotLoading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    contentSwitcher?.showEmpty()
                } else {
                    contentSwitcher?.showContent()
                }
                if (this.isRefreshing) {
                    contentSwitcher?.findRecyclerChild()?.smoothScrollToPosition(0)
                }
                this.isRefreshing = false
            }

            is LoadState.Loading -> {
                if (pagingDataAdapter.itemCount == 0) {
                    contentSwitcher?.showLoading()
                } else {
                    contentSwitcher?.showContent()
                }
            }

        }
    }
    contentSwitcher?.setRetryListener {
        pagingDataAdapter.refresh()
    }
    contentSwitcher?.showLoading()
}


fun LoadStateViewHolder.showErrorWithToast(e: Throwable?,showContent: Boolean=true) {
    if (showContent) {
        showContent()
        // 显示 Toast
        e?.let {
            parseMessage(it)?.showToast()
        }
    } else {
        showRetry(e)
    }
}

fun ConcatAdapter.getAdapterByItemPosition(position: Int): RecyclerView.Adapter<out RecyclerView.ViewHolder>? {
    var pos = position
    val adapters = adapters
    for (adapter in adapters) {
        when {
            pos >= adapter.itemCount -> pos -= adapter.itemCount
            pos < 0 -> return null
            else -> return adapter
        }
    }
    return null
}
