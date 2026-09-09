package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;


public class HorizontalRecyclerView extends RecyclerView {
    float mDownX = 0;
    float mDownY = 0;
    private ViewParent viewPagerParent;

    public HorizontalRecyclerView(Context context) {
        super(context);
    }

    public HorizontalRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        //dispatch event to child
        super.dispatchTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (viewPagerParent != null) {
                    viewPagerParent.requestDisallowInterceptTouchEvent(false);
                }
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - mDownX);
                float dy = Math.abs(event.getY() - mDownY);
                float moveX = event.getX() - mDownX;
                if (viewPagerParent == null) {
                    viewPagerParent = findViewPagerParent();
                }
                if (dx >= dy) {
                    // 书架仍可继续滚动时由自身消费；到达边界后释放给父 ViewPager2 切 Tab。
                    int direction = moveX > 0 ? -1 : 1;
                    boolean childCanScroll = canScrollHorizontally(direction);
                    getParent().requestDisallowInterceptTouchEvent(childCanScroll);
                    if (viewPagerParent != null) {
                        viewPagerParent.requestDisallowInterceptTouchEvent(childCanScroll);
                    }
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (viewPagerParent != null) {
                        viewPagerParent.requestDisallowInterceptTouchEvent(false);
                    }
                }

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (viewPagerParent != null) {
                    viewPagerParent.requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return true;
    }
    private ViewParent findViewPagerParent() {
        ViewParent parent = getParent();
        while (parent != null) {
            parent = parent.getParent();
            if (parent instanceof ViewPager||parent instanceof ViewPager2) {
                return parent;
            }
        }
        return null;
    }
}
