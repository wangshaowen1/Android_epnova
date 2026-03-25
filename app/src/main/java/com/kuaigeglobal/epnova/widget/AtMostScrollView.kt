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
            context.obtainStyledAttributes(attributeSet, R.styleable.AtMostRecyclerView, defStyle, 0)
        maxHeight = a.getDimensionPixelSize(R.styleable.AtMostRecyclerView_maxHeight, maxHeight)
        maxWidth = a.getDimensionPixelSize(R.styleable.AtMostRecyclerView_maxWidth, maxWidth)
        a.recycle()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        var tempWidthSpec = widthSpec
        if (maxWidth > 0) {
            tempWidthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
        }
        var tempHeightSpec = heightSpec
        if (maxHeight > 0) {
            tempHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        }
        super.onMeasure(tempWidthSpec, tempHeightSpec)
    }
}