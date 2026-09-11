package com.kuaigeglobal.epnova.widget.dragfooterview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewParent
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * ragContainer 的手势冲突容器，唯一直接子 View 为原有 DragContainer。
 * 起点右滑、滑动及竖滑交给父容器；提示、触发和回弹仍由子控件负责。
 */
class ShelfDragConflictLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private lateinit var items: RecyclerView
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var viewPagerParent: ViewParent? = null
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var gestureAxis = AXIS_UNDECIDED
    private var trackingGesture = false
    private var handedToParent = false
    private var pulling = false
    private var disallowParentIntercept = false

    override fun onFinishInflate() {
        super.onFinishInflate()
        check(childCount == 1 && getChildAt(0) is DragContainer) {
            "ShelfDragConflictLayout requires one DragContainer child"
        }
        items = (getChildAt(0) as DragContainer).getChildAt(1) as RecyclerView
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // 子控件在方向未判定时会请求放开父级，不能覆盖当前手势的归属。
        requestParentIntercept(
            if (trackingGesture) disallowParentIntercept else disallowIntercept,
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            trackingGesture = true
            handedToParent = false
            pulling = false
            gestureAxis = AXIS_UNDECIDED
            activePointerId = event.getPointerId(0)
            downX = event.x
            downY = event.y
            lastX = downX
            viewPagerParent = findViewPagerParent()
            disallowParentIntercept = hasHorizontalScroll()
            // 从 DOWN 开始保护可横滑的列表，避免第一段 MOVE 先被 ViewPager2 拦截。
            requestParentIntercept(disallowParentIntercept)
            val handled = super.dispatchTouchEvent(event)
            if (handled) requestParentIntercept(disallowParentIntercept) else finishGesture()
            return handled
        }

        if (!trackingGesture) return super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL
        ) {
            return try {
                if (handedToParent) true else super.dispatchTouchEvent(event)
            } finally {
                finishGesture()
            }
        }
        if (handedToParent) return true
        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            handToParent(event)
            return true
        }
        if (event.actionMasked != MotionEvent.ACTION_MOVE) {
            return super.dispatchTouchEvent(event)
        }

        val pointerIndex = event.findPointerIndex(activePointerId)
        if (pointerIndex < 0) {
            handToParent(event)
            return true
        }
        val x = event.getX(pointerIndex)
        val dx = abs(x - downX)
        val dy = abs(event.getY(pointerIndex) - downY)
        if (gestureAxis == AXIS_UNDECIDED && maxOf(dx, dy) > touchSlop) {
            gestureAxis = if (dy >= dx) AXIS_VERTICAL else AXIS_HORIZONTAL
        }
        val moveX = x - lastX
        if (shouldHandToParent(moveX)) {
            handToParent(event)
        } else {
            super.dispatchTouchEvent(event)
            // 子控件接管末尾拖动后，回拉到零也不把同一次手势交给 ViewPager2。
            pulling = pulling || items.translationX < 0f
            // RecyclerView 消费本次 MOVE 后可能刚好到达起点，需要再次检查边界。
            if (shouldHandToParent(moveX)) {
                handToParent(event)
            } else {
                requestParentIntercept(disallowParentIntercept)
            }
        }
        lastX = x
        return true
    }

    private fun shouldHandToParent(moveX: Float): Boolean {
        if (gestureAxis == AXIS_VERTICAL) return true
        // 未超过 touchSlop 或零位移时保留 DOWN 的决定，不提前让出横滑。
        if (gestureAxis != AXIS_HORIZONTAL || moveX == 0f || pulling) return false
        val direction = if (moveX > 0f) -1 else 1
        val atDragEdge = direction == 1 && hasHorizontalScroll() &&
            !items.canScrollHorizontally(1)
        disallowParentIntercept = items.canScrollHorizontally(direction) || atDragEdge
        return !disallowParentIntercept
    }

    private fun handToParent(event: MotionEvent) {
        handedToParent = true
        disallowParentIntercept = false
        // 先终止子控件的序列，避免父级尚未收到下一次 MOVE 时短列表仍拖出或触发。
        val cancel = MotionEvent.obtain(event)
        cancel.action = MotionEvent.ACTION_CANCEL
        try {
            super.dispatchTouchEvent(cancel)
        } finally {
            cancel.recycle()
            requestParentIntercept(false)
        }
    }

    private fun hasHorizontalScroll(): Boolean =
        items.canScrollHorizontally(-1) || items.canScrollHorizontally(1)

    private fun findViewPagerParent(): ViewParent? {
        var ancestor = parent
        while (ancestor != null) {
            if (ancestor is ViewPager || ancestor is ViewPager2) return ancestor
            ancestor = ancestor.parent
        }
        return null
    }

    private fun requestParentIntercept(disallow: Boolean) {
        parent?.requestDisallowInterceptTouchEvent(disallow)
        viewPagerParent?.requestDisallowInterceptTouchEvent(disallow)
    }

    private fun finishGesture() {
        disallowParentIntercept = false
        // 未参与手势的书架离屏时，不放开其他书架正在使用的父级拦截保护。
        if (trackingGesture) requestParentIntercept(false)
        trackingGesture = false
        handedToParent = false
        pulling = false
        gestureAxis = AXIS_UNDECIDED
        activePointerId = MotionEvent.INVALID_POINTER_ID
        viewPagerParent = null
    }

    override fun onDetachedFromWindow() {
        finishGesture()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val AXIS_UNDECIDED = 0
        const val AXIS_HORIZONTAL = 1
        const val AXIS_VERTICAL = 2
    }
}