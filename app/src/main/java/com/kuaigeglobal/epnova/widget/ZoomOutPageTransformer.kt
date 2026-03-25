package com.kuaigeglobal.epnova.widget

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce: 实现viewpage画廊效果
 * @Data: 2024/6/25 星期二 17:33
 * @Version: 1.0
 * *****************************************
 */
class ZoomOutPageTransformer : ViewPager.PageTransformer {
    companion object {
        //自由控制缩放比例
        const val MAX_SCALE = 1f
        const val MIN_SCALE = 0.8f
    }

    override fun transformPage(page: View, position: Float) {
        if (position <= 1) {
            val scaleFactor = MIN_SCALE + (1 - abs(position)) * (MAX_SCALE - MIN_SCALE);
            page.scaleX = scaleFactor;
            if (position > 0) {
                page.translationX = -scaleFactor * 2;
            } else if (position < 0) {
                page.translationX = scaleFactor * 2;
            }
            page.scaleY = scaleFactor;
        } else {
            page.scaleX = MIN_SCALE;
            page.scaleY = MIN_SCALE;
        }
    }
}