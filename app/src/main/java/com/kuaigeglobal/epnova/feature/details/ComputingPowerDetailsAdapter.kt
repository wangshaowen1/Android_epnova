package com.kuaigeglobal.epnova.feature.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.response.ComputingPowerDetailsInfo
import com.kuaigeglobal.epnova.databinding.ItemComputingPowerDetailsBinding
import com.kuaigeglobal.epnova.feature.details.child.ComputingPowerDetailsTabFragment
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:充值明细
 * @Data: 2025/9/10/周三  17:52
 * @Version: 1.0
 * *****************************************
 */
class ComputingPowerDetailsAdapter(
    private val assetType: Int = ComputingPowerDetailsTabFragment.ASSET_TYPE_COINS
) : PagingDataAdapter<ComputingPowerDetailsInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemComputingPowerDetailsBinding.inflate(
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


    inner class ViewHolder(binding: ItemComputingPowerDetailsBinding) :
        BindingViewHolder<ItemComputingPowerDetailsBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemComputingPowerDetailsVm::class.java)
                transVM.assetType = assetType
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ComputingPowerDetailsInfo>() {
            override fun areItemsTheSame(
                oldItem: ComputingPowerDetailsInfo,
                newItem: ComputingPowerDetailsInfo
            ): Boolean {
                val oldCode = oldItem.pointDetailCode ?: oldItem.coinsDetailCode
                val newCode = newItem.pointDetailCode ?: newItem.coinsDetailCode
                return if (oldCode != null && newCode != null) {
                    oldCode == newCode
                } else {
                    oldItem == newItem
                }
            }

            override fun areContentsTheSame(
                oldItem: ComputingPowerDetailsInfo,
                newItem: ComputingPowerDetailsInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}