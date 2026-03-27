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
    private val viewModel: DramasPlayPagerViewModel, // [新增] 引入 ViewModel 负责发请求
    private val recyclerView: RecyclerView,
    private val adapter: VideoFeedAdapter,
    private val playerManager: FeedPlayerManager
) {
    private val layoutManager = LinearLayoutManager(recyclerView.context)
    private val snapHelper = PagerSnapHelper()

    // 页面切换回调：(当前数据, 当前位置) -> Unit
    var onPageChanged: ((EpisodeInfo, Int) -> Unit)? = null
    private var fetchUrlJob: Job? = null

    init {
        recyclerView.layoutManager = layoutManager
        snapHelper.attachToRecyclerView(recyclerView)

        // 1. 滑动监听
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    playCurrentItem()
                }
            }
        })
    }

    /**
     * [新增] 替代原有的首屏自动播放逻辑。
     * 由 Activity 在 adapter.submitList() 的 commit 回调中主动调用。
     */
    fun startInitialPlay(targetPosition: Int = 0) {
        if (adapter.itemCount == 0) return
        val safePosition = targetPosition.coerceIn(0, adapter.itemCount - 1)

        // 1. 先让 LayoutManager 滚过去
        if (safePosition > 0) {
            layoutManager.scrollToPositionWithOffset(safePosition, 0)
        }

        // 2. 监听 RecyclerView 真正完成测绘的那一帧
        recyclerView.doOnPreDraw {
            playCurrentItem()
        }
    }

    fun playCurrentItem() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        if (position < 0 || position >= adapter.itemCount) return

        // ListAdapter 取数据直接用 currentList
        val item = adapter.currentList[position] ?: return
        val holder = recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder ?: return

        onPageChanged?.invoke(item, position)

        // --- 详情懒加载逻辑 ---
        if (item.videoUrl.isNullOrEmpty()) {
            holder.updatePlayerUiState(FeedPlayerManager.FeedPlayerState.Buffering)

            // 取消上一个可能还在请求的 Job
            fetchUrlJob?.cancel()

            fetchUrlJob = recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                try {
                    val readyItem = viewModel.fetchPlayUrlIfNeeded(item)
                    if (getCurrentPosition() == position) {
                        playerManager.attachAndPlay(readyItem, holder.binding.playerView)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            fetchUrlJob?.cancel() // 如果当前有缓存，也清理掉之前的废弃请求
            playerManager.attachAndPlay(item, holder.binding.playerView)
        }

        // 预加载下一条
        preloadNextItem(position)
    }

    private fun preloadNextItem(currentPosition: Int) {
        if (currentPosition + 1 < adapter.itemCount) {
            val nextItem = adapter.currentList[currentPosition + 1] ?: return
            if (nextItem.videoUrl.isNullOrEmpty()) {
                // 静默预加载下一集的 URL
                recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                    try {
                        val readyItem = viewModel.fetchPlayUrlIfNeeded(nextItem)
                        // URL拿到了，接着触发底层媒体预加载
                        if (!readyItem.videoUrl.isNullOrEmpty()) {
                            playerManager.preload(readyItem.videoUrl)
                        }
                    } catch (e: Exception) { /* 静默失败，等滑到了再重试 */ }
                }
            } else {
                playerManager.preload(nextItem.videoUrl)
            }
        }
    }

    fun smoothScrollToNext(): Boolean {
        val currentPos = getCurrentPosition()
        val totalCount = adapter.itemCount

        Logger.d("FeedListController", "smoothScrollToNext: current=$currentPos, total=$totalCount")

        if (currentPos == -1 || totalCount == 0) {
            Logger.w("FeedListController", "Cannot determine position, skipping scroll")
            return true
        }

        if (currentPos < totalCount - 1) {
            recyclerView.smoothScrollToPosition(currentPos + 1)
            return true
        }

        Logger.i("FeedListController", "Reached end of list")
        return false
    }

    fun getCurrentItem(): EpisodeInfo? {
        val currentPos = getCurrentPosition()
        return if (currentPos != -1) adapter.currentList[currentPos] else null
    }

    fun getCurrentPosition(): Int {
        val centerView = snapHelper.findSnapView(layoutManager)
        if (centerView != null) {
            val pos = layoutManager.getPosition(centerView)
            if (pos != RecyclerView.NO_POSITION) return pos
        }

        val firstPos = layoutManager.findFirstVisibleItemPosition()
        if (firstPos != RecyclerView.NO_POSITION) {
            return firstPos
        }

        return -1
    }
}