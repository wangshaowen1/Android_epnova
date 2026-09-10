package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class MaxHeightScrollView : ScrollView {
    /** 0 或负数表示不限制高度；父布局的固定尺寸优先。 */
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
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val constrainedHeightSpec = if (maxHeight > 0 && mode != MeasureSpec.EXACTLY) {
            val height = if (mode == MeasureSpec.AT_MOST) {
                minOf(MeasureSpec.getSize(heightMeasureSpec), maxHeight)
            } else {
                maxHeight
            }
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, constrainedHeightSpec)
    }
}
