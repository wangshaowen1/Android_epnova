package com.kuaigeglobal.epnova.feature.mine.child

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.ItemMineChildBinding
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.nhsd.basic.item.BindingViewHolder


class MineChildAdapter(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksVOEvent, String>,
    val collect: Boolean
) : PagingDataAdapter<WorkVOInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMineChildBinding.inflate(
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


    inner class ViewHolder(binding: ItemMineChildBinding) :
        BindingViewHolder<ItemMineChildBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemMineChildVM, WorkVOInfo>(
                    key = item.workId,
                    item = item,
                    viewModelFactory = {
                        ItemMineChildVM(eventVm, item, collect)
                    },
                    initCallback = {}
                )
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkVOInfo>() {
            override fun areItemsTheSame(
                oldItem: WorkVOInfo,
                newItem: WorkVOInfo
            ): Boolean {
                return oldItem.workId == newItem.workId
            }

            override fun areContentsTheSame(
                oldItem: WorkVOInfo,
                newItem: WorkVOInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}