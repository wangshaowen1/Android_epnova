package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor

/**
 * 播放器圆环进度条
 */
class PlayerRoundProgressBar : View {

    private val dp1 by lazy { 1.dp.toFloat() }
    var currentPosition = 0L
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    var duration = 0L
        set(value) {
            if (value >= 0) {
                field = value
                invalidate()
            }
        }
    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = R.color.status_bar_dark.getResColor()
            style = Paint.Style.STROKE
            strokeWidth = dp1 * 2
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val arcPath by lazy { Path() }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }


    private fun init(context: Context, attrs: AttributeSet?) {
        if (isInEditMode) {
            currentPosition = 1
            duration = 5
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentPosition <= 0 || duration <= 0) {
            return
        }
        arcPath.reset()
        arcPath.addArc(dp1, dp1, width - dp1, height - dp1, -90f, 360f * currentPosition / duration)
        canvas.drawPath(arcPath, paint)
    }

}
