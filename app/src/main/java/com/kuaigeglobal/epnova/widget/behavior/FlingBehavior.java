package com.kuaigeglobal.epnova.widget.behavior;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.OverScroller;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;

import java.lang.reflect.Field;

/**
 * 解决appbarLayout若干问题：
 * （1）快速滑动appbarLayout会出现回弹
 * （2）快速滑动appbarLayout到折叠状态下，立马下滑，会出现抖动的问题
 * （3）滑动appbarLayout，无法通过手指按下让其停止滑动
 *
 * @author yuruiyin
 * @version 2018/1/3
 */
public class FlingBehavior extends AppBarLayout.Behavior {


    public FlingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    OverScroller scroller = null;

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        boolean result = super.onInterceptTouchEvent(parent, child, ev);

        if (ev.getAction() == MotionEvent.ACTION_CANCEL && ev.getX() == 0f && ev.getY() == 0f && ev.getMetaState() == 0) {

            if (scroller == null) {
                getOverScroller();
            }
            // There is an animation in progress. Stop it.
            if (scroller != null && !scroller.isFinished()) {
                scroller.abortAnimation();
            }
        }

        return result;
    }

    private void getOverScroller() {
        try {
            Field field = getScrollerField();
            field.setAccessible(true);
            scroller = (OverScroller) field.get(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Field getScrollerField() throws NoSuchFieldException {
        Class<?> headerBehaviorType = this.getClass().getSuperclass().getSuperclass().getSuperclass();
        return headerBehaviorType.getDeclaredField("scroller");
    }


}
