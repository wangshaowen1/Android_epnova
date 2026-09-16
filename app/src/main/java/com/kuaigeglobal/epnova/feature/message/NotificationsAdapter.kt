package com.kuaigeglobal.epnova.feature.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.core.network.apis.entity.MessagePageInfo
import com.kuaigeglobal.epnova.databinding.ItemNotificationBinding
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BindingViewHolder

class NotificationsAdapter(
    private val onItemClick: (MessagePageInfo) -> Unit
) :
    PagingDataAdapter<MessagePageInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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
            binding.item = item?.let(::ItemNotificationVm)
            binding.root.setOnClickListener(null)
            binding.root.isClickable = item != null
            if (item != null) {
                binding.root.setOnSingleClickListener { onItemClick(item) }
            }
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MessagePageInfo>() {
            override fun areItemsTheSame(
                oldItem: MessagePageInfo,
                newItem: MessagePageInfo
            ): Boolean {
                return if (oldItem.messageId != null || newItem.messageId != null) {
                    oldItem.messageId != null && oldItem.messageId == newItem.messageId
                } else {
                    oldItem.sendTime != null && oldItem.sendTime == newItem.sendTime
                }
            }

            override fun areContentsTheSame(
                oldItem: MessagePageInfo,
                newItem: MessagePageInfo
            ): Boolean = oldItem == newItem
        }
    }
}
