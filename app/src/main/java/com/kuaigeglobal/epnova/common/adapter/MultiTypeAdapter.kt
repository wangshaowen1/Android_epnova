package com.kuaigeglobal.epnova.common.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.chad.library.adapter.base.BaseQuickAdapter
import com.nhsd.basic.item.QuickItemViewModel
import com.nhsd.basic.item.QuickViewHolder


/**
 * 多视图适配器
 */
class MultiTypeAdapter<T : QuickItemViewModel>(private val viewType: ViewType<T>) : BaseQuickAdapter<T, QuickViewHolder>() {
    override fun getItemViewType(position: Int, list: List<T>): Int {
        return viewType.getViewType(position, getItem(position))
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: T?) {
        holder.bind(item)
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder.create(this.viewType.getLayoutId(viewType), parent)
    }

}

interface ViewType<T : QuickItemViewModel> {
    fun getViewType(position: Int, item: T?): Int

    @LayoutRes
    fun getLayoutId(viewType: Int): Int
}

class DefaultViewType<T : QuickItemViewModel>(@LayoutRes private val layoutId: Int) : ViewType<T> {
    override fun getViewType(position: Int, item: T?): Int {
        return 1
    }

    override fun getLayoutId(viewType: Int): Int {
        return layoutId
    }
}