package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;

/**
 * 用于解决嵌套Scrollview的时候由于多行而产生的滑动冲突问题
 */
public class EditTextWithScrollView extends XEditText {

    private final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private float lastTouchY;
    private boolean dragging;

    public EditTextWithScrollView(Context context) {
        super(context);
    }

    public EditTextWithScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextWithScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                lastTouchY = event.getY();
                dragging = false;
                requestParentIntercept(result && (canScroll(-1) || canScroll(1)));
                break;
            case MotionEvent.ACTION_MOVE:
                int index = event.findPointerIndex(activePointerId);
                if (index < 0) {
                    requestParentIntercept(false);
                    break;
                }
                float y = event.getY(index);
                float delta = lastTouchY - y;
                if (dragging || Math.abs(delta) > touchSlop) {
                    dragging = true;
                    // 只在当前拖动方向已经到边界时交给父布局，不修改原始事件。
                    if (delta != 0) requestParentIntercept(result && canScroll(delta > 0 ? 1 : -1));
                    lastTouchY = y;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int liftedIndex = event.getActionIndex();
                if (event.getPointerId(liftedIndex) == activePointerId) {
                    int nextIndex = liftedIndex == 0 ? 1 : 0;
                    activePointerId = event.getPointerId(nextIndex);
                    lastTouchY = event.getY(nextIndex);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                dragging = false;
                requestParentIntercept(false);
                break;
        }
        return result;
    }

    private boolean canScroll(int direction) {
        return getLayout() != null && canScrollVertically(direction);
    }

    private void requestParentIntercept(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(disallow);
    }
}
