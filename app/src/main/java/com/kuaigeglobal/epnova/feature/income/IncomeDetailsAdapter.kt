package com.kuaigeglobal.epnova.feature.income

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeDetailInfo
import com.kuaigeglobal.epnova.databinding.ItemIncomeDetailsBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * 用户收益明细分页适配器。
 */
class IncomeDetailsAdapter : PagingDataAdapter<IncomeDetailInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemIncomeDetailsBinding.inflate(
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

    inner class ViewHolder(binding: ItemIncomeDetailsBinding) :
        BindingViewHolder<ItemIncomeDetailsBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                binding.item = binding.getVm<ItemIncomeDetailsVM, IncomeDetailInfo>(
                    key = getItemKey(item),
                    item = item,
                    viewModelFactory = { ItemIncomeDetailsVM(item) }
                )
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<IncomeDetailInfo>() {
            override fun areItemsTheSame(
                oldItem: IncomeDetailInfo,
                newItem: IncomeDetailInfo
            ): Boolean {
                return getItemKey(oldItem) == getItemKey(newItem)
            }

            override fun areContentsTheSame(
                oldItem: IncomeDetailInfo,
                newItem: IncomeDetailInfo
            ): Boolean {
                return oldItem == newItem
            }
        }

        private fun getItemKey(item: IncomeDetailInfo): Any {
            return item.incomeDetailId ?: "${item.workId}_${item.storyId}_${item.incomeTime}"
        }
    }
}
