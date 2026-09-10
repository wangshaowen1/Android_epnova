package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText

/** 分格绘制验证码，输入、选区、字体和无障碍语义仍由 EditText 提供。 */
class CodeInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fontMetrics = Paint.FontMetrics()
    private val borderHeight = resources.displayMetrics.density
    val codeLength: Int
        get() = (filters.firstOrNull { it is InputFilter.LengthFilter } as? InputFilter.LengthFilter)?.max ?: 4

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val length = codeLength
        if (length <= 0) return
        val contentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0).toFloat()
        val unitWidth = contentWidth / (5f * length - 2f)
        val codeWidth = unitWidth * 3f
        val step = unitWidth * 5f
        val bottom = (height - paddingBottom).toFloat()
        val selected = selectionStart.coerceIn(0, length - 1)
        val rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
        borderPaint.color = currentTextColor
        val colorAlpha = borderPaint.alpha
        for (index in 0 until length) {
            val left = paddingLeft + (if (rtl) length - 1 - index else index) * step
            val focused = isFocused && isEnabled && index == selected
            borderPaint.alpha = if (focused) colorAlpha else (colorAlpha * 0.5f).toInt()
            canvas.drawRect(left, bottom - borderHeight * (if (focused) 2f else 1f),
                left + codeWidth, bottom, borderPaint)
        }
        // 使用 TextView 的画笔跟随主题、字号和字体；绘制期间不拆分字符串或创建列表。
        paint.color = currentTextColor
        paint.getFontMetrics(fontMetrics)
        val baseline = paddingTop + (height - paddingTop - paddingBottom) / 2f -
            (fontMetrics.ascent + fontMetrics.descent) / 2f
        val value = text ?: return
        for (index in 0 until minOf(value.length, length)) {
            val textWidth = paint.measureText(value, index, index + 1)
            val slot = if (rtl) length - 1 - index else index
            val x = paddingLeft + slot * step + (codeWidth - textWidth) / 2f
            canvas.drawText(value, index, index + 1, x, baseline, paint)
        }
    }
}
