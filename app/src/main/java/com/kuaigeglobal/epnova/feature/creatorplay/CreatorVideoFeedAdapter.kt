package com.kuaigeglobal.epnova.feature.creatorplay

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.databinding.ItemCreatorVideoFeedBinding
import com.kuaigeglobal.epnova.widget.PlayerSeekbar
import com.nhsd.basic.item.BindingViewHolder

/**
 * 新创作播放器 feed Adapter。
 *
 * Adapter 只绑定 CreatorPlayableEpisode，不接旧剧集模型、解锁、点赞、收藏或选集逻辑。
 */
class CreatorVideoFeedAdapter(
    private val interactionListener: InteractionListener
) : PagingDataAdapter<CreatorPlayableEpisode, CreatorVideoFeedAdapter.CreatorVideoViewHolder>(DIFF_CALLBACK) {

    /**播放器 item 的基础交互，只包含播放切换和进度拖动。*/
    interface InteractionListener {
        /**点击播放区域或中心按钮时切换播放/暂停。*/
        fun onTogglePlay(position: Int)

        /**拖动进度条结束后跳到指定毫秒。*/
        fun onSeek(position: Int, progress: Long)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatorVideoViewHolder {
        return CreatorVideoViewHolder(
            ItemCreatorVideoFeedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CreatorVideoViewHolder, position: Int) {
        holder.bind(position)
    }

    /**Controller 通过 peek 读取当前 item，不触发 Paging 额外加载。*/
    fun peekPlayableItem(position: Int): CreatorPlayableEpisode? {
        if (position < 0 || position >= itemCount) return null
        return peek(position)
    }

    inner class CreatorVideoViewHolder(
        val binding: ItemCreatorVideoFeedBinding
    ) : BindingViewHolder<ItemCreatorVideoFeedBinding>(binding) {

        /**loading 图标复用项目现有旋转动画，只表达播放器缓冲态。*/
        private val rotateAnim by lazy {
            AnimationUtils.loadAnimation(binding.root.context, R.anim.loading_2)
        }

        init {
            binding.seekbar.onSeekListener = object : PlayerSeekbar.OnSeekListener {
                override fun onDrag() = Unit

                override fun onDragEnd(position: Long) {
                    val adapterPosition = bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        interactionListener.onSeek(adapterPosition, position)
                    }
                }
            }

            binding.playerView.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    interactionListener.onTogglePlay(adapterPosition)
                }
            }

            binding.btnPause.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    interactionListener.onTogglePlay(adapterPosition)
                }
            }
        }

        override fun bind(position: Int) {
            val item = getItem(position) ?: return
            val viewModel = binding.getVm<ItemCreatorVideoFeedVm, CreatorPlayableEpisode>(
                key = ItemCreatorVideoFeedVm.stableKey(item),
                item = item,
                viewModelFactory = {
                    ItemCreatorVideoFeedVm(item)
                }
            )
            viewModel.updateData(item)
            viewModel.resetPlayerUi()
            binding.item = viewModel
            binding.seekbar.currentPosition = 0L
            binding.seekbar.duration = viewModel.durationMs()
            binding.executePendingBindings()
        }

        /**更新当前 item 的播放进度。*/
        fun updateProgress(current: Long, total: Long) {
            if (total > 0L) {
                binding.seekbar.duration = total
                binding.seekbar.currentPosition = current
            }
        }

        /**根据播放器状态刷新当前 item UI，不做 3 秒隐藏或旧互动栏状态。*/
        fun updatePlayerUiState(state: CreatorFeedPlayerManager.CreatorPlayerState) {
            binding.item?.applyPlayerState(state)
            binding.executePendingBindings()

            if (binding.item?.showBuffering?.get() == true) {
                binding.animBuffering.clearAnimation()
                binding.animBuffering.startAnimation(rotateAnim)
            } else {
                binding.animBuffering.clearAnimation()
            }
        }
    }

    companion object {
        /**播放 feed 以 feedKey 优先作为稳定 key，缺失时降级到 storyId/workId/feedIndex。*/
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CreatorPlayableEpisode>() {
            override fun areItemsTheSame(
                oldItem: CreatorPlayableEpisode,
                newItem: CreatorPlayableEpisode
            ): Boolean {
                return ItemCreatorVideoFeedVm.stableKey(oldItem) == ItemCreatorVideoFeedVm.stableKey(newItem)
            }

            override fun areContentsTheSame(
                oldItem: CreatorPlayableEpisode,
                newItem: CreatorPlayableEpisode
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
