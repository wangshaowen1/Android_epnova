package com.kuaigeglobal.epnova.feature.play.manger

import androidx.core.view.doOnPreDraw
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerViewModel
import com.kuaigeglobal.epnova.feature.play.VideoFeedAdapter
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 列表控制器：负责滑动监听、自动播放、预加载
 */
class FeedListController(
    private val viewModel: DramasPlayPagerViewModel,
    private val recyclerView: RecyclerView,
    private val adapter: VideoFeedAdapter,
    private val playerManager: FeedPlayerManager
) {
    private val layoutManager = LinearLayoutManager(recyclerView.context)
    private val snapHelper = PagerSnapHelper()

    // [关键修复] 记录最后一次播放的位置，防止 IDLE 状态重复触发
    private var lastPlayedPosition = -1

    // 页面切换回调
    var onPageChanged: ((EpisodeInfo, Int) -> Unit)? = null
    private var fetchUrlJob: Job? = null

    init {
        recyclerView.layoutManager = layoutManager
        snapHelper.attachToRecyclerView(recyclerView)

        // 1. 滑动监听
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // 仅在完全停止时尝试播放
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkAndPlay()
                }
            }
        })
    }

    /**
     * 内部校验逻辑：判断是否需要执行播放
     */
    private fun checkAndPlay() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        if (position == RecyclerView.NO_POSITION || position == lastPlayedPosition) {
            // 位置没变或者无效，直接拦截，解决重复触发问题
            return
        }

        playCurrentItem(position)
    }

    /**
     * 由 Activity 在初始化数据完成后主动调用
     */
    fun startInitialPlay(targetPosition: Int = 0) {
        if (adapter.itemCount == 0) return
        val safePosition = targetPosition.coerceIn(0, adapter.itemCount - 1)

        // 如果初始位置就是 0，且当前就在 0，需要强制触发一次逻辑
        if (safePosition == 0) lastPlayedPosition = -1

        if (getCurrentPosition() != safePosition) {
            layoutManager.scrollToPositionWithOffset(safePosition, 0)
        }

        recyclerView.doOnPreDraw {
            checkAndPlay()
        }
    }

    private fun playCurrentItem(position: Int) {
        if (position < 0 || position >= adapter.itemCount) return

        // 立即更新记录位
        lastPlayedPosition = position

        val item = adapter.currentList[position] ?: return
        // 尽量通过 findViewHolderForAdapterPosition 准确获取
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as? VideoFeedAdapter.VideoViewHolder ?: return

        onPageChanged?.invoke(item, position)

        // --- 详情懒加载逻辑 ---
        if (item.videoUrl.isNullOrEmpty()) {
            holder.updatePlayerUiState(FeedPlayerManager.FeedPlayerState.Buffering)

            fetchUrlJob?.cancel()
            fetchUrlJob = recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                try {
                    val readyItem = viewModel.fetchPlayUrlIfNeeded(item)
                    // [关键] 异步回来后，必须再次检查当前位置是否还是这一条
                    if (getCurrentPosition() == position) {
                        playerManager.attachAndPlay(readyItem, holder.binding.playerView)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            fetchUrlJob?.cancel()
            playerManager.attachAndPlay(item, holder.binding.playerView)
        }

        preloadNextItem(position)
    }

    private fun preloadNextItem(currentPosition: Int) {
        if (currentPosition + 1 < adapter.itemCount) {
            val nextItem = adapter.currentList[currentPosition + 1] ?: return
            if (nextItem.videoUrl.isNullOrEmpty()) {
                recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                    try {
                        val readyItem = viewModel.fetchPlayUrlIfNeeded(nextItem)
                        if (!readyItem.videoUrl.isNullOrEmpty()) {
                            playerManager.preload(readyItem.videoUrl)
                        }
                    } catch (e: Exception) { }
                }
            } else {
                playerManager.preload(nextItem.videoUrl)
            }
        }
    }

    /**
     * 重置状态（当切换剧集、刷新整个列表时调用）
     */
    fun reset() {
        lastPlayedPosition = -1
        fetchUrlJob?.cancel()
    }

    // FeedListController.kt 修改
    fun smoothScrollToNext(): Boolean {
        val currentPos = getCurrentPosition()
        val totalCount = adapter.itemCount

        // 如果列表还没数据，或者位置获取不到，认为“还没结束”，返回 true 拦截掉 handlePlayEnded
        if (totalCount <= 0 || currentPos == RecyclerView.NO_POSITION) {
            return true
        }

        if (currentPos < totalCount - 1) {
            recyclerView.smoothScrollToPosition(currentPos + 1)
            return true
        }

        // 只有真正确定 currentPos == totalCount - 1 时才返回 false，触发切剧
        return false
    }

    fun getCurrentPosition(): Int {
        val centerView = snapHelper.findSnapView(layoutManager)
        return centerView?.let { layoutManager.getPosition(it) } ?: -1
    }

    fun getCurrentItem(): EpisodeInfo? {
        val pos = getCurrentPosition()
        return if (pos != -1) adapter.currentList.getOrNull(pos) else null
    }
}