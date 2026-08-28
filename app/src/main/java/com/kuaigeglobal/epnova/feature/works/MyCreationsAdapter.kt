package com.kuaigeglobal.epnova.feature.works

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.databinding.ItemMyCreationsBinding
import com.nhsd.basic.item.BindingViewHolder

class MyCreationsAdapter(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>
) : PagingDataAdapter<WorkInformationInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMyCreationsBinding.inflate(
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

    inner class ViewHolder(binding: ItemMyCreationsBinding) :
        BindingViewHolder<ItemMyCreationsBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemMyWorkVM, CreatorWorkInfo>(
                    key = item.workId,
                    item = item.work,
                    viewModelFactory = {
                        ItemMyWorkVM(eventVm, item.work)
                    },
                    initCallback = {}
                )
                viewModel.updateData(item.work)
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkInformationInfo>() {
            override fun areItemsTheSame(
                oldItem: WorkInformationInfo,
                newItem: WorkInformationInfo
            ): Boolean {
                return oldItem.workId == newItem.workId
            }

            override fun areContentsTheSame(
                oldItem: WorkInformationInfo,
                newItem: WorkInformationInfo
            ): Boolean {
                return oldItem.work == newItem.work
            }
        }
    }
}
