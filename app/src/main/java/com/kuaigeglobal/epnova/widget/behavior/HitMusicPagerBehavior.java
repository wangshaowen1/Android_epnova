package com.kuaigeglobal.epnova.widget.behavior;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.blankj.utilcode.util.SizeUtils;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.common.ext.ViewExtKt;
import com.google.android.material.appbar.AppBarLayout;
import com.gyf.immersionbar.ImmersionBar;

/**
 * 爆款专用Behavior 其他地方不要用这个
 */
public class HitMusicPagerBehavior extends AppBarLayout.ScrollingViewBehavior {

    TextView toolbarTitle;

    private View appBarLayout;

    public HitMusicPagerBehavior() {
    }

    public HitMusicPagerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {

        if (appBarLayout == null) {
            ViewGroup container = (ViewGroup) parent.getParent();
            appBarLayout = container.findViewById(R.id.toolbar);
            toolbarTitle = container.findViewById(R.id.tvTitle);
        }
        float ratio = -dependency.getY() / SizeUtils.dp2px(50);
        float realRatio = Math.min(1, ratio);
        if (realRatio < 0.3) {
            if(appBarLayout!=null) {
                appBarLayout.setBackgroundColor(Color.TRANSPARENT);
            }
            ImmersionBar.with(ViewExtKt.getActivity(parent)).statusBarDarkFont(false).init();
            resetTitleColor();
        } else {
            ArgbEvaluator bgEvaluator = new ArgbEvaluator();
            int bgColor = (int) bgEvaluator.evaluate(realRatio, Color.TRANSPARENT, Color.WHITE);
            if(appBarLayout!=null) {
                appBarLayout.setBackgroundColor(bgColor);
            }
            ImmersionBar.with(ViewExtKt.getActivity(parent)).statusBarDarkFont(true).init();
            tint(realRatio);
        }
        return super.onDependentViewChanged(parent, child, dependency);
    }

    private void tint(float realRatio) {
        if (appBarLayout != null && toolbarTitle != null) {
            ArgbEvaluator drawableEvaluator = new ArgbEvaluator();
            int tintColor = (int) drawableEvaluator.evaluate(realRatio, Color.WHITE, Color.BLACK);
            toolbarTitle.setTextColor(tintColor);
        }
    }

    private void resetTitleColor() {
        if (appBarLayout != null && toolbarTitle != null) {
            toolbarTitle.setTextColor(Color.WHITE);
        }
    }
}
