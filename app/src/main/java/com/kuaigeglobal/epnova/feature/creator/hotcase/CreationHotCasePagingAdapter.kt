package com.kuaigeglobal.epnova.feature.creator.hotcase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.databinding.ItemCreationHotCaseBinding
import com.kuaigeglobal.epnova.databinding.ItemHotScriptTagBinding
import com.nhsd.basic.item.BaseItemEventsViewModel
import com.nhsd.basic.item.BindingViewHolder

class CreationHotCasePagingAdapter(
    private val itemClickVm: BaseItemEventsViewModel<CreationHotCaseEvent, CreationHotCaseInfo>
) : PagingDataAdapter<CreationHotCaseInfo, CreationHotCasePagingAdapter.ItemVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        return ItemVH(
            ItemCreationHotCaseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        holder.bind(position)
    }

    inner class ItemVH(
        binding: ItemCreationHotCaseBinding
    ) : BindingViewHolder<ItemCreationHotCaseBinding>(binding) {

        private val tagAdapter = HotScriptTagAdapter().also {
            binding.rvTags.adapter = it
        }

        override fun bind(position: Int) {
            val info = getItem(position)
            if (info == null) {
                tagAdapter.submitList(emptyList())
                binding.item = null
                binding.executePendingBindings()
                return
            }
            val viewModel = binding.getVm<ItemCreationHotCaseVM, CreationHotCaseInfo>(
                key = info.id,
                item = info,
                viewModelFactory = { ItemCreationHotCaseVM(info, itemClickVm) },
                initCallback = {}
            )
            viewModel.updateData(info)
            tagAdapter.submitList(info.tags.orEmpty())
            binding.item = viewModel
            binding.executePendingBindings()
        }
    }

    // 同款标签适配器
    private class HotScriptTagAdapter :
        ListAdapter<String, HotScriptTagAdapter.TagVH>(TAG_DIFF) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagVH {
            return TagVH(
                ItemHotScriptTagBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: TagVH, position: Int) {
            holder.bind(getItem(position))
        }

        class TagVH(
            private val binding: ItemHotScriptTagBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(tag: String) {
                binding.tag = tag
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CreationHotCaseInfo>() {
            override fun areItemsTheSame(oldItem: CreationHotCaseInfo, newItem: CreationHotCaseInfo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CreationHotCaseInfo, newItem: CreationHotCaseInfo): Boolean {
                return oldItem == newItem
            }
        }

        private val TAG_DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }
}
