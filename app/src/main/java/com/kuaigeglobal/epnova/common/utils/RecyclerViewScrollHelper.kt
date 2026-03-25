package com.kuaigeglobal.epnova.common.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.widget.BottomSmoothScroller

object RecyclerViewScrollHelper {
    fun scrollToBottom(recyclerView: RecyclerView, delayTIme: Long) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager?
        layoutManager?.let {
            val lastPosition = layoutManager.itemCount - 1

            recyclerView.postDelayed({
                // 第一步：先跳转到目标位置
                layoutManager.scrollToPositionWithOffset(lastPosition, 0)

                // 第二步：补充滚动距离
                val lastItem = layoutManager.findViewByPosition(lastPosition)
                if (lastItem != null) {
                    val targetScroll = lastItem.bottom - recyclerView.height
                    if (targetScroll > 0) {
                        recyclerView.scrollBy(0, targetScroll)
                    }
                }

                // 反向布局特殊处理
                if (layoutManager.reverseLayout) {
                    recyclerView.scrollToPosition(0)
                    recyclerView.scrollBy(0, Int.MAX_VALUE) // 强制到底
                }
            }, delayTIme)
        }

    }

    fun smoothScrollToBottom(recyclerView: RecyclerView, delayTime: Long, speed: Float = 0.1f) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager?
        layoutManager?.let {
            recyclerView.postDelayed({
                // 获取最后一个 item 的位置
                val lastPosition = layoutManager.itemCount - 1
                if (lastPosition < 0) return@postDelayed
// 创建自定义 SmoothScroller
                val smoothScroller: RecyclerView.SmoothScroller =
                    BottomSmoothScroller(recyclerView.context, speed)
                smoothScroller.targetPosition = lastPosition
// 触发滚动
                recyclerView.layoutManager
                    ?.startSmoothScroll(smoothScroller)

//            binding.messageListView.recyclerView.scrollToPosition(aiChatComposerAdapter.itemCount - 1)
//            computeScroll()
            }, delayTime)
        }
    }

    fun isLastItemVisible(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val totalItemCount = layoutManager.itemCount
        if (totalItemCount == 0) return true // 空列表视为在底部
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        return lastVisiblePosition >= totalItemCount - 2
    }

    fun isScrollBottom(recyclerView: RecyclerView, offset:Int): Boolean {
        val layoutManager: RecyclerView.LayoutManager? = recyclerView.layoutManager
        if (layoutManager?.itemCount == null || layoutManager.itemCount == 0) return true
        val itemCount = layoutManager.itemCount
        if (layoutManager is LinearLayoutManager) {
            val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
            if (lastVisiblePosition == itemCount - 1) {
                val lastItem = layoutManager.findViewByPosition(lastVisiblePosition)
                if (lastItem != null) {
                    // 考虑RecyclerView的padding和Item的装饰（如分割线）
                    val bottomWithDecor = layoutManager.getDecoratedBottom(lastItem)
                    val recyclerVisibleBottom: Int =
                        recyclerView.height - recyclerView.paddingBottom
                    return bottomWithDecor <= recyclerVisibleBottom+offset
                }
            }
        }
        return false
    }
}