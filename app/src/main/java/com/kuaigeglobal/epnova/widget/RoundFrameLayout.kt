package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.kuaigeglobal.epnova.R

class RoundFrameLayout(context: Context) : FrameLayout(context) {


    private val mPath = Path()

    /**
     * 左上角 圆角大小
     */
    private var leftTopRound: Float = 0f
    /**
     * 右上角 圆角大小
     */
    private var rightTopRound: Float = 0f
    /**
     * 右下角 圆角大小
     */
    private var leftBottomRound: Float = 0f
    /**
     * 左下角 圆角大小
     */
    private var rightBottomRound: Float = 0f

    /**
     * 全部圆角
     */
    private var round: Float = 0f

    constructor(context: Context, attributeSet: AttributeSet) : this(context) {
        val obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.RoundFrameLayout)
        round = obtainStyledAttributes.getDimension(R.styleable.RoundFrameLayout_round, round)
        leftTopRound = obtainStyledAttributes.getDimension(R.styleable.RoundFrameLayout_leftTopRound, leftTopRound)
        rightTopRound = obtainStyledAttributes.getDimension(R.styleable.RoundFrameLayout_rightTopRound, rightTopRound)
        rightBottomRound = obtainStyledAttributes.getDimension(R.styleable.RoundFrameLayout_rightBottomRound, rightBottomRound)
        leftBottomRound = obtainStyledAttributes.getDimension(R.styleable.RoundFrameLayout_leftBottomRound, leftBottomRound)
        if (round != 0f) {
            leftTopRound = round
            rightTopRound = round
            rightBottomRound = round
            leftBottomRound = round
        }
        obtainStyledAttributes.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mPath.reset()
        val radii = floatArrayOf(leftTopRound, leftTopRound, rightTopRound, rightTopRound, rightBottomRound, rightBottomRound, leftBottomRound, leftBottomRound)
        mPath.addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radii, Path.Direction.CW)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val saveCount = canvas.save()
        try {
            canvas.clipPath(mPath)
            super.dispatchDraw(canvas)
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * 优化子控件点击体验
     */
    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }
}
