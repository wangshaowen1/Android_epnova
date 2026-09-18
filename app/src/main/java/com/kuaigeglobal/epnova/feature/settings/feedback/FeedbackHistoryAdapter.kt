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

/** 反馈历史分页适配器，负责条目绑定，并向页面转发有有效反馈 ID 的点击。 */
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
            // 无图片时也传入空列表，清除 ViewHolder 复用时可能残留的上一条图片。
            binding.photo.setPhotoUrls(item?.imagesUrls.orEmpty())
            val clickableItem = item?.takeIf { !it.feedbackId.isNullOrBlank() }
            // 先清除复用的点击回调，再按当前条目是否有有效 ID 重新设置。
            binding.root.setOnClickListener(null)
            binding.root.isClickable = clickableItem != null
            if (clickableItem != null) {
                binding.root.setOnSingleClickListener { onItemClick(clickableItem) }
            }
            binding.executePendingBindings()
        }
    }

    companion object {
        // 优先用反馈业务 ID 识别条目，只有两条记录都缺少 ID 时才用创建时间兜底。
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
