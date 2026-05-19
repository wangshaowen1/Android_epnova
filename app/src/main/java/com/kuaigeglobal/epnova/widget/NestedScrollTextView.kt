package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class NestedScrollTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    private var lastY = 0f
    init {
        movementMethod = ScrollingMovementMethod.getInstance()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.y
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - lastY
                // canScrollVertically(1) 表示是否能向上滚动（手指往上拨，看下面的内容）
                // deltaY < 0 表示手指正在往上滑动
                if (deltaY < 0 && !canScrollVertically(1)) {
                    // 到底了，且还在往上滑，交给父容器
                    parent.requestDisallowInterceptTouchEvent(false)
                } else if (deltaY > 0 && !canScrollVertically(-1)) {
                    // 到顶了，且还在往下滑，交给父容器
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    // 还在中间，继续内部滚动，禁止父容器拦截
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }
}