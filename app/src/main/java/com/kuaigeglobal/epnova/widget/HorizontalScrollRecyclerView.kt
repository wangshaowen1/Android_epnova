package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.dp

class HorizontalScrollRecyclerView : RecyclerView {

    private var scrollPxPerSecond = 30.dp
    var scrollable = true
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    checkScroll()
                } else {
                    scroller.forceFinished(true)
                    scrollTo(0, 0)
                    checkScroll()
                }
            }
        }
    private var touched = false
        set(value) {
            field = value
            checkScroll()
        }
    private val scroller by lazy { Scroller(context, LinearInterpolator()) }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HorizontalScrollRecyclerView)
        scrollPxPerSecond = typedArray.getDimensionPixelSize(R.styleable.HorizontalScrollRecyclerView_distancePerSecond, 30.dp)
        typedArray.recycle()
    }

    override fun dispatchTouchEvent(e: MotionEvent?): Boolean {
        handleTouchEvent(e)
        return super.dispatchTouchEvent(e)
    }

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        handleTouchEvent(e)
        return super.onInterceptTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        handleTouchEvent(e)
        return super.onTouchEvent(e)
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        scroller.forceFinished(true)
        scrollTo(0, 0)
        checkScroll()
    }

    private fun handleTouchEvent(e: MotionEvent?) {
        when (e?.action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                touched = true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touched = false
            }
        }
    }

    private fun checkScroll() {
        val adapter = adapter
        if (adapter == null || adapter.itemCount == 0 || touched || !scrollable || !isAttachedToWindow) {
            return
        }
        start()
    }

    private fun start() {
        lastX = 0
        val reverse = (layoutManager as? LinearLayoutManager)?.reverseLayout ?: false
        scroller.startScroll(0, 0, if (reverse) -scrollPxPerSecond else scrollPxPerSecond, 0, 1000)
        invalidate()
    }

    private var lastX = 0

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.computeScrollOffset()) {
            val currX = scroller.currX
            scrollBy(currX - lastX, 0)
            lastX = currX
        } else {
            checkScroll()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scroller.forceFinished(true)  // 强制停止滚动
    }
}