package com.kuaigeglobal.epnova.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class BlankSpan(private val lineWidth: Int, private val lineColor: Int, private val lineHeight: Int, private val space: Int) : ReplacementSpan() {
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        paint.color = lineColor
        canvas.drawRect(x + space, y.toFloat() + lineHeight, x + space + lineWidth, y.toFloat(), paint)
    }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return lineWidth + space * 2
    }
}
