package com.kuaigeglobal.epnova.feature.play

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.getVm
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.ItemVideoFeedBinding
import com.kuaigeglobal.epnova.feature.play.manger.FeedPlayerManager
import com.kuaigeglobal.epnova.feature.play.more.EpisodeUnlockController
import com.kuaigeglobal.epnova.widget.PlayerSeekbar
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BindingViewHolder

class VideoFeedAdapter(private val interactionListener: InteractionListener) :
    ListAdapter<EpisodeInfo, VideoFeedAdapter.VideoViewHolder>(DIFF_CALLBACK) {
    
    // 定义交互回调
    interface InteractionListener {
        fun onTogglePlay(position: Int)
        fun onSeek(position: Int, progress: Long)
        fun onToggleCollect(position: Int)
        fun onToggleLike(position: Int)
        fun onReportPlayback(position: Int, episodeInfo: EpisodeInfo?, progress: Long, duration: Long)
        fun showEpisodeSelectDialog(position: Int)
    }

    private var globalWorkInfo: WorkVOInfo? = null

    fun updateWorkInfo(workInfo: WorkVOInfo?) {
        this.globalWorkInfo = workInfo
        // Payload 局部刷新
        notifyItemRangeChanged(0, itemCount, PAYLOAD_WORK_INFO)
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
            val currentItem = getItem(position) // 获取最新数据

            if (activePayloads.contains(PAYLOAD_URL)) {
                holder.binding.seekbar.duration = (getItem(position).duration ?: 0) * 1000L
            }
            if (activePayloads.contains(PAYLOAD_LIKE)) {
                holder.binding.item?.syncLike(getItem(position))
                holder.binding.item?.updateData(currentItem)
                holder.binding.executePendingBindings()
            }
            if (activePayloads.contains(PAYLOAD_WORK_INFO)) {
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

        private val handler = Handler(Looper.getMainLooper())

        // 当前激活的解锁控制器，随 ViewHolder 回收自动释放
        private var activeUnlockController: EpisodeUnlockController? = null

        // 自动隐藏控制栏的任务 (规则2)
        private val autoHideControlsTask = Runnable {
            binding.item?.isHideViews?.set(true)
            binding.item?.showCenterPlayBtn?.set(false)
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
            // 复用时重置解锁覆盖层，防止旧集锁定态残留
            hideUnlockOverlay()
            getItem(position)?.let { item ->
                val transVM = binding.getVm<ItemVideoFeedVm, EpisodeInfo>(
                    key = item.storyId,
                    item = item
                )

                //强制重置复用带来的脏状态
                transVM.showCenterPlayBtn.set(false)
                transVM.isHideViews.set(false)
                transVM.isPlaying.set(true)
                transVM.showBuffering.set(false)

                transVM.updateData(item)
                if (item.liked != null || item.likeCount != null) {
                    transVM.syncLike(item)
                }
                transVM.actionListener = interactionListener
                transVM.positionProvider = { bindingAdapterPosition } // 动态获取位置，防串位神器
                resetTimers()
                // 【核心逻辑：点击空白区】
                transVM.onBlankAreaClicked = {
                    val isCurrentlyHidden = transVM.isHideViews.get()
                    if (isCurrentlyHidden) {
                        // 规则3：点击空白区，呼出控制栏，展示播放暂停icon
                        transVM.isHideViews.set(false)
                        transVM.showCenterPlayBtn.set(true)

                        // 呼出后，如果正在播放，重新开始3秒倒计时 (PRD说等待3秒自动消失)
                        if (transVM.isPlaying.get()) {
                            startAutoHideTimer()
                        }
                    } else {
                        // 如果已经显示了，再次点击空白区（通常是隐藏，或者根据你的PRD也可以不做操作）
                        // 按照PRD第2条“点击播放器空白区...控制栏自动消失”
                        transVM.isHideViews.set(true)
                        transVM.showCenterPlayBtn.set(false)
                        cancelAutoHideTimer()
                    }
                }
                // 塞数据
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
            val vm = binding.item ?: return

            val isBuffering = state is FeedPlayerManager.FeedPlayerState.Buffering
            vm.showBuffering.set(isBuffering)
//            vm.showPauseBtn.set(state is FeedPlayerManager.FeedPlayerState.Pause)

            when (state) {
                is FeedPlayerManager.FeedPlayerState.Playing -> {
                    vm.isPlaying.set(true)

                    // 规则 5 & 6：正在播放时
                    // 1. 如果之前是显示状态，现在要开始 3s 倒计时自动隐藏
                    if (!vm.isHideViews.get()) {
                        startAutoHideTimer()
                    }

                    // 播放时隐藏中心播放按钮（根据 PRD，只有暂停才显现按钮）
                    vm.showCenterPlayBtn.set(false)

                }

                is FeedPlayerManager.FeedPlayerState.Pause -> {
                    vm.isPlaying.set(false)

                    // 【你的新需求】：点击暂停后，立即显示所有 UI
                    cancelAutoHideTimer() // 停止隐藏倒计时
                    vm.isHideViews.set(false) // 显示进度条、标题等
                    vm.showCenterPlayBtn.set(true) // 显示中间的播放按钮

                }

                is FeedPlayerManager.FeedPlayerState.Error -> {
                    if (state.isNoCache) {
                        binding.lsv.showRetry(RuntimeException(state.msg))
                    }
                    resetTimers()
                }

                else -> {}
            }

            if (isBuffering) {
                // 使用 post 确保在下一帧布局完成后再计算中心点启动动画
                binding.animBuffering.post {
                    binding.animBuffering.clearAnimation()
                    binding.animBuffering.startAnimation(rotateAnim)
                }
            } else {
                binding.animBuffering.clearAnimation()
            }
        }

        // --- 定时器辅助方法 ---
        private fun startAutoHideTimer() {
            handler.removeCallbacks(autoHideControlsTask)
            handler.postDelayed(autoHideControlsTask, 3000L)
        }

        private fun cancelAutoHideTimer() {
            handler.removeCallbacks(autoHideControlsTask)
        }

        private fun resetTimers() {
            handler.removeCallbacksAndMessages(null)
        }

        fun performProgressReport() {
            val currentPos = bindingAdapterPosition
            if (currentPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return

            val episode = getItem(currentPos) ?: return
            val progress = binding.seekbar.currentPosition
            val duration = binding.seekbar.duration

            // Adapter 只做切走/退出时的兜底，不再承担 10% 节点判断。
            // 过滤 0 进度，避免 ViewHolder 复用或未开始播放时把服务端续播点覆盖为开头。
            if (progress <= 0L || duration <= 0L) return
            interactionListener.onReportPlayback(currentPos, episode, progress, duration)
        }

        fun onViewRecycled() {
            hideUnlockOverlay()
            resetTimers()
        }

        /**
         * 显示解锁覆盖层并绑定控制器。
         * 若已有旧控制器，先释放再绑新的。
         */
        fun showUnlockOverlay(controller: EpisodeUnlockController) {
            activeUnlockController?.release()
            activeUnlockController = controller

            val overlay = binding.episodeUnlockOverlay
            overlay.ctrl = controller
            overlay.executePendingBindings()
            overlay.root.visibility = View.VISIBLE

            // 解锁层出现时隐藏收藏/点赞按钮
            binding.llInteractionSide.visibility = View.GONE

            val activity = binding.root.context as? Activity
            overlay.btnUnlockNow.setOnSingleClickListener {
                controller.onClickUnlockNow()
                controller.saveAutoUnlock(controller.autoUnlock.get())
            }
            overlay.llAdUnlock.setOnSingleClickListener {
                if (activity != null) {
                    controller.onClickAdUnlock(binding.root.context, activity)
                }
                controller.saveAutoUnlock(controller.autoUnlock.get())
            }
            overlay.llAutoUnlock.setOnSingleClickListener {
                controller.toggleAutoUnlock()
            }
        }

        /** 隐藏解锁覆盖层，释放控制器资源 */
        fun hideUnlockOverlay() {
            activeUnlockController?.release()
            activeUnlockController = null
            binding.episodeUnlockOverlay.root.visibility = View.GONE
            // 解锁层消失后，根据当前播放 UI 状态恢复收藏/点赞按钮
            if (binding.item?.isHideViews?.get() != true) {
                binding.llInteractionSide.visibility = View.VISIBLE
            }
        }

        fun onDetached() {
            // 切走/退出时兜底上报当前进度，节点上报由播放页 progressFlow 统一处理。
            performProgressReport()
            resetTimers() // 清理掉现有的 Handler 任务
        }
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // 当 Item 划出屏幕，立即保存当前进度
        holder.onDetached()
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.onViewRecycled()
    }

    companion object {
        const val PAYLOAD_URL = "PAYLOAD_URL"
        const val PAYLOAD_LIKE = "PAYLOAD_LIKE"
        const val PAYLOAD_WORK_INFO = "PAYLOAD_WORK_INFO"
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EpisodeInfo>() {
            override fun areItemsTheSame(oldItem: EpisodeInfo, newItem: EpisodeInfo) = oldItem.storyId == newItem.storyId
            override fun areContentsTheSame(oldItem: EpisodeInfo, newItem: EpisodeInfo) = oldItem == newItem

            override fun getChangePayload(oldItem: EpisodeInfo, newItem: EpisodeInfo): Any? {
                val payloads = mutableListOf<String>()

                if (oldItem.videoUrl != newItem.videoUrl || oldItem.duration != newItem.duration) {
                    payloads.add(PAYLOAD_URL)
                }
                // [新增] 判断点赞状态是否有变更
                if (oldItem.liked != newItem.liked || oldItem.likeCount != newItem.likeCount) {
                    payloads.add(PAYLOAD_LIKE)
                }

                return payloads.ifEmpty { null }
            }
        }
    }
}
