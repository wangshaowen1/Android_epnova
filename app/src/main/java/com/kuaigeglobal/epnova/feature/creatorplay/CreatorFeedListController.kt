package com.kuaigeglobal.epnova.feature.creatorplay

import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode

/**
 * 新创作播放器列表控制器。
 *
 * 只负责上下滑分页、当前 item 播放和完播下一条，不包含锁集拦截或选集逻辑。
 */
class CreatorFeedListController(
    private val recyclerView: RecyclerView,
    private val adapter: CreatorVideoFeedAdapter,
    private val playerManager: CreatorFeedPlayerManager
) {

    /**竖向分页布局；不限制滑动范围，具体 source 边界由传入的 feed 决定。*/
    private val layoutManager = LinearLayoutManager(recyclerView.context)

    /**每次滑动停靠到一个完整作品/剧集 item。*/
    private val snapHelper = PagerSnapHelper()

    /**最近一次触发播放的位置，避免 RecyclerView IDLE 重复回调导致重复 prepare。*/
    private var lastPlayedPosition = RecyclerView.NO_POSITION

    /**首次进入或外部跳转时的目标位置，只对下一次 startInitialPlay 生效。*/
    private var pendingStartPosition = RecyclerView.NO_POSITION

    /**首次播放目标位置的起播毫秒，阶段四默认 0，后续可接续播。*/
    private var pendingStartPositionMs = 0L

    /**当前页变化回调，Activity 用于同步 VM 当前播放单元。*/
    var onPageChanged: ((CreatorPlayableEpisode, Int) -> Unit)? = null

    init {
        recyclerView.layoutManager = layoutManager
        snapHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkAndPlay()
                }
            }
        })
    }

    /**数据首次渲染后调用，滚到目标位置并播放。*/
    fun startInitialPlay(targetPosition: Int = 0, startPositionMs: Long = 0L) {
        if (adapter.itemCount == 0) return
        val safePosition = targetPosition.coerceIn(0, adapter.itemCount - 1)
        lastPlayedPosition = RecyclerView.NO_POSITION
        pendingStartPosition = safePosition
        pendingStartPositionMs = startPositionMs.coerceAtLeast(0L)

        if (getCurrentPosition() != safePosition) {
            layoutManager.scrollToPositionWithOffset(safePosition, 0)
        }

        recyclerView.doOnPreDraw {
            checkAndPlay()
        }
    }

    /**重置播放位置缓存，feed 刷新或外部切换 source 时调用。*/
    fun reset() {
        lastPlayedPosition = RecyclerView.NO_POSITION
        pendingStartPosition = RecyclerView.NO_POSITION
        pendingStartPositionMs = 0L
    }

    /**完播后尝试滚到下一条；返回 false 表示当前已经是最后一条。*/
    fun smoothScrollToNext(): Boolean {
        val currentPosition = getCurrentPosition()
        if (currentPosition == RecyclerView.NO_POSITION || adapter.itemCount == 0) {
            return true
        }
        if (currentPosition < adapter.itemCount - 1) {
            recyclerView.smoothScrollToPosition(currentPosition + 1)
            return true
        }
        return false
    }

    /**当前吸附在屏幕中心的 item position。*/
    fun getCurrentPosition(): Int {
        val centerView = snapHelper.findSnapView(layoutManager)
        return centerView?.let { layoutManager.getPosition(it) } ?: RecyclerView.NO_POSITION
    }

    /**当前正在展示的 ViewHolder，用于 Activity 分发进度和播放状态。*/
    fun getCurrentHolder(): CreatorVideoFeedAdapter.CreatorVideoViewHolder? {
        val position = getCurrentPosition()
        if (position == RecyclerView.NO_POSITION) return null
        return recyclerView.findViewHolderForAdapterPosition(position)
            as? CreatorVideoFeedAdapter.CreatorVideoViewHolder
    }

    /**滚动停止后检查当前页是否需要开始播放。*/
    private fun checkAndPlay() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)
        if (position == RecyclerView.NO_POSITION || position == lastPlayedPosition) {
            return
        }
        playCurrentItem(position)
    }

    /**播放指定位置的 CreatorPlayableEpisode。*/
    private fun playCurrentItem(position: Int) {
        if (position < 0 || position >= adapter.itemCount) return
        val item = adapter.peekPlayableItem(position) ?: return
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
            as? CreatorVideoFeedAdapter.CreatorVideoViewHolder
            ?: return

        lastPlayedPosition = position
        val startPositionMs = if (position == pendingStartPosition) pendingStartPositionMs else 0L
        pendingStartPosition = RecyclerView.NO_POSITION
        pendingStartPositionMs = 0L

        onPageChanged?.invoke(item, position)
        holder.updatePlayerUiState(CreatorFeedPlayerManager.CreatorPlayerState.Buffering)
        playerManager.attachAndPlay(item, holder.binding.playerView, startPositionMs)
    }
}
