package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.graphics.BlurMaskFilter
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.kuaigeglobal.epnova.R


/**
 * Created by Debojyoti Singha on 22,September,2019.
 */
open class BlurTextView : AppCompatTextView {

    private var contextMain: Context? = null
    private var isBlurActive: Boolean = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attributeSet: AttributeSet?) {
        this.contextMain = context

        if (attributeSet != null) {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.BlurTextView)
            isBlurActive = typedArray.getBoolean(R.styleable.BlurTextView_blurEnabled, false)
            settingBlur(this, isBlurActive)
            typedArray.recycle()
        }
    }

    private fun settingBlur(blurTextView: TextView, isBlurActive: Boolean) {
        if (isBlurActive) {
            blurTextView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val radius = blurTextView.textSize / 3
            val filter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            blurTextView.paint.maskFilter = filter
        } else {
            blurTextView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            blurTextView.paint.maskFilter = null
        }
    }

    fun setBlurEnabled(blurActive: Boolean) {
        isBlurActive = blurActive
        settingBlur(this, blurActive)
    }
}
