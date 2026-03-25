package com.kuaigeglobal.epnova.widget.htmlview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:垂直居中对齐的 ImageSpan
 * @Data: 2025/8/22/周五  13:16
 * @Version: 1.0
 * *****************************************
 */
class CenterAlignImageSpan(d: Drawable) : ImageSpan(d) {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val d = drawable
        val rect = d.bounds

        if (fm != null) {
            val pfm = paint.fontMetricsInt
            // 计算图片高度和字体高度的差值
            val height = rect.bottom - rect.top
            val need = height - (pfm.descent - pfm.ascent)

            if (need > 0) {
                // 将差值均匀分配到顶部和底部
                val ascent = pfm.ascent - need / 2
                val descent = pfm.descent + need / 2

                fm.ascent = ascent
                fm.descent = descent
                fm.top = ascent
                fm.bottom = descent
            } else {
                // 图片较小，使用字体度量
                fm.ascent = pfm.ascent
                fm.descent = pfm.descent
                fm.top = pfm.top
                fm.bottom = pfm.bottom
            }
        }

        return rect.right
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val d = drawable
        canvas.save()

        // 计算垂直居中位置
        val transY = (y - d.bounds.bottom) + (bottom - top - d.bounds.bottom) / 2

        canvas.translate(x, transY.toFloat())
        d.draw(canvas)
        canvas.restore()
    }
}