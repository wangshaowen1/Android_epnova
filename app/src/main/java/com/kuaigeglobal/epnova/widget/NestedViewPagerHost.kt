package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.sign

/**
 * Handles same-direction nested ViewPager2 gestures.
 *
 * The child ViewPager2 keeps horizontal swipes while it can scroll; once it
 * reaches an edge, the gesture is released to the parent ViewPager2.
 */
class NestedViewPagerHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialX = 0f
    private var initialY = 0f

    private val childViewPager: ViewPager2?
        get() = getChildAt(0) as? ViewPager2

    private val parentViewPager: ViewPager2?
        get() {
            var view: View? = parent as? View
            while (view != null && view !is ViewPager2) {
                view = view.parent as? View
            }
            return view as? ViewPager2
        }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        handleInterceptTouchEvent(event)
        return super.onInterceptTouchEvent(event)
    }

    private fun handleInterceptTouchEvent(event: MotionEvent) {
        val parentViewPager = parentViewPager ?: return
        val childViewPager = childViewPager ?: return
        if (childViewPager.orientation != parentViewPager.orientation) return

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initialX
                val dy = event.y - initialY
                val isHorizontal = parentViewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL
                val scaledDx = abs(dx) * if (isHorizontal) 0.5f else 1f
                val scaledDy = abs(dy) * if (isHorizontal) 1f else 0.5f
                if (scaledDx <= touchSlop && scaledDy <= touchSlop) return

                if (isHorizontal == (scaledDx > scaledDy)) {
                    parent.requestDisallowInterceptTouchEvent(canChildScroll(childViewPager, dx, dy))
                } else {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> parent.requestDisallowInterceptTouchEvent(false)
        }
    }

    private fun canChildScroll(viewPager: ViewPager2, dx: Float, dy: Float): Boolean {
        val delta = if (viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL) dx else dy
        if (delta == 0f) return false
        val direction = -delta.sign.toInt()
        return if (viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
            viewPager.canScrollHorizontally(direction)
        } else {
            viewPager.canScrollVertically(direction)
        }
    }
}
