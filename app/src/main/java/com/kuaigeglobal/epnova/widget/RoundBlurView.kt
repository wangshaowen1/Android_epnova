package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withClip
import com.kuaigeglobal.epnova.R
import eightbitlab.com.blurview.BlurView

class RoundBlurView : BlurView {
    private var cornerRadius = 0f
    private val bgPath by lazy { Path() }

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
        setWillNotDraw(false)
        attrs ?: return
        context.withStyledAttributes(attrs, R.styleable.RoundBlurView) {
            cornerRadius = getDimensionPixelSize(R.styleable.RoundBlurView_corner_radius, 0).toFloat()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bgRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val innerCornerRadius = cornerRadius.coerceAtLeast(0f)
        bgPath.reset()
        bgPath.addRoundRect(bgRect, innerCornerRadius, innerCornerRadius, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        canvas.withClip(bgPath) { super.draw(canvas) }
    }

}