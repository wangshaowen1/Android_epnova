package com.kuaigeglobal.epnova.common.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Pair
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:CommonNavigator工具类
 * @Data: 2025/5/16  10:34
 * @Version: 1.0
 * *****************************************
 */
object CommonNavigatorUtils {

    fun getCommonNavigator(
        context: Context?,
        data: List<String>?,
        vp: ViewPager,
        indicatorView: IPagerIndicator? = null,
        titleViewCreator: ((Context, Int, String) -> IPagerTitleView)? = null,
        isBold: Boolean = true,
        normalColor: Int = R.color.color_60_121212.getResColor(),
        selectedColor: Int = R.color.color_121212.getResColor(),
        textSize: Float = 16f,
        indicatorWidth: Float = 30f,
        gravity: Int = Gravity.CENTER
    ): CommonNavigator? {
        if (context == null) return null
        val navigator = CommonNavigator(context)
        navigator.isAdjustMode = false
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int {
                return data?.size ?: 0
            }

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                return titleViewCreator?.invoke(context, index, data?.getOrNull(index) ?: "")
                    ?: getDefaultTitleView(
                        context, index, data?.getOrNull(index), vp,
                        isBold, normalColor, selectedColor, textSize, gravity
                    )
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                if (indicatorView != null) {
                    return indicatorView
                }
                return getLinePagerIndicator(context, indicatorWidth)
            }
        }
        return navigator
    }

    private fun getDefaultTitleView(
        context: Context,
        index: Int,
        text: String?,
        vp: ViewPager,
        isBold: Boolean,
        normalColor: Int,
        selectedColor: Int,
        textSize: Float,
        gravity: Int
    ): SimplePagerTitleView {
        val titleView = SimplePagerTitleView(context)
        titleView.setIsSelectedBold(isBold)
        titleView.setPadding(12.dp, 0, 12.dp, 0)
        titleView.setLines(1)
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
        titleView.gravity = gravity
        titleView.setTypeface(null, Typeface.BOLD)
        titleView.text = text
        titleView.normalColor = normalColor
        titleView.selectedColor = selectedColor
        titleView.setOnClickListener { vp.setCurrentItem(index, true) }
        return titleView
    }


    fun getCommonNavigatorVp2(
        context: Context?,
        data: List<String>?,
        vp: ViewPager2,
        indicatorView: IPagerIndicator? = null,
        isBold: Boolean = true,
        normalColor: Int = R.color.text_color_3_dark.getResColor(),
        selectedColor: Int = R.color.text_color_1_dark.getResColor(),
        textSize: Float = 16f,
        indicatorWidth: Float = 30f,
        gravity: Int = Gravity.CENTER,
        onTitleClick: ((index: Int) -> Unit)? = null
    ): CommonNavigator? {
        if (context == null) return null
        val navigator = CommonNavigator(context)
        navigator.isAdjustMode = false
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int {
                return data?.size ?: 0
            }

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = SimplePagerTitleView(context)
                titleView.setIsSelectedBold(isBold)
                titleView.setPadding(12.dp, 0, 12.dp, 0)
                titleView.setLines(1)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                titleView.gravity = gravity

                titleView.text = data?.getOrNull(index)
                titleView.normalColor = normalColor
                titleView.selectedColor = selectedColor
                titleView.setOnClickListener {
                    vp.currentItem = index
                    onTitleClick?.invoke(index)
                }

                return titleView
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                if (indicatorView != null) {
                    return indicatorView
                }
                return getLinePagerIndicator(context, indicatorWidth)
            }
        }
        return navigator
    }


    fun getLinePagerIndicator(context: Context, indicatorWidth: Float): LinePagerIndicator {
        val indicator = LinePagerIndicator(context)
        indicator.mode = LinePagerIndicator.MODE_EXACTLY
        indicator.startInterpolator = AccelerateInterpolator()
        indicator.endInterpolator = DecelerateInterpolator(1.6f)
        indicator.lineHeight = 3.dp.toFloat()
        indicator.lineWidth = indicatorWidth.dp.toFloat()
        indicator.roundRadius = 2.dp.toFloat()
        indicator.yOffset = 4.dp.toFloat()
        indicator.setColors(R.color.theme_color_1_dark.getResColor())
        return indicator
    }

    fun getLinePagerIndicatorGradient(context: Context?): LinePagerIndicator? {
        if (context == null) return null
        val indicator = LinePagerIndicator(context)
        indicator.mode = LinePagerIndicator.MODE_EXACTLY
        indicator.startInterpolator = AccelerateInterpolator()
        indicator.endInterpolator = DecelerateInterpolator(1.6f)
        indicator.lineHeight = 4.dp.toFloat()
        indicator.lineWidth = 12.dp.toFloat()
        indicator.roundRadius = 2.dp.toFloat()
        indicator.yOffset = 1.dp.toFloat()
        indicator.setColors(R.color.theme_color_1_dark.getResColor())
        //indicator.setGradientColors(
        //    Pair(
        //        R.color.color_ff4332.getResColor(),
        //        R.color.color_ffa029.getResColor()
        //    )
        //)
        return indicator
    }
}