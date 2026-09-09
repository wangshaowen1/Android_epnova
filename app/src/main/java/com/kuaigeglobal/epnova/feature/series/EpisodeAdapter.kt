package com.kuaigeglobal.epnova.feature.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.databinding.ItemMyEpisodeInfoBinding
import com.kuaigeglobal.epnova.databinding.ItemMyEpisodeInfoListBinding
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.nhsd.basic.item.BindingViewHolder

class EpisodeAdapter(
    private val eventVm: ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>,
    isGridMode: Boolean = true
) : PagingDataAdapter<CreatorEpisodeInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    private var isGridMode = isGridMode

    fun setGridMode(gridMode: Boolean) {
        if (isGridMode == gridMode) return
        isGridMode = gridMode
        // viewType 已改变，需要让 RecyclerView 重建当前可见 holder。
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridMode) TYPE_GRID else TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_GRID) {
            GridViewHolder(ItemMyEpisodeInfoBinding.inflate(inflater, parent, false))
        } else {
            ListViewHolder(ItemMyEpisodeInfoListBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GridViewHolder -> holder.bind(position)
            is ListViewHolder -> holder.bind(position)
        }
    }

    inner class GridViewHolder(
        private val binding: ItemMyEpisodeInfoBinding
    ) : BindingViewHolder<ItemMyEpisodeInfoBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemSeriesEpisodeVM, CreatorEpisodeInfo>(
                    key = item.storyId ?: "",
                    item = item,
                    viewModelFactory = { ItemSeriesEpisodeVM(eventVm, position, item) },
                )
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    inner class ListViewHolder(
        private val binding: ItemMyEpisodeInfoListBinding
    ) : BindingViewHolder<ItemMyEpisodeInfoListBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemSeriesEpisodeVM, CreatorEpisodeInfo>(
                    key = item.storyId ?: "",
                    item = item,
                    viewModelFactory = { ItemSeriesEpisodeVM(eventVm, position, item) },
                )
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private const val TYPE_GRID = 0
        private const val TYPE_LIST = 1

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
