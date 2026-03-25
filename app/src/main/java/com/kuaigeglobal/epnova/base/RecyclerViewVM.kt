package com.kuaigeglobal.epnova.base

import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.widget.NoAlphaItemAnimator

open class RecyclerViewVM {
    fun notifyDataSetChanged() {
        mAdapter.get()?.notifyDataSetChanged()
    }

    var mAdapter: ObservableField<RecyclerView.Adapter<out RecyclerView.ViewHolder>> =
        ObservableField()

    var mScrollToTop = ObservableField(false)
    var mOnScrollListener:ObservableField<RecyclerView.OnScrollListener> = ObservableField(RvScrollListener())

    var mItemAnimator:ObservableField<RecyclerView.ItemAnimator> = ObservableField(NoAlphaItemAnimator())

    var mOnLoadMoreListener = {}

    var mItemDecoration: ObservableField<RecyclerView.ItemDecoration> = ObservableField()


    inner class Builder {

        fun setAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>): Builder {
            mAdapter.set(adapter)
            return this
        }
        fun setQuickAdapter(resLayoutId:Int, data:List<AppItemViewModel<*>>): Builder {
            mAdapter.set(QuickAdapter(resLayoutId, data))
            return this
        }
        fun setScrollToTop(scrollToTop: Boolean): Builder {
            mScrollToTop.set(scrollToTop)
            return this
        }
        fun setOnScrollListener(onScrollListener: RecyclerView.OnScrollListener): Builder {
            mOnScrollListener.set(onScrollListener)
            return this
        }
        fun setItemAnimator(itemAnimator: RecyclerView.ItemAnimator): Builder {
            mItemAnimator.set(itemAnimator)
            return this
        }
        fun setOnLoadMoreListener(onLoadMoreListener: () -> Unit): Builder {
            mOnLoadMoreListener = onLoadMoreListener
            return this
        }
        fun setItemDecoration(itemDecoration: RecyclerView.ItemDecoration): Builder {
            mItemDecoration.set(itemDecoration)
            return this
        }
        fun build(): RecyclerViewVM {
            return this@RecyclerViewVM
        }
    }

}

open class RvScrollListener : RecyclerView.OnScrollListener()