package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.InputFilter
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.kuaigeglobal.epnova.R

class CodeInputView : AppCompatEditText {

    private val borderPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }
    }
    private val codePaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#292929")
            textSize = context.resources.getDimensionPixelSize(R.dimen.dp20).toFloat()
            isFakeBoldText = true
        }
    }
    private val borderHeight by lazy { context.resources.getDimensionPixelSize(R.dimen.dp1) }
    val codeLength get() = filters?.firstOrNull { it is InputFilter.LengthFilter }?.let { it as? InputFilter.LengthFilter }?.max ?: 4

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
    }

    override fun onDraw(canvas: Canvas) {
        val unitWidth = width.toFloat() / (3 * codeLength + 2 * (codeLength - 1))
        val codeWidth = unitWidth * 3
        val dividerWidth = unitWidth * 2
        (0 until codeLength).forEach {
            val left = it * (codeWidth + dividerWidth)
            canvas.drawRect(left, height.toFloat() - borderHeight, left + codeWidth, height.toFloat(), borderPaint)
        }
        val textHeight = codePaint.fontMetrics.let { it.bottom - it.top }
        val y = (height - textHeight) / 2 - codePaint.fontMetrics.top
        text?.split("")?.filterNot { it.isEmpty() }?.forEachIndexed { index, code ->
            val textWidth = codePaint.measureText(code)
            val x = index * (codeWidth + dividerWidth) + (codeWidth - textWidth) / 2
            canvas.drawText(code, x, y, codePaint)
        }
    }

}