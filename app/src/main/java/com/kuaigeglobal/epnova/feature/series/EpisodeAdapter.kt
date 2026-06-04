package com.kuaigeglobal.epnova.feature.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.common.ext.putVm
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.databinding.ItemMyEpisodeInfoBinding
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.nhsd.basic.item.BindingViewHolder

class EpisodeAdapter(private val eventVm: ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>) :
    PagingDataAdapter<CreatorEpisodeInfo, EpisodeAdapter.ViewHolder>(DIFF_CALLBACK) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMyEpisodeInfoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(binding: ItemMyEpisodeInfoBinding) :
        BindingViewHolder<ItemMyEpisodeInfoBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemSeriesEpisodeVM>(position)?.also { it.updateData(item) }
                        ?: ItemSeriesEpisodeVM(eventVm, position, item,).also { binding.putVm(position, it) }
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CreatorEpisodeInfo>() {
            override fun areItemsTheSame(
                oldItem: CreatorEpisodeInfo,
                newItem: CreatorEpisodeInfo
            ): Boolean {
                return oldItem.storyId == newItem.storyId
            }

            override fun areContentsTheSame(
                oldItem: CreatorEpisodeInfo,
                newItem: CreatorEpisodeInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
