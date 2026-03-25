package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import com.kuaigeglobal.epnova.R

/**
 * 边缘阴影效果的view
 */
open class FadeScrollView :NestedScrollView {


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    ) {
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.FadeScrollView, defStyle, 0)
        horizontalFadeEdgeEnabled = array.getBoolean(R.styleable.FadeScrollView_horizontalFadingEdgeEnabled, horizontalFadeEdgeEnabled)
        verticalFadeEdgeEnabled = array.getBoolean(R.styleable.FadeScrollView_verticalFadingEdgeEnabled, verticalFadeEdgeEnabled)
        topFadeEdgeStrength = array.getFloat(R.styleable.FadeScrollView_topFadingEdgeStrength, topFadingEdgeStrength)
        bottomFadeEdgeStrength = array.getFloat(R.styleable.FadeScrollView_bottomFadingEdgeStrength, bottomFadeEdgeStrength)
        leftFadeEdgeStrength = array.getFloat(R.styleable.FadeScrollView_leftFadingEdgeStrength, leftFadeEdgeStrength)
        rightFadeEdgeStrength = array.getFloat(R.styleable.FadeScrollView_rightFadingEdgeStrength, rightFadeEdgeStrength)
        array.recycle()
        isHorizontalFadingEdgeEnabled = horizontalFadeEdgeEnabled
        isVerticalFadingEdgeEnabled = verticalFadeEdgeEnabled
    }

    companion object {
        //渐变从0-1.0
        private const val DEFAULT_FADING_EDGE_ENABLED = false
        private const val DEFAULT_FADING_EDGE_STRENGTH = 0.9f
    }

    private var horizontalFadeEdgeEnabled = DEFAULT_FADING_EDGE_ENABLED
    private var verticalFadeEdgeEnabled = DEFAULT_FADING_EDGE_ENABLED
    private var topFadeEdgeStrength = DEFAULT_FADING_EDGE_STRENGTH
    private var bottomFadeEdgeStrength = DEFAULT_FADING_EDGE_STRENGTH
    private var leftFadeEdgeStrength = DEFAULT_FADING_EDGE_STRENGTH
    private var rightFadeEdgeStrength = DEFAULT_FADING_EDGE_STRENGTH

    init {

    }

    override fun getTopFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(verticalFadeEdgeEnabled, topFadeEdgeStrength)
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(verticalFadeEdgeEnabled, bottomFadeEdgeStrength)
    }

    override fun getLeftFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(horizontalFadeEdgeEnabled, leftFadeEdgeStrength)
    }

    override fun getRightFadingEdgeStrength(): Float {
        return getFadingEdgeStrength(horizontalFadeEdgeEnabled, rightFadeEdgeStrength)
    }

    private fun getFadingEdgeStrength(fadeEdgeEnabled: Boolean, fadeEdgeStrength: Float): Float {
        return if (fadeEdgeEnabled) {
            when {
                fadeEdgeStrength <= 0 -> {
                    0f
                }
                fadeEdgeStrength >= 1 -> {
                    1f
                }
                else -> {
                    fadeEdgeStrength
                }
            }
        } else {
            0f
        }
    }
}