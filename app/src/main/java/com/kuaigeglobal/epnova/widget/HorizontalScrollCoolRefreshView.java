package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.shizhefei.view.coolrefreshview.CoolRefreshView;

public class HorizontalScrollCoolRefreshView extends CoolRefreshView {

    public HorizontalScrollCoolRefreshView(Context context) {
        super(context);
    }

    public HorizontalScrollCoolRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 如果是横向滑动则不拦截事件
        if (isHorizontalScroll(ev)) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean isHorizontalScroll(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                initialY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = Math.abs(ev.getX() - initialX);
                float deltaY = Math.abs(ev.getY() - initialY);
                // 横向滑动距离大于纵向时判定为横向滑动
                return deltaX > deltaY * 1.2; // 敏感度因子
        }
        return false;
    }

    private float initialX, initialY;
}