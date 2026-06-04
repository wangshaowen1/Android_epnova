package com.kuaigeglobal.epnova.feature.create.child

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.OfficialConsultationInfo
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import com.kuaigeglobal.epnova.databinding.ItemResourceCenterBinding
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.item.BindingViewHolder

class ResourceCenterAdapter : PagingDataAdapter<OfficialConsultationInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemResourceCenterBinding.inflate(
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

    inner class ViewHolder(binding: ItemResourceCenterBinding) :
        BindingViewHolder<ItemResourceCenterBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemResourceCenterVm, OfficialConsultationInfo>(
                    key = item.officialConsultationId,
                    item = item,
                    viewModelFactory = {
                        ItemResourceCenterVm(item)
                    },
                ).apply {
                    onItemClick.value = {
                        AppRouter.jump(item.jumpUri)
                    }
                }
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<OfficialConsultationInfo>() {
            override fun areItemsTheSame(
                oldItem: OfficialConsultationInfo,
                newItem: OfficialConsultationInfo
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: OfficialConsultationInfo,
                newItem: OfficialConsultationInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}
