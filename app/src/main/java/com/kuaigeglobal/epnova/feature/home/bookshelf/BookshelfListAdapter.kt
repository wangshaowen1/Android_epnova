package com.kuaigeglobal.epnova.feature.home.bookshelf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.ItemNewReleaseBinding
import com.kuaigeglobal.epnova.feature.home.ItemHomeDramaVm
import com.nhsd.basic.item.BindingViewHolder

class BookshelfListAdapter(
    private val onItemClick: (WorkVOInfo) -> Unit
) : PagingDataAdapter<WorkVOInfo, BookshelfListAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNewReleaseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(
        private val binding: ItemNewReleaseBinding
    ) : BindingViewHolder<ItemNewReleaseBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { work ->
                binding.item = ItemHomeDramaVm(work).apply {
                    onItemClick.value = { onItemClick(work) }
                }
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkVOInfo>() {
            override fun areItemsTheSame(oldItem: WorkVOInfo, newItem: WorkVOInfo): Boolean {
                return oldItem.workId == newItem.workId
            }

            override fun areContentsTheSame(oldItem: WorkVOInfo, newItem: WorkVOInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
