package com.kuaigeglobal.epnova.widget.decoration

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:自定义参数的分割线
 * @Data: 2025/7/4/周五  15:28
 * @Version: 1.0
 * *****************************************
 */
class CustomDividerDecoration(
    @ColorRes colorRes: Int = R.color.color_121212,
    heightRes: Int = 1.dp,
    marginStart: Int = 0,
    marginEnd: Int = 0
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = colorRes.getResColor()
        style = Paint.Style.FILL
    }
    private val height = heightRes
    private val marginLeft = marginStart
    private val marginRight = marginEnd

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.getChildAdapterPosition(view) < (parent.adapter?.itemCount ?: 0) - 1) {
            outRect.bottom = height
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft + marginLeft
        val right = parent.width - parent.paddingRight - marginRight

        (0 until parent.childCount - 1).forEach { i ->
            val child = parent.getChildAt(i)
            val top = child.bottom.toFloat()
            val bottom = top + height
            c.drawRect(left.toFloat(), top, right.toFloat(), bottom, paint)
        }
    }
}