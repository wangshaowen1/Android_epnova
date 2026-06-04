package com.kuaigeglobal.epnova.feature.create.child

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.databinding.loadUri
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.databinding.ItemTopEarnersBinding
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.item.BindingViewHolder

/**
 * TopEarners 作品列表。
 *
 * 列表保持作品维度绑定 CreatorWorkInfo；播放器需要的 CreatorPlayableEpisode 会在新播放器阶段派生。
 */
class TopEarnersAdapter : PagingDataAdapter<CreatorWorkInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemTopEarnersBinding.inflate(
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

    inner class ViewHolder(binding: ItemTopEarnersBinding) :
        BindingViewHolder<ItemTopEarnersBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                // item VM 仍然绑定作品模型，避免列表阶段提前转换成播放单元。
                val viewModel = binding.getVm<ItemTopEarnersVm, CreatorWorkInfo>(
                    key = item.workId,
                    item = item,
                    viewModelFactory = {
                        ItemTopEarnersVm(item)
                    },
                ).apply {
                    onItemClick.value = {
                        // 临时保留旧入口。阶段五会替换为 CreatorPlayerActivity，并复用 TopEarnersCreatorFeedRepository。
                        DramasPlayPagerActivity.start(workId = item.workId, 0, false)
                    }
                }
                binding.item = viewModel
                bindPlatforms(item)
                binding.executePendingBindings()
            }
        }

        /**绑定发行平台 logo；平台链接属于 TopEarners/FastFilm 作品展示信息，不进入旧播放器体系。*/
        private fun bindPlatforms(item: CreatorWorkInfo) {
            binding.llPlatforms.removeAllViews()
            item.platformLinks.orEmpty()
                .mapNotNull { it.platformLogoUrl }
                .forEach { logoUrl ->
                    val imageView = ImageView(binding.root.context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        layoutParams = LinearLayout.LayoutParams(20.dp, 20.dp).apply {
                            marginEnd = 4.dp
                        }
                    }
                    loadUri(imageView, logoUrl)
                    binding.llPlatforms.addView(imageView)
                }
        }
    }

    companion object {
        /**分页列表以 workId 作为稳定 key；同一个作品的数据内容变化时仍触发 item 刷新。*/
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CreatorWorkInfo>() {
            override fun areItemsTheSame(
                oldItem: CreatorWorkInfo,
                newItem: CreatorWorkInfo
            ): Boolean {
                return oldItem.workId == newItem.workId
            }

            override fun areContentsTheSame(
                oldItem: CreatorWorkInfo,
                newItem: CreatorWorkInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
