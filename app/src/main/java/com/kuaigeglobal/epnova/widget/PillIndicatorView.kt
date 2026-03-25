package com.kuaigeglobal.epnova.widget

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.viewpager2.widget.ViewPager2
import com.kuaigeglobal.epnova.R
import kotlin.math.max

/**
 * 胶囊+圆点 风格指示器
 */
class PillIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var activeColor = Color.WHITE
    private var inactiveColor = "#4DFFFFFF".toColorInt() // 半透明白或暗蓝色
    private var indicatorHeight = 20f // 默认高度/圆直径
    private var activeWidth = 60f     // 默认胶囊宽度
    private var indicatorSpacing = 20f // 默认间距

    private var indicatorCount = 0
    private var currentPosition = 0
    private var slideOffset = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val rectF = RectF()
    private val argbEvaluator = ArgbEvaluator()

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.PillIndicatorView)
            activeColor = typedArray.getColor(R.styleable.PillIndicatorView_pi_activeColor, activeColor)
            inactiveColor = typedArray.getColor(R.styleable.PillIndicatorView_pi_inactiveColor, inactiveColor)
            indicatorHeight = typedArray.getDimension(R.styleable.PillIndicatorView_pi_indicatorHeight, indicatorHeight)
            activeWidth = typedArray.getDimension(R.styleable.PillIndicatorView_pi_activeWidth, activeWidth)
            indicatorSpacing = typedArray.getDimension(R.styleable.PillIndicatorView_pi_indicatorSpacing, indicatorSpacing)

            // 确保 activeWidth 至少等于 indicatorHeight（圆）
            activeWidth = max(activeWidth, indicatorHeight)
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (indicatorCount <= 0) {
            setMeasuredDimension(0, 0)
            return
        }

        // 计算总宽度：(数量-1) 个圆的宽度 + 1 个胶囊的宽度 + (数量-1) 个间距
        val desiredWidth = paddingLeft + paddingRight +
                (indicatorCount - 1) * indicatorHeight +
                activeWidth +
                (indicatorCount - 1) * indicatorSpacing

        val desiredHeight = paddingTop + paddingBottom + indicatorHeight

        setMeasuredDimension(
            resolveSize(desiredWidth.toInt(), widthMeasureSpec),
            resolveSize(desiredHeight.toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (indicatorCount <= 0) return

        var currentX = paddingLeft.toFloat()
        val startY = paddingTop.toFloat()
        val endY = startY + indicatorHeight
        val radius = indicatorHeight / 2f

        for (i in 0 until indicatorCount) {
            var width = indicatorHeight
            var color = inactiveColor

            // 核心逻辑：根据当前的 position 和滑动的 offset 动态计算宽度和颜色
            if (i == currentPosition) {
                // 当前正要滑出的指示器（变短，变暗）
                width = indicatorHeight + (activeWidth - indicatorHeight) * (1 - slideOffset)
                color = argbEvaluator.evaluate(slideOffset, activeColor, inactiveColor) as Int
            } else if (i == currentPosition + 1) {
                // 下一个正要滑入的指示器（变长，变亮）
                width = indicatorHeight + (activeWidth - indicatorHeight) * slideOffset
                color = argbEvaluator.evaluate(slideOffset, inactiveColor, activeColor) as Int
            }

            paint.color = color
            rectF.set(currentX, startY, currentX + width, endY)
            canvas.drawRoundRect(rectF, radius, radius, paint)

            // 下一个指示器的起点 X 坐标
            currentX += width + indicatorSpacing
        }
    }

    // ================= 公开 API =================

    /**
     * 手动更新滑动进度（可配合任何滚动组件）
     */
    fun onPageScrolled(position: Int, positionOffset: Float) {
        this.currentPosition = position
        this.slideOffset = positionOffset
        invalidate()
    }

    /**
     * 设置指示器总数
     */
    fun setCount(count: Int) {
        this.indicatorCount = count
        requestLayout() // 数量改变需要重新计算宽度
        invalidate()
    }

    /**
     * 快捷绑定 ViewPager2
     */
    fun attachToViewPager2(viewPager: ViewPager2) {
        setCount(viewPager.adapter?.itemCount ?: 0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // 边界保护，防止越界
                val safePosition = if (position >= indicatorCount - 1) indicatorCount - 1 else position
                val safeOffset = if (position >= indicatorCount - 1) 0f else positionOffset
                this@PillIndicatorView.onPageScrolled(safePosition, safeOffset)
            }

            override fun onPageSelected(position: Int) {
                // 兜底同步状态，防止滑动过快时 offset 没有归零
                this@PillIndicatorView.onPageScrolled(position, 0f)
            }
        })
    }
}