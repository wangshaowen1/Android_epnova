package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.kuaigeglobal.epnova.R

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:虚线控件
 * @Data: 2025/5/28  11:09
 * @Version: 1.0
 * *****************************************
 */
class VerticalDashedLineView (context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.parseColor("#FFBE6E")
        // 2dp 线宽
        strokeWidth = 2.dpToPx()
        style = Paint.Style.STROKE
        // 8dp虚线+4dp间隔
        pathEffect = DashPathEffect(floatArrayOf(8.dpToPx(), 4.dpToPx()), 0f)
    }

    init {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        val vdv = context.obtainStyledAttributes(attrs, R.styleable.VerticalDashedLineView)
        val vdvColor = vdv.getColor(R.styleable.VerticalDashedLineView_vdvColor,
            ContextCompat.getColor(context, R.color.color_ffbe6e))
        paint.color = vdvColor
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        canvas.drawLine(centerX, 0f, centerX, height.toFloat(), paint)
    }

    private fun Int.dpToPx(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics
    )

    // VerticalDashedLineView.kt
    fun setVdvColor(colorRes: Int) {
        // 判断传入的是资源 ID 还是直接的颜色值
        val color = if (isColorResource(colorRes)) {
            ContextCompat.getColor(context, colorRes)
        } else {
            colorRes
        }
        paint.color = color
        invalidate()
    }

    // 检查是否为有效的颜色资源 ID
    private fun isColorResource(resId: Int): Boolean {
        return try {
            context.resources.getResourceTypeName(resId) == "color"
        } catch (e: Exception) {
            false
        }
    }
}
