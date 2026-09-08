package com.kuaigeglobal.epnova.feature.settings.feedback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.databinding.ItemFeedbackHistoryBinding
import com.nhsd.basic.item.BindingViewHolder

class FeedbackHistoryAdapter : PagingDataAdapter<FeedbackHistoryItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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
            binding.item = getItem(position)?.let(::ItemFeedbackHistoryVm)
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedbackHistoryItem>() {
            override fun areItemsTheSame(
                oldItem: FeedbackHistoryItem,
                newItem: FeedbackHistoryItem
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: FeedbackHistoryItem,
                newItem: FeedbackHistoryItem
            ): Boolean = oldItem == newItem
        }
    }
}
