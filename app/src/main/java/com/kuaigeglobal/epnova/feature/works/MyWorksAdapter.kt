package com.kuaigeglobal.epnova.feature.works

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.databinding.ItemMyWorkBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品
 * @Data: 2026/2/3/周二  9:17
 * @Version: 1.0
 * *****************************************
 */
class MyWorksAdapter(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>
) : PagingDataAdapter<WorkInformationInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMyWorkBinding.inflate(
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


    inner class ViewHolder(binding: ItemMyWorkBinding) :
        BindingViewHolder<ItemMyWorkBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemMyWorkVM, WorkInfo>(
                    key = item.workInformationID,
                    item = item.work,
                    viewModelFactory = {
                        ItemMyWorkVM(eventVm, item.work)
                    },
                    initCallback = {}
                )
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
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: WorkInformationInfo,
                newItem: WorkInformationInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}