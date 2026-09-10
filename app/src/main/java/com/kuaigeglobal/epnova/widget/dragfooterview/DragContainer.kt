package com.kuaigeglobal.epnova.widget.dragfooterview

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import kotlin.math.abs

/**
 * RecyclerView 右侧边界的左拉触发容器。
 * XML 中依次放置两个直接子 View：右侧提示布局、RecyclerView，提示宽度即触发阈值。
 * 容器负责手势、位移和回弹；提示内容及触发后的操作由调用方处理，并提供等价点击入口。
 */
class DragContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private lateinit var items: RecyclerView
    private lateinit var hint: View
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var onDrag: (() -> Boolean)? = null
    private var holdOnTrigger = false
    private var holdingTrigger = false
    // 防止调用方未完成操作也未主动结束，导致内容长期停留在拖出位置。
    private val holdTimeout = Runnable { finishTrigger(animate = true) }
    private var onDragStateChanged: ((readyToTrigger: Boolean, isReset: Boolean) -> Unit)? = null
    private var resetAnimator: ValueAnimator? = null
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var pullOriginX = 0f
    private var horizontalGesture = false
    private var verticalGesture = false
    private var dragging = false
    private var cancelledGesture = false
    private var pullDistance = 0f
    private var readyToTrigger = false
    private val resetDuration: Long
    private val dragDamping: Float

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DragContainer)
        try {
            resetDuration = attributes.getInt(
                R.styleable.DragContainer_dc_reset_animator_duration,
                RESET_DURATION_MS,
            ).toLong().coerceAtLeast(0L)
            dragDamping = attributes.getFloat(
                R.styleable.DragContainer_dc_drag_damp,
                DRAG_DAMPING,
            ).takeIf { it > 0f && it <= 1f } ?: DRAG_DAMPING
        } finally {
            attributes.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        check(childCount == 2 && getChildAt(1) is RecyclerView) {
            "DragContainer requires a footer View followed by a RecyclerView"
        }
        hint = getChildAt(0)
        items = getChildAt(1) as RecyclerView
        renderPull(0f)
    }

    /**
     * 回调返回是否接受操作；传 null 禁用拖动。
     * holdOnTrigger 为 true 时保持当前位置，由调用方在内容不可见时 finishTrigger()；
     * 拒绝操作立即回弹，未收到结束通知则在 2 秒后回弹兜底。
     */
    fun setOnPullTriggerListener(
        listener: (() -> Boolean)?,
        holdOnTrigger: Boolean = false,
    ) {
        reset()
        onDrag = listener
        this.holdOnTrigger = holdOnTrigger
    }

    /** 结束触发后的保持；内容不可见时静默复位，操作取消时用动画归位。 */
    fun finishTrigger(animate: Boolean = false) {
        if (!holdingTrigger) return
        holdingTrigger = false
        removeCallbacks(holdTimeout)
        if (animate) settle() else reset()
    }

    /** 达阈值状态变化或复位时通知；isReset 为 true 时应立即清理提示动画。 */
    fun setOnDragStateChangedListener(
        listener: ((readyToTrigger: Boolean, isReset: Boolean) -> Unit)?,
    ) {
        onDragStateChanged = listener
        listener?.invoke(readyToTrigger, pullDistance == 0f)
    }

    /** 重绑、回收或离屏时清理，避免复用上一次拖动状态。 */
    fun reset() {
        removeCallbacks(holdTimeout)
        holdingTrigger = false
        resetAnimator?.cancel()
        resetAnimator = null
        if (horizontalGesture || dragging) parent?.requestDisallowInterceptTouchEvent(false)
        dragging = false
        horizontalGesture = false
        verticalGesture = false
        cancelledGesture = false
        activePointerId = MotionEvent.INVALID_POINTER_ID
        if (::items.isInitialized) renderPull(0f)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (onDrag == null) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
            return
        }
        // 仍需观察 RecyclerView 到达末尾后的 MOVE；竖向手势继续交给父容器。
        parent?.requestDisallowInterceptTouchEvent(disallowIntercept && horizontalGesture)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (holdingTrigger) return true
        if (onDrag == null || (items.adapter?.itemCount ?: 0) == 0) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 回弹期间吞掉新的手势，防止重复触发或点中移动中的内容。
                if (resetAnimator?.isRunning == true) {
                    cancelledGesture = true
                    return true
                }
                reset()
                activePointerId = event.getPointerId(0)
                downX = event.x
                lastX = downX
                downY = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> cancelledGesture = true
            MotionEvent.ACTION_MOVE -> {
                if (cancelledGesture || verticalGesture) return false
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex < 0) return false
                val x = event.getX(pointerIndex)
                val dx = x - downX
                val dy = event.getY(pointerIndex) - downY
                if (!horizontalGesture) {
                    if (abs(dy) > touchSlop && abs(dy) >= abs(dx)) {
                        verticalGesture = true
                        parent?.requestDisallowInterceptTouchEvent(false)
                        return false
                    }
                    horizontalGesture = abs(dx) > touchSlop && abs(dx) > abs(dy)
                }
                if (horizontalGesture) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    if (x < lastX && !items.canScrollHorizontally(1)) {
                        dragging = true
                        // 只计算抵达末尾之后的距离，不把前面的列表滚动计入触发阈值。
                        pullOriginX = lastX
                        items.stopScroll()
                        renderPull((pullOriginX - x) * dragDamping)
                        return true
                    }
                    lastX = x
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility") // 无独立点击动作；调用方提供等价的无障碍点击入口。
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (holdingTrigger) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!dragging || cancelledGesture) return true
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    cancelledGesture = true
                    settle()
                } else {
                    renderPull((pullOriginX - event.getX(pointerIndex)) * dragDamping)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                cancelledGesture = true
                settle()
            }
            MotionEvent.ACTION_UP -> {
                val callback = onDrag.takeIf {
                    dragging && !cancelledGesture && readyToTrigger && isAttachedToWindow &&
                        (items.adapter?.itemCount ?: 0) > 0 && !items.canScrollHorizontally(1)
                }
                if (callback != null && holdOnTrigger) {
                    dragging = false
                    horizontalGesture = false
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    parent?.requestDisallowInterceptTouchEvent(false)
                    // 保留确认态和拖动位置，让后续页面转场成为主要动效。
                    holdingTrigger = true
                    postDelayed(holdTimeout, HOLD_TIMEOUT_MS)
                    var accepted = false
                    try {
                        accepted = callback()
                    } finally {
                        if (!accepted) finishTrigger(animate = true)
                    }
                } else {
                    settle()
                    callback?.invoke()
                }
            }
            MotionEvent.ACTION_CANCEL -> settle()
        }
        return true
    }

    private fun renderPull(distance: Float) {
        val threshold = hint.width.toFloat().coerceAtLeast(1f)
        pullDistance = distance.coerceIn(0f, threshold * MAX_DRAG_RATE)
        items.translationX = -pullDistance
        hint.translationX = (threshold - pullDistance).coerceAtLeast(0f)
        hint.alpha = (pullDistance / threshold).coerceIn(0f, 1f)
        val ready = pullDistance >= threshold
        if (readyToTrigger != ready || pullDistance == 0f) {
            readyToTrigger = ready
            onDragStateChanged?.invoke(ready, pullDistance == 0f)
        }
    }

    private fun settle() {
        dragging = false
        activePointerId = MotionEvent.INVALID_POINTER_ID
        parent?.requestDisallowInterceptTouchEvent(false)
        if (resetAnimator?.isRunning == true || pullDistance == 0f) return
        resetAnimator = ValueAnimator.ofFloat(pullDistance, 0f).apply {
            duration = resetDuration
            addUpdateListener { renderPull(it.animatedValue as Float) }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val DRAG_DAMPING = 0.65f
        const val MAX_DRAG_RATE = 1.2f
        const val RESET_DURATION_MS = 180
        const val HOLD_TIMEOUT_MS = 2_000L
    }
}
