package com.kuaigeglobal.epnova.feature.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.ItemVideoFeedBinding
import com.kuaigeglobal.epnova.feature.play.manger.FeedPlayerManager
import com.kuaigeglobal.epnova.widget.PlayerSeekbar
import com.nhsd.basic.item.BindingViewHolder

class VideoFeedAdapter(private val interactionListener: InteractionListener) :
    ListAdapter<EpisodeInfo, VideoFeedAdapter.VideoViewHolder>(DIFF_CALLBACK) {

    // 定义交互回调
    interface InteractionListener {
        fun onTogglePlay(position: Int)
        fun onSeek(position: Int, progress: Long)
        fun onToggleCollect(position: Int)
        fun onToggleLike(position: Int)
        fun showEpisodeSelectDialog(position: Int)
    }

    private var globalWorkInfo: WorkVOInfo? = null

    fun updateWorkInfo(workInfo: WorkVOInfo?) {
        this.globalWorkInfo = workInfo
        // Payload 局部刷新
        notifyItemRangeChanged(0, itemCount, "WORK_INFO_UPDATED")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            ItemVideoFeedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(position)
    }

    // 可以在这里处理 payload 局部刷新 (例如点赞)
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val activePayloads = payloads.filterIsInstance<List<Any>>().flatten().ifEmpty { payloads }

            if (activePayloads.contains("URL_UPDATED")) {
                holder.binding.seekbar.duration = (getItem(position).duration ?: 0) * 1000L
            }
            if (activePayloads.contains("LIKE_UPDATED")) {
                holder.binding.item?.syncLike(getItem(position))
            }
            if (activePayloads.contains("WORK_INFO_UPDATED")) {
                holder.binding.item?.syncWorkInfo(globalWorkInfo)
            }
            return
        }
        // 如果没有 payload 或者是全量刷新，必须调用 super
        super.onBindViewHolder(holder, position, payloads)
    }

    inner class VideoViewHolder(binding: ItemVideoFeedBinding) : BindingViewHolder<ItemVideoFeedBinding>(binding) {

        private val rotateAnim by lazy {
            AnimationUtils.loadAnimation(binding.root.context, R.anim.loading_2)
        }

        init {
            binding.seekbar.onSeekListener = object : PlayerSeekbar.OnSeekListener {
                override fun onDrag() {
                    binding.tvTitle.visibility = View.INVISIBLE
                    binding.tvContent.visibility = View.INVISIBLE
                }
                override fun onDragEnd(position: Long) {
                    interactionListener.onSeek(bindingAdapterPosition, position)
                    binding.tvTitle.visibility = View.VISIBLE
                    binding.tvContent.visibility = View.VISIBLE
                }
            }

            binding.lsv.setRetryListener {
                interactionListener.onTogglePlay(bindingAdapterPosition)
            }
        }

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemVideoFeedVm::class.java)
                transVM.actionListener = interactionListener
                transVM.positionProvider = { bindingAdapterPosition } // 动态获取位置，防串位神器

                // 塞数据
                transVM.syncLike(item)
                transVM.syncWorkInfo(globalWorkInfo)
                binding.item = transVM
                binding.executePendingBindings()

                // 其他基础 UI 设置
                binding.lsv.showContent()
                binding.playerView.setTrack(item)
                binding.seekbar.currentPosition = 0
                binding.seekbar.duration = (item.duration ?: 0) * 1000L
            }
        }

        fun updateProgress(current: Long, total: Long) {
            binding.lsv.showContent()
            if (total > 0) {
                binding.seekbar.duration = total
                binding.seekbar.currentPosition = current
            }
        }

        fun updatePlayerUiState(state: FeedPlayerManager.FeedPlayerState) {
            binding.lsv.showContent()

            val isBuffering = state is FeedPlayerManager.FeedPlayerState.Buffering
            binding.item?.showBuffering?.set(isBuffering)
            binding.item?.showPauseBtn?.set(state is FeedPlayerManager.FeedPlayerState.Pause)

            if (isBuffering) {
                if (binding.animBuffering.animation == null) {
                    binding.animBuffering.startAnimation(rotateAnim)
                }
            } else {
                binding.animBuffering.clearAnimation()
            }

            // 3. 错误状态处理
            if (state is FeedPlayerManager.FeedPlayerState.Error && state.isNoCache) {
                binding.lsv.showRetry(RuntimeException(state.msg))
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EpisodeInfo>() {
            override fun areItemsTheSame(oldItem: EpisodeInfo, newItem: EpisodeInfo) = oldItem.storyId == newItem.storyId
            override fun areContentsTheSame(oldItem: EpisodeInfo, newItem: EpisodeInfo) = oldItem == newItem

            override fun getChangePayload(oldItem: EpisodeInfo, newItem: EpisodeInfo): Any? {
                val payloads = mutableListOf<String>()

                if (oldItem.videoUrl != newItem.videoUrl || oldItem.duration != newItem.duration) {
                    payloads.add("URL_UPDATED")
                }
                // [新增] 判断点赞状态是否有变更
                if (oldItem.liked != newItem.liked || oldItem.likeCount != newItem.likeCount) {
                    payloads.add("LIKE_UPDATED")
                }

                return payloads.ifEmpty { null }
            }
        }
    }
}