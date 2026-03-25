package com.kuaigeglobal.epnova.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * 淡入淡出
 */
public class FadeInOutTransformer implements ViewPager2.PageTransformer {

    private static final float DEF_MIN_ALPHA =0.5f;
    private float mMinAlpha = DEF_MIN_ALPHA;

    public FadeInOutTransformer(float mMinAlpha) {
        this.mMinAlpha = mMinAlpha;
    }

    @Override
    public void transformPage(@NonNull View page, float position) {
        if (position < -1f) {//[-Infinity, -1)
            page.setAlpha(mMinAlpha);
        } else if (position <= 1f) {//[-1, 1]
            if (position < 0f) {//[-1, 0)
                page.setAlpha(1f + (1f - mMinAlpha) * position);
            } else { //[0, 1]
                page.setAlpha(1f - (1f - mMinAlpha) * position);
            }
        } else {//(1, +Infinity]
            page.setAlpha(mMinAlpha);
        }
    }
}