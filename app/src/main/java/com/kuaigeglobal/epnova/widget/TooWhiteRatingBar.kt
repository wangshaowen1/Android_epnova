package com.kuaigeglobal.epnova.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kuaigeglobal.epnova.R
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 评分进度条控件
 * @createtime 2024年10月16日 11:45
 * @author TooWhiteT
 */

class TooWhiteRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.ratingBarStyle
) : View(context, attrs, defStyleAttr) {

    private val DEFAULT_RATING_MAX = 10f
    private val DEFAULT_RATING_CURRENT = 0f
    private val DEFAULT_STAR_NUM = 5
    private val DEFAULT_STAR_SPACE = 30
    private val DEFAULT_STAR_WIDTH = 30
    private val DEFAULT_STAR_HEIGHT = 30

    // 评分形状
    private var ratingDrawable: Drawable? = null
    private var fillColor: Int = Color.BLACK
    private var noneColor: Int = Color.GRAY
    private var ratingShapeWidth: Int = DEFAULT_STAR_WIDTH
    private var ratingShapeHeight: Int = DEFAULT_STAR_HEIGHT

    private var ratingMax: Float = DEFAULT_RATING_MAX
    private var ratingCurrent: Float = DEFAULT_RATING_CURRENT
    private var starNum: Int = DEFAULT_STAR_NUM
    private var starSpace: Int = DEFAULT_STAR_SPACE
    // 默认不开启步长定位
    private var isStep: Boolean = false

    // 评分填充色
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // 默认填充色
    private var noneColorFilter = PorterDuffColorFilter(noneColor, PorterDuff.Mode.SRC_IN)
    // 进度混合模式
    private val maskXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    init {
        // 软件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val attr = context.obtainStyledAttributes(attrs, R.styleable.TooWhiteRatingBar)
        ratingDrawable = attr.getDrawable(R.styleable.TooWhiteRatingBar_rating_shape)
        fillColor = attr.getColor(R.styleable.TooWhiteRatingBar_rating_fill_color, Color.BLACK)
        noneColor = attr.getColor(R.styleable.TooWhiteRatingBar_rating_none_color, Color.GRAY)

        noneColorFilter = PorterDuffColorFilter(noneColor, PorterDuff.Mode.SRC_IN)

        ratingMax = attr.getFloat(R.styleable.TooWhiteRatingBar_rating_max, DEFAULT_RATING_MAX)
        ratingCurrent = attr.getFloat(R.styleable.TooWhiteRatingBar_rating_current, DEFAULT_RATING_CURRENT)

        starNum = attr.getInt(R.styleable.TooWhiteRatingBar_rating_star_num, DEFAULT_STAR_NUM)
        starSpace = attr.getDimensionPixelSize(R.styleable.TooWhiteRatingBar_rating_star_space, DEFAULT_STAR_SPACE)
        ratingShapeWidth = attr.getDimensionPixelSize(R.styleable.TooWhiteRatingBar_rating_star_width, DEFAULT_STAR_WIDTH)
        ratingShapeHeight = attr.getDimensionPixelSize(R.styleable.TooWhiteRatingBar_rating_star_height, DEFAULT_STAR_HEIGHT)
        attr.recycle()

        // 相等则支持半步区间
        isStep = starNum == ratingMax.toInt()

    }

    var onRatingChanged: (rating: Float) -> Unit = {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = (starNum * ratingShapeWidth + (starNum - 1) * starSpace) + paddingLeft + paddingRight
        val h = (ratingShapeHeight + paddingTop + paddingBottom)
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制灰色底层星星
        var x = paddingLeft.toFloat()
        val y = paddingTop.toFloat()
        for (i in 0 until starNum) {
            val left = x
            val top = y
            val right = x + ratingShapeWidth
            val bottom = top + ratingShapeHeight
            ratingDrawable?.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            canvas.saveLayer(left, top, right, bottom, null)
            ratingDrawable?.draw(canvas)
            // 给灰底上色
//            canvas.drawRect(left, top, right, bottom, starPaint)
            canvas.restore()
            x += ratingShapeWidth + starSpace
        }

        // 创建混合模式层（绘制填充区域）
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 绘制星形蒙版（Destination）
        x = paddingLeft.toFloat()
        for (i in 0 until starNum) {
            val left = x
            val top = y
            val right = x + ratingShapeWidth
            val bottom = top + ratingShapeHeight
            ratingDrawable?.colorFilter = noneColorFilter
            ratingDrawable?.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            ratingDrawable?.draw(canvas)
            x += ratingShapeWidth + starSpace
        }

        // 绘制填充进度矩形（Source），并混合
        val progressWidth = calculateProgressWidth()

        fillPaint.color = fillColor
        fillPaint.xfermode = maskXfermode
        canvas.drawRect(0f, 0f, progressWidth, height.toFloat(), fillPaint)
        fillPaint.xfermode = null // 重置混合模式

        canvas.restoreToCount(layerId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> {
                if (isStep) {
                    val newRating = (event.x / width) * ratingMax
                    val half = roundToHalfStep(newRating) // 四舍五入到0.5步长
                    if (half != ratingCurrent) {
                        ratingCurrent = half
                        invalidate()
                        onRatingChanged(ratingCurrent)
                    }
                } else {
                    val newRating = (event.x / width * ratingMax).coerceIn(0f, ratingMax)
                    if (newRating != ratingCurrent) {
                        ratingCurrent = newRating
                        invalidate()
                        onRatingChanged(ratingCurrent)
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun roundToHalfStep(value: Float): Float {
        return (value * 2).roundToInt() / 2f
    }

    /**
     * 计算填充进度条的宽度
     * 根据评分星星形状的实际布局（宽度、间距、padding）
     */
    private fun calculateProgressWidth(): Float {
        // 每颗星星对应的评分值
        val ratingPerStar = ratingMax / starNum
        // 当前评分对应多少颗星
        val currentStars = ratingCurrent / ratingPerStar
        // 整数部分：完整的星星数量
        val fullStars = floor(currentStars).toInt()
        // 小数部分：部分填充的星星比例
        val partialStar = currentStars - fullStars
        // 计算总宽度
        // 完整星星的宽度
        val fullStarsWidth = fullStars * ratingShapeWidth
        // 完整星星之间的间距（n颗星有n-1个间距）
        val fullStarsSpacing = if (fullStars > 0) (fullStars - 1) * starSpace else 0
        // 部分填充星星的宽度
        val partialStarWidth = partialStar * ratingShapeWidth
        // 如果有部分填充，需要加上它前面的间距
        val partialStarSpacing = if (partialStar > 0 && fullStars > 0) starSpace else 0
        // 总宽度 = padding + 完整星星宽度 + 完整星星间距 + 部分星星间距 + 部分星星宽度
        return paddingLeft + fullStarsWidth + fullStarsSpacing + partialStarSpacing + partialStarWidth
    }

    fun setSpace(space: Int) {
        this.starSpace = space
        requestLayout()
        invalidate()
    }

    fun setSize(num: Int) {
        this.starNum = num
        requestLayout()
        invalidate()
    }

    fun getSize() = this.starNum

    fun setRating(current: Float) {
        this.ratingCurrent = current
        invalidate()
    }

    fun getCurrentRating() = this.ratingCurrent

    fun setMaxRating(max: Float) {
        this.ratingMax = max
        invalidate()
    }

    fun getMaxRating() = this.ratingMax
}