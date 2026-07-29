package com.kuaigeglobal.epnova.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.ItemHomeDramaBinding
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.nhsd.basic.item.BindingViewHolder

class HomeAdapter : PagingDataAdapter<WorkVOInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    private val viewedDramaIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemHomeDramaBinding.inflate(
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


    inner class ViewHolder(binding: ItemHomeDramaBinding) :
        BindingViewHolder<ItemHomeDramaBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
//                item.workId?.let { workId ->
//                    if (viewedDramaIds.add(workId)) {
//                    }
//                }
                val transVM = binding.getVm(position, item, ItemHomeDramaVm::class.java).apply {
                    onItemClick.value = {
                        EventManager.onFirebaseTrack(
                            FirebaseEvent.Category.HOME,
                            FirebaseEvent.Action.DRAMA_CLICK,
                            item.workId
                        )
                        DramasPlayPagerActivity.start(
                            workId = item.workId,
                            position = 0,
                            isSeriesMode = false,
                            source = FirebaseEvent.Value.SOURCE_HOME
                        )
                    }
                }
                binding.item = transVM
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
