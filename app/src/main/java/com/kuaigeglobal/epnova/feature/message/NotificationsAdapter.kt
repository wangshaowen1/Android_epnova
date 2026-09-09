package com.kuaigeglobal.epnova.feature.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.core.network.apis.entity.SystemNoticeInfo
import com.kuaigeglobal.epnova.databinding.ItemNotificationBinding
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BindingViewHolder

class NotificationsAdapter(
    private val onItemClick: (SystemNoticeInfo) -> Unit
) :
    PagingDataAdapter<SystemNoticeInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(position)
        }
    }

    inner class ViewHolder(binding: ItemNotificationBinding) :
        BindingViewHolder<ItemNotificationBinding>(binding) {

        override fun bind(position: Int) {
            val item = getItem(position)
            val clickableItem = item?.takeIf { it.showViewDetail }
            binding.item = item?.let(::ItemNotificationVm)
            binding.root.setOnClickListener(null)
            binding.root.isClickable = clickableItem != null
            if (clickableItem != null) {
                binding.root.setOnSingleClickListener { onItemClick(clickableItem) }
            }
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SystemNoticeInfo>() {
            override fun areItemsTheSame(
                oldItem: SystemNoticeInfo,
                newItem: SystemNoticeInfo
            ): Boolean {
                return if (oldItem.messagesId != null || newItem.messagesId != null) {
                    oldItem.messagesId != null && oldItem.messagesId == newItem.messagesId
                } else {
                    oldItem.createTime != null && oldItem.createTime == newItem.createTime
                }
            }

            override fun areContentsTheSame(
                oldItem: SystemNoticeInfo,
                newItem: SystemNoticeInfo
            ): Boolean = oldItem == newItem
        }
    }
}
