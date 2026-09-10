package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import com.kuaigeglobal.epnova.R

class AtMostScrollView : FadeScrollView {

    private var maxHeight = LayoutParams.MATCH_PARENT
    private var maxWidth = LayoutParams.MATCH_PARENT


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    ) {
        val a =
            context.obtainStyledAttributes(attributeSet, R.styleable.AtMostScrollView, defStyle, 0)
        maxHeight = a.getDimensionPixelSize(R.styleable.AtMostScrollView_maxHeight, maxHeight)
        maxWidth = a.getDimensionPixelSize(R.styleable.AtMostScrollView_maxWidth, maxWidth)
        a.recycle()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(constrainSpec(widthSpec, maxWidth), constrainSpec(heightSpec, maxHeight))
    }

    private fun constrainSpec(spec: Int, maximum: Int): Int {
        val mode = MeasureSpec.getMode(spec)
        if (maximum <= 0 || mode == MeasureSpec.EXACTLY) return spec
        val size = if (mode == MeasureSpec.AT_MOST) minOf(MeasureSpec.getSize(spec), maximum) else maximum
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST)
    }
}
