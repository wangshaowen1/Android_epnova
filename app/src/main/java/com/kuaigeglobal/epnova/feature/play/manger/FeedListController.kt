package com.kuaigeglobal.epnova.feature.play.manger

import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.feature.play.VideoFeedAdapter
import com.nhsd.basic.utils.Logger

/**
 * 列表控制器：负责滑动监听、自动播放、预加载
 */
class FeedListController(
    private val recyclerView: RecyclerView,
    private val adapter: VideoFeedAdapter,
    private val playerManager: FeedPlayerManager
) {
    private val layoutManager = LinearLayoutManager(recyclerView.context)
    private val snapHelper = PagerSnapHelper()

    // 标记是否是第一次加载数据
    private var isFirstLoad = true

    // 目标初始位置，默认为 -1 (不跳转)
    private var targetInitialPosition: Int = -1

    // 页面切换回调：(当前数据, 当前位置) -> Unit
    var onPageChanged: ((WorkInfo, Int) -> Unit)? = null

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

        // 2. 初始化首屏监听 (只注册一次！)
        setupFirstLoadListener()
    }

    /**
     * 设置初始播放位置
     * 注意：这个方法必须在 adapter 数据提交之前（onCreate 中）调用
     */
    fun setInitialPosition(position: Int) {
        this.targetInitialPosition = position
    }

    // 获取当前选中的 Item
    fun getCurrentItem(): WorkInfo? {
        val pos = getCurrentPosition() // 现有的方法
        if (pos in 0 until adapter.itemCount) {
            return adapter.peek(pos)
        }
        return null
    }

    private fun setupFirstLoadListener() {
        // [修改方案] 拆分触发逻辑：
        // 1. 定义一个通用的检查并播放的方法
        val checkAndPlay = {
            if (isFirstLoad && adapter.itemCount > 0) {
                Logger.d("FeedListController", "AutoPlay triggered! itemCount=${adapter.itemCount}")
                // 立即锁定，防止重复触发
                isFirstLoad = false

                recyclerView.post {
                    if (recyclerView.isAttachedToWindow) {
                        Logger.d(
                            "FeedListController",
                            "targetInitialPosition=$targetInitialPosition adapter.itemcount = ${adapter.itemCount}"
                        )
                        if (targetInitialPosition != -1 && targetInitialPosition < adapter.itemCount) {
                            // 1. 直接闪现到指定位置
                            layoutManager.scrollToPositionWithOffset(targetInitialPosition, 0)
                            // 2. 等待布局稳定后播放
                            recyclerView.post { playCurrentItem() }
                        } else {
                            // 默认播放第 0 个
                            playCurrentItem()
                        }
                    }
                }
            }
        }

        // 2. 监听 LoadState (保留，用于兼容网络加载的Loading->NotLoading状态切换)
        adapter.addLoadStateListener { loadState ->
            Logger.d("FeedListController", "LoadState: ${loadState.refresh}, Count: ${adapter.itemCount}")
            if (loadState.refresh is LoadState.NotLoading) {
                checkAndPlay.invoke()
            }
        }

        // 3. [核心修复] 监听数据页面更新 (专门解决 PagingData.from 内存数据不触发 LoadState 变化的问题)
        adapter.addOnPagesUpdatedListener {
            Logger.d("FeedListController", "PagesUpdated! Count: ${adapter.itemCount}")
            checkAndPlay.invoke()
        }
    }

    fun playCurrentItem() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        if (position < 0 || position >= adapter.itemCount) return

        val item = adapter.peek(position) ?: return

        onPageChanged?.invoke(item, position)
        val holder =
            recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder
                ?: return

        // 调用 Manager 播放
        playerManager.attachAndPlay(item, holder.binding.playerView)
        // 强制更新 UI 为播放状态
        holder.updatePlayState(true)

        // 预加载下一条
        if (position + 1 < adapter.itemCount) {
            val nextItem = adapter.peek(position + 1)
            // 根据你的 TrackInfo/DramaInfo 结构调用正确的 url 方法
            // 假设是 getPlayUrl() 或 playUrl
            nextItem?.getPlayUrl()?.let { playerManager.preload(it) }
        }
    }

    // [修复] 只有在真的到了最后一条时，才返回 false
    fun smoothScrollToNext(): Boolean {
        // 使用加强版的方法获取位置
        val currentPos = getCurrentPosition()
        val totalCount = adapter.itemCount

        Logger.d("FeedListController", "smoothScrollToNext: current=$currentPos, total=$totalCount")

        // 1. 异常保护：如果没数据，或者连当前位置都找不到，不能认为是“最后一集”，
        // 但也不能翻页。这里返回 true 并在内部消化，防止 Activity 误报 Toast
        if (currentPos == -1 || totalCount == 0) {
            Logger.w("FeedListController", "Cannot determine position, skipping scroll")
            return true // 返回 true 欺骗 Activity 别弹 Toast
        }

        // 2. 正常翻页逻辑
        if (currentPos < totalCount - 1) {
            recyclerView.smoothScrollToPosition(currentPos + 1)
            return true
        }

        // 3. 只有明确确认是最后一集（当前位置 == 总数-1），才返回 false 让 Activity 弹 Toast
        Logger.i("FeedListController", "Reached end of list")
        return false
    }

    // [修复] 获取当前位置的加强版方法
    fun getCurrentPosition(): Int {
        // 1. 优先尝试标准的 SnapHelper (获取正中间的)
        val centerView = snapHelper.findSnapView(layoutManager)
        if (centerView != null) {
            val pos = layoutManager.getPosition(centerView)
            if (pos != RecyclerView.NO_POSITION) return pos
        }

        // 2. [关键兜底] 如果 SnapHelper 失败（例如刚进页面或正在快速滑动），
        // 退而求其次，获取第一个完全可见/部分可见的 Item
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        if (firstPos != RecyclerView.NO_POSITION) {
            return firstPos
        }

        return -1
    }
}