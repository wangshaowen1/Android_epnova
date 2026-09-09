package com.kuaigeglobal.epnova.feature.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.databinding.ItemNotificationBinding
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BindingViewHolder

class NotificationsAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) :
    PagingDataAdapter<NotificationItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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
            binding.root.isClickable = item != null && item.type != NotificationItem.TYPE_NONE
            if (item != null && item.type != NotificationItem.TYPE_NONE) {
                binding.root.setOnSingleClickListener { onItemClick(item) }
            }
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationItem>() {
            override fun areItemsTheSame(
                oldItem: NotificationItem,
                newItem: NotificationItem
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: NotificationItem,
                newItem: NotificationItem
            ): Boolean = oldItem == newItem
        }
    }
}
