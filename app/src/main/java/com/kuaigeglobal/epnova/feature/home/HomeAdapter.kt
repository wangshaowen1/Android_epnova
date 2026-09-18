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

/** Home 作品分页适配器，负责卡片绑定、作品点击埋点和播放页跳转。 */
class HomeAdapter(
    private val tabName: String?,
    var bookshelfName: String? = null
) : PagingDataAdapter<WorkVOInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
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

        /** 为当前作品创建展示 VM，并把点击事件交给统一的播放入口。 */
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
//                item.workId?.let { workId ->
//                    if (viewedDramaIds.add(workId)) {
//                    }
//                }
                // 使用 ItemHomeDramaVm 保存卡片状态，Adapter 只负责注入页面级点击行为。
                val transVM = binding.getVm(position, item, ItemHomeDramaVm::class.java).apply {
                    onItemClick.value = {
                        // 埋点使用当前 Tab 和书架名称区分作品的展示来源。
                        EventManager.onFirebaseTrack(
                            FirebaseEvent.Category.HOME,
                            FirebaseEvent.Action.DRAMA_CLICK,
                            label1 = item.workId,
                            label2 = tabName,
                            label3 = bookshelfName
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
        // workId 是作品稳定标识，内容变化交给 areContentsTheSame 刷新卡片。
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
