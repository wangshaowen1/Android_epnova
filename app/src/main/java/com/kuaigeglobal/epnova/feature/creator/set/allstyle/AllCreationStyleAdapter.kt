package com.kuaigeglobal.epnova.feature.creator.set.allstyle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoStyleConfigInfo
import com.kuaigeglobal.epnova.databinding.ItemCreationStyleAllBinding
import com.nhsd.basic.item.BindingViewHolder

class AllCreationStyleAdapter(val lastStyleId: String?) : PagingDataAdapter<VideoStyleConfigInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCreationStyleAllBinding.inflate(
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

    var itemVm: ItemCreationStyleVm? = null

    inner class ViewHolder(binding: ItemCreationStyleAllBinding) :
        BindingViewHolder<ItemCreationStyleAllBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemCreationStyleVm::class.java)
                binding.item = transVM.apply {
                    if (itemVm == null && item.styleCode == lastStyleId) {
                        checked.set(true)
                        itemVm = this
                    }
                    onItemClick.value = {
                        itemVm?.checked?.set(false)
                        this.checked.set(true)
                        itemVm = this
                    }
                }
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VideoStyleConfigInfo>() {
            override fun areItemsTheSame(
                oldItem: VideoStyleConfigInfo,
                newItem: VideoStyleConfigInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: VideoStyleConfigInfo,
                newItem: VideoStyleConfigInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}