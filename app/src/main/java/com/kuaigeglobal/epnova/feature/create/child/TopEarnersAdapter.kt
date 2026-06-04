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
import com.kuaigeglobal.epnova.core.network.apis.entity.TopEarnerWorkInfo
import com.kuaigeglobal.epnova.databinding.ItemTopEarnersBinding
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.item.BindingViewHolder

class TopEarnersAdapter : PagingDataAdapter<TopEarnerWorkInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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
                val viewModel = binding.getVm<ItemTopEarnersVm, TopEarnerWorkInfo>(
                    key = item.workId,
                    item = item,
                    viewModelFactory = {
                        ItemTopEarnersVm(item)
                    },
                ).apply {
                    onItemClick.value = {
                        DramasPlayPagerActivity.start(workId = item.workId, 0, false)
                    }
                }
                binding.item = viewModel
                bindPlatforms(item)
                binding.executePendingBindings()
            }
        }

        private fun bindPlatforms(item: TopEarnerWorkInfo) {
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
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TopEarnerWorkInfo>() {
            override fun areItemsTheSame(
                oldItem: TopEarnerWorkInfo,
                newItem: TopEarnerWorkInfo
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: TopEarnerWorkInfo,
                newItem: TopEarnerWorkInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}
