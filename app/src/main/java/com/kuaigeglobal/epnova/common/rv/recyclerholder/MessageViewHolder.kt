package com.kuaigeglobal.epnova.common.rv.recyclerholder

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class MessageViewHolder<V : ViewBinding,Message>(protected var binding: V) :
    RecyclerView.ViewHolder(binding.root) {


    abstract fun bind(data: Message)
}