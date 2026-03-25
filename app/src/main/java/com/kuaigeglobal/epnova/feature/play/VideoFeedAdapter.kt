package com.kuaigeglobal.epnova.feature.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.databinding.ItemVideoFeedBinding
import com.kuaigeglobal.epnova.feature.play.manger.FeedPlayerManager
import com.kuaigeglobal.epnova.widget.PlayerSeekbar
import com.nhsd.basic.item.BindingViewHolder

class VideoFeedAdapter(private val interactionListener: InteractionListener) :
    PagingDataAdapter<WorkInfo, VideoFeedAdapter.VideoViewHolder>(DIFF_CALLBACK) {

    // 定义交互回调
    interface InteractionListener {
        fun onTogglePlay(position: Int)
        fun onSeek(position: Int, progress: Long)
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
            // 处理局部刷新
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class VideoViewHolder(binding: ItemVideoFeedBinding) : BindingViewHolder<ItemVideoFeedBinding>(binding) {

        private val rotateAnim by lazy {
            AnimationUtils.loadAnimation(binding.root.context, R.anim.loading_2)
        }

        init {
            binding.playerView.setOnClickListener {
                interactionListener.onTogglePlay(bindingAdapterPosition)
            }

            // 2. 进度条拖拽监听
            binding.seekbar.onSeekListener = object : PlayerSeekbar.OnSeekListener {
                override fun onDrag() {
                    // 拖拽开始：PlayerSeekbar 内部会自动处理 dragging 状态，
                    // 此时即使 updateProgress 被调用，PlayerSeekbar 也会优先显示 dragValue
                    // 所以这里可以不用做特殊逻辑，或者用于暂停自动隐藏控件的计时器
                    binding.tvTitle.visibility = View.INVISIBLE
                    binding.tvContent.visibility = View.INVISIBLE
                }

                override fun onDragEnd(position: Long) {
                    // 拖拽结束：执行 Seek 操作
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
                binding.item = transVM
                binding.executePendingBindings()
                binding.lsv.showContent()
                toggleLoading(false)
                // [补充] 初始设置数据给 PlayerView，防止 DataBinding 延迟
                binding.playerView.setTrack(item)
                binding.seekbar.currentPosition = 0
                binding.seekbar.duration = item.getDuring()
                binding.btnPause.visibility = View.GONE
            }
        }

        fun updateProgress(current: Long, total: Long) {
            binding.lsv.showContent()
            if (total > 0) {
                binding.seekbar.duration = total
                binding.seekbar.currentPosition = current
            }
        }

        // 更新播放/暂停图标状态
        fun updatePlayState(isPlaying: Boolean) {
            binding.lsv.showContent()
            binding.btnPause.visibility = if (isPlaying) View.GONE else View.VISIBLE
        }

        private fun toggleLoading(show: Boolean) {
            if (show) {
                binding.pbBuffering.visibility = View.VISIBLE
                // 只有当没有动画在运行时才启动，防止动画重置闪烁
                if (binding.animBuffering.animation == null) {
                    binding.animBuffering.startAnimation(rotateAnim)
                }
            } else {
                binding.pbBuffering.visibility = View.GONE
                binding.animBuffering.clearAnimation()
            }
        }

        fun updatePlayerUiState(state: FeedPlayerManager.FeedPlayerState) {
            // 只要不是全屏错误，就必须显示内容容器
            binding.lsv.showContent()

            toggleLoading(state is FeedPlayerManager.FeedPlayerState.Buffering)
            binding.btnPause.visibility = if (state is FeedPlayerManager.FeedPlayerState.Pause) View.VISIBLE else View.GONE

            when (state) {
                is FeedPlayerManager.FeedPlayerState.Idle -> {
                }

                is FeedPlayerManager.FeedPlayerState.Buffering -> {
                    // 左图模式：显示转圈，隐藏暂停按钮，保持画面可见
                }

                is FeedPlayerManager.FeedPlayerState.Playing -> {
                }

                is FeedPlayerManager.FeedPlayerState.Pause -> {
                }

                is FeedPlayerManager.FeedPlayerState.Error -> {
                    if (state.isNoCache) {
                        // 右图模式：全屏错误，切换 LSV 状态
                        binding.lsv.showRetry(RuntimeException(state.msg))
                    } else {
                        // 左图延伸：有缓存出错，保持画面，Toast提示(Activity处理)，这里维持原样即可
                        // 也可以选择让 pbBuffering 继续转，或者显示一个小重试标
                    }
                }
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkInfo>() {
            override fun areItemsTheSame(oldItem: WorkInfo, newItem: WorkInfo) = oldItem.workId == newItem.workId
            override fun areContentsTheSame(oldItem: WorkInfo, newItem: WorkInfo) = oldItem == newItem
        }
    }
}