package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import kotlin.math.min

class MaxHeightScrollView : ScrollView {
    var maxHeight = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, min(measuredHeight, maxHeight))
    }
}