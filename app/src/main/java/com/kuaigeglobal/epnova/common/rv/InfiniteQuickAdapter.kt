package com.kuaigeglobal.epnova.common.rv

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.chad.library.adapter.base.BaseQuickAdapter
import com.nhsd.basic.item.QuickItemViewModel
import com.nhsd.basic.item.QuickViewHolder

open class InfiniteQuickAdapter<T : QuickItemViewModel>(@LayoutRes var layoutId: Int, mData: List<T>) :
    BaseQuickAdapter<T, QuickViewHolder>(mData) {

    override fun getItemCount(items: List<T>): Int {
        return if (items.isEmpty()) 0 else Int.MAX_VALUE
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: T?) = holder.bind(getItem(position % items.size))

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder = QuickViewHolder.create(layoutId, parent)

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        notifyDataSetChanged()
    }

    // 初始偏移量，确保可以从中间开始滑动
    private var startOffset: Int = Int.MAX_VALUE / 2

    /**
     * 获取初始偏移后的起始位置（用于首次加载时居中显示）
     */
    fun getStartPosition(): Int {
        return startOffset - (startOffset % items.size)
    }

    /**
     * 获取实际数据的位置（用于外部定位）
     */
    fun getRealPosition(adapterPosition: Int): Int {
        return adapterPosition % items.size
    }
}