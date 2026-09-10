package com.kuaigeglobal.epnova.feature.settings.feedback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.core.network.apis.entity.FeedbackPageInfo
import com.kuaigeglobal.epnova.databinding.ItemFeedbackHistoryBinding
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BindingViewHolder

class FeedbackHistoryAdapter(
    private val onItemClick: (FeedbackPageInfo) -> Unit
) : PagingDataAdapter<FeedbackPageInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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
            binding.photo.setPhotoUrls(item?.imagesUrl.orEmpty())
            val clickableItem = item?.takeIf { !it.feedbackId.isNullOrBlank() }
            binding.root.setOnClickListener(null)
            binding.root.isClickable = clickableItem != null
            if (clickableItem != null) {
                binding.root.setOnSingleClickListener { onItemClick(clickableItem) }
            }
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedbackPageInfo>() {
            override fun areItemsTheSame(
                oldItem: FeedbackPageInfo,
                newItem: FeedbackPageInfo
            ): Boolean {
                return if (oldItem.feedbackId != null || newItem.feedbackId != null) {
                    oldItem.feedbackId != null && oldItem.feedbackId == newItem.feedbackId
                } else {
                    oldItem.createTime != null && oldItem.createTime == newItem.createTime
                }
            }

            override fun areContentsTheSame(
                oldItem: FeedbackPageInfo,
                newItem: FeedbackPageInfo
            ): Boolean = oldItem == newItem
        }
    }
}
