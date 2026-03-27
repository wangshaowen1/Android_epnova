package com.kuaigeglobal.epnova.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.ItemBookShelfChildBinding
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.nhsd.basic.item.BindingViewHolder

class InnerHorizontalAdapter : ListAdapter<WorkVOInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    init {
        // 优化：防止数据更新期间（短暂的 empty 状态）丢失横向滚动位置
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBookShelfChildBinding.inflate(
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

    inner class ViewHolder(binding: ItemBookShelfChildBinding) : BindingViewHolder<ItemBookShelfChildBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemHomeDramaVm::class.java).apply {
                    onItemClick.value = {
                        DramasPlayPagerActivity.start(workId = item.workId, 0, false)
                    }
                }
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkVOInfo>() {
            override fun areItemsTheSame(oldItem: WorkVOInfo, newItem: WorkVOInfo): Boolean {
                // 优化：使用业务上的唯一 ID 判断是不是同一个卡片，避免乱用 HashCode
                return oldItem.workId == newItem.workId
            }

            override fun areContentsTheSame(oldItem: WorkVOInfo, newItem: WorkVOInfo): Boolean {
                // 如果后端返回的数据模型重写了 equals，这里直接用 == 是没问题的
                return oldItem == newItem
            }
        }
    }
}