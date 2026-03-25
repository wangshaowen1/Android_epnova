package com.kuaigeglobal.epnova.common.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.item.QuickItemViewModel
import com.nhsd.basic.item.QuickViewHolder

open class LoopQuickAdapter<T : QuickItemViewModel>(@LayoutRes var layoutId: Int, private val mData: List<T>) :
    RecyclerView.Adapter<QuickViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun refresh(){
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int) {
        val item=mData.getOrNull(position%mData.size)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder.create(layoutId, parent)
    }

    override fun getItemCount(): Int {
        return if(mData.isEmpty())0 else Integer.MAX_VALUE
    }
}