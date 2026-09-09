package com.kuaigeglobal.epnova.feature.settings.feedback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.databinding.ItemFeedbackHistoryBinding
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BindingViewHolder

class FeedbackHistoryAdapter(
    private val onItemClick: (FeedbackHistoryItem) -> Unit
) : PagingDataAdapter<FeedbackHistoryItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemFeedbackHistoryBinding.inflate(
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

    inner class ViewHolder(binding: ItemFeedbackHistoryBinding) :
        BindingViewHolder<ItemFeedbackHistoryBinding>(binding) {

        override fun bind(position: Int) {
            val item = getItem(position)
            binding.item = item?.let(::ItemFeedbackHistoryVm)
            binding.photo.setAll(item?.imageUrls ?: emptyList())
            binding.root.setOnClickListener(null)
            binding.root.isClickable = item != null
            if (item != null) {
                binding.root.setOnSingleClickListener { onItemClick(item) }
            }
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedbackHistoryItem>() {
            override fun areItemsTheSame(
                oldItem: FeedbackHistoryItem,
                newItem: FeedbackHistoryItem
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: FeedbackHistoryItem,
                newItem: FeedbackHistoryItem
            ): Boolean = oldItem == newItem
        }
    }
}
