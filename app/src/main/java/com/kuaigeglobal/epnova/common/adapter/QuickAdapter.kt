package com.kuaigeglobal.epnova.common.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.chad.library.adapter.base.BaseQuickAdapter
import com.nhsd.basic.item.QuickItemViewModel
import com.nhsd.basic.item.QuickViewHolder

open class QuickAdapter<T : QuickItemViewModel>(@LayoutRes var layoutId: Int, mData: List<T>) :
    BaseQuickAdapter<T, QuickViewHolder>(mData) {

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: T?) =holder.bind(item)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup,viewType: Int): QuickViewHolder = QuickViewHolder.create(layoutId, parent)

    @SuppressLint("NotifyDataSetChanged")
    fun refresh(){
        notifyDataSetChanged()
    }
}