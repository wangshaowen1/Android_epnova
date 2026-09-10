package com.kuaigeglobal.epnova.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.kuaigeglobal.epnova.R

/**
 * 自动旋转布局
 */
class AutoRotateLayout : FrameLayout {

    /**
     * 是否自动旋转
     */
    var autoRotate = false
        set(value) {
            if (field != value) {
                field = value
                updateRotation()
            }
        }
    private var duration = 5000L
    private var rotateAnimator: Animator? = null

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
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoRotateLayout)
        duration = typedArray.getInt(R.styleable.AutoRotateLayout_duration, 5000).toLong().coerceAtLeast(1L)
        typedArray.recycle()
    }

    private fun startRotate() {
        if (rotateAnimator != null) {
            return
        }
        rotateAnimator = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).also {
            rotateAnimator = it
            it.duration = duration
            it.repeatMode = ValueAnimator.RESTART
            it.repeatCount = ValueAnimator.INFINITE
            it.interpolator = LinearInterpolator()
            it.start()
        }
    }

    private fun stopRotate() {
        rotateAnimator?.cancel()
        rotateAnimator = null
        rotation = 0f
    }

    private fun updateRotation() {
        if (autoRotate && isAttachedToWindow && isShown && windowVisibility == VISIBLE) {
            startRotate()
        } else {
            stopRotate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateRotation()
    }

    override fun onDetachedFromWindow() {
        stopRotate()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateRotation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateRotation()
    }
}
