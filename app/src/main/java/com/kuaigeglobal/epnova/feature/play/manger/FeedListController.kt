package com.kuaigeglobal.epnova.feature.play.manger

import androidx.core.view.doOnPreDraw
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerViewModel
import com.kuaigeglobal.epnova.feature.play.EpisodeLockedEx
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
    /** 允许滑到的最大 position（第一个锁定集的 index），超过则拦截下滑 */
    private var maxScrollPosition = Int.MAX_VALUE

    private val layoutManager = object : LinearLayoutManager(recyclerView.context) {
        override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
            // dy > 0 = 向下滑（看后面的集）；当前已在 maxScrollPosition 时拦截
            if (dy > 0 && findFirstVisibleItemPosition() >= maxScrollPosition) return 0
            return super.scrollVerticallyBy(dy, recycler, state)
        }
    }
    private val snapHelper = PagerSnapHelper()

    // [关键修复] 记录最后一次播放的位置，防止 IDLE 状态重复触发
    private var lastPlayedPosition = -1

    // 页面切换回调
    var onPageChanged: ((EpisodeInfo, Int) -> Unit)? = null
    /**
     * 集数锁定回调
     * isAutoUnlockFailed true=自动解锁开启但余额不足（流程：充值→广告→手动解锁）
     *                           false=自动解锁未开启（直接展示手动解锁弹窗）
     */
    var onEpisodeLocked: ((episode: EpisodeInfo, isAutoUnlockFailed: Boolean) -> Unit)? = null
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

        // 始终重置，确保跳集/解锁后重新进入同一位置时能正常触发播放
//        lastPlayedPosition = -1
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
        lastPlayedPosition = position
        val item = adapter.currentList[position] ?: return

        // 发送 UI 状态等可以继续用当前的 holder
        val initialHolder = recyclerView.findViewHolderForAdapterPosition(position) as? VideoFeedAdapter.VideoViewHolder ?: return
        onPageChanged?.invoke(item, position)

        // 锁定 + 自动解锁关闭 → 直接回调弹窗，不发起网络请求（isAutoUnlockFailed=false）
        if (item.lockStatus == 2 && !Config.get().isEpisodeAutoUnlock()) {
            onEpisodeLocked?.invoke(item, false)
            return
        }

        if (item.videoUrl.isNullOrEmpty() || item.lockStatus == 2) {
            initialHolder.updatePlayerUiState(FeedPlayerManager.FeedPlayerState.Buffering)

            fetchUrlJob?.cancel()
            fetchUrlJob = recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                try {
                    // 挂起等待网络请求（内含自动解锁逻辑）
                    val readyItem = viewModel.fetchPlayUrlIfNeeded(item)

                    // 恢复后校验
                    if (getCurrentPosition() == position) {
                        val currentHolder = recyclerView.findViewHolderForAdapterPosition(position) as? VideoFeedAdapter.VideoViewHolder
                        if (currentHolder != null) {
                            playerManager.attachAndPlay(readyItem, currentHolder.binding.playerView)
                        }
                    }
                } catch (e: EpisodeLockedEx) {
                    // 用户已滑走则放弃，避免 onEpisodeLocked 打到错误位置
                    if (getCurrentPosition() != position) return@launch
                    val freshHolder = recyclerView.findViewHolderForAdapterPosition(position)
                        as? VideoFeedAdapter.VideoViewHolder
                    freshHolder?.updatePlayerUiState(FeedPlayerManager.FeedPlayerState.Idle)
                    onEpisodeLocked?.invoke(e.episode, Config.get().isEpisodeAutoUnlock())
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e  // 必须重新抛出，让协程框架正确处理 Job 取消
                } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            fetchUrlJob?.cancel()
            playerManager.attachAndPlay(item, initialHolder.binding.playerView)
        }
        preloadNextItem(position)
    }
    private fun preloadNextItem(currentPosition: Int) {
        if (currentPosition + 1 < adapter.itemCount) {
            val nextItem = adapter.currentList[currentPosition + 1] ?: return
            if (nextItem.videoUrl.isNullOrEmpty()) {
                recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                    try {
                        // 锁定集不预加载
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
     * 根据当前列表重新计算滑动上限。
     * 在 adapter.submitList 回调或解锁成功后调用。
     */
    fun updateScrollLimit(episodes: List<EpisodeInfo>) {
        val idx = episodes.indexOfFirst { it.lockStatus == 2 }
        maxScrollPosition = if (idx >= 0) idx else Int.MAX_VALUE
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