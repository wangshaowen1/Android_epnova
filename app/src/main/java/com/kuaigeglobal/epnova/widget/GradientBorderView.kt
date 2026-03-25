package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withClip
import com.kuaigeglobal.epnova.R
import kotlin.math.tan

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:渐变边框View
 * @Data: 2025/9/9/周二  17:13
 * @Version: 1.0
 * *****************************************
 */
class GradientBorderView : View {

    private val bgPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val borderPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    var showBorder = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    var borderWidth = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var borderStartColor = Color.TRANSPARENT
    private var borderCenterColor = Color.TRANSPARENT
    private var borderEndColor = Color.TRANSPARENT
    private var borderAngle = 0
    private var backgroundStartColor = Color.TRANSPARENT
    private var backgroundEndColor = Color.TRANSPARENT
    private var backgroundAngle = 0
    private var cornerRadius = 0f
    private val bgPath by lazy { Path() }
    private val borderPath by lazy { Path().apply { fillType = Path.FillType.INVERSE_WINDING } }

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
        setWillNotDraw(false)
        attrs ?: return
        context.withStyledAttributes(attrs, R.styleable.GradientBorderView) {
            borderWidth = getDimensionPixelSize(R.styleable.GradientBorderView_borderWidth, 0).toFloat()
            borderStartColor = getColor(R.styleable.GradientBorderView_borderStartColor, Color.TRANSPARENT)
            borderEndColor = getColor(R.styleable.GradientBorderView_borderEndColor, Color.TRANSPARENT)
            borderAngle = getInteger(R.styleable.GradientBorderView_borderAngle, 0)
            backgroundStartColor = getColor(R.styleable.GradientBorderView_backgroundStartColor, Color.TRANSPARENT)
            borderCenterColor = getColor(R.styleable.GradientBorderView_borderCenterColor, Color.TRANSPARENT)
            backgroundEndColor = getColor(R.styleable.GradientBorderView_backgroundEndColor, Color.TRANSPARENT)
            backgroundAngle = getInteger(R.styleable.GradientBorderView_backgroundAngle, 0)
            cornerRadius = getDimensionPixelSize(R.styleable.GradientBorderView_roundCornerRadius, 0).toFloat()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (borderWidth > 0) {
            if (borderStartColor == Color.TRANSPARENT) {
                borderWidth = 0f
            } else if (borderEndColor == Color.TRANSPARENT) {
                //纯色
                borderPaint.shader = null
                borderPaint.color = borderStartColor
            } else {
                //渐变色
                val endPosition: Pair<Float, Float>
                if (borderAngle <= 0) {
                    endPosition = w.toFloat() to 0f
                } else if (borderAngle >= 90) {
                    endPosition = 0f to h.toFloat()
                } else {
                    val degree = borderAngle / 180f
                    var endX = w.toDouble()
                    //tan(degree) = endY/endX
                    val tan = tan(degree.toDouble())
                    var endY = endX * tan
                    if (endY > h) {
                        endY = h.toDouble()
                        endX = endY / tan
                    }
                    endPosition = endX.toFloat() to endY.toFloat()
                }
                borderPaint.shader = if (borderCenterColor != Color.TRANSPARENT)
                    LinearGradient(0f, 0f, endPosition.first, endPosition.second, intArrayOf(borderStartColor, borderCenterColor, borderEndColor), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                else
                    LinearGradient(0f, 0f, endPosition.first, endPosition.second, borderStartColor, borderEndColor, Shader.TileMode.CLAMP)
                borderPaint.color = Color.WHITE
            }
        }
        if (backgroundStartColor == Color.TRANSPARENT || backgroundEndColor == Color.TRANSPARENT) {
            //纯色
            bgPaint.shader = null
            bgPaint.color = backgroundStartColor
        } else {
            //渐变色
            val bgWidth = w.toFloat() - borderWidth * 2
            val bgHeight = h.toFloat() - borderWidth * 2
            val endPosition: Pair<Float, Float>
            if (backgroundAngle <= 0) {
                endPosition = bgWidth to 0f
            } else if (backgroundAngle >= 90) {
                endPosition = 0f to bgHeight
            } else {
                val degree = backgroundAngle / 180f
                var endX = bgWidth.toDouble()
                //tan(degree) = endY/endX
                val tan = tan(degree.toDouble())
                var endY = endX * tan
                if (endY > bgHeight) {
                    endY = bgHeight.toDouble()
                    endX = bgHeight / tan
                }
                endPosition = endX.toFloat() to endY.toFloat()
            }
            bgPaint.shader = LinearGradient(
                borderWidth,
                borderWidth,
                borderWidth + endPosition.first,
                borderWidth + endPosition.second,
                backgroundStartColor,
                backgroundEndColor,
                Shader.TileMode.CLAMP
            )
            bgPaint.color = Color.WHITE
        }
        val bgRect = RectF(borderWidth, borderWidth, w - borderWidth, h - borderWidth)
        val innerCornerRadius = (cornerRadius - borderWidth).coerceAtLeast(0f)
        borderPath.reset()
        borderPath.fillType = Path.FillType.INVERSE_WINDING
        borderPath.addRoundRect(bgRect, innerCornerRadius, innerCornerRadius, Path.Direction.CW)
        bgPath.reset()
        bgPath.addRoundRect(bgRect, innerCornerRadius, innerCornerRadius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showBorder && borderWidth > 0) {
            canvas.withClip(bgPath) { drawPaint(bgPaint) }
        } else {
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, bgPaint)
        }
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        if (showBorder && borderWidth > 0) {
            canvas.withClip(borderPath) {
                if (cornerRadius > 0) {
                    drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, borderPaint)
                } else {
                    drawPaint(borderPaint)
                }
            }
        }
    }
}