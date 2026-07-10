package com.kuaigeglobal.epnova.feature.creator.hotcase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.databinding.ItemCreationHotCaseBinding
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

        override fun bind(position: Int) {
            val info = getItem(position)
            if (info == null) {
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
            binding.item = viewModel
            binding.executePendingBindings()
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
    }
}
