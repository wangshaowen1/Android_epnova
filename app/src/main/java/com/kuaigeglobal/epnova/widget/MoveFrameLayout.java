package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kuaigeglobal.epnova.R;

/** 可拖拽并吸边的悬浮容器，点击仍经过标准 performClick 分发。 */
public class MoveFrameLayout extends FrameLayout {
    private final boolean isAttach;
    private final boolean isDrag;
    private final int touchSlop;
    private float downRawX;
    private float downRawY;
    private float originX;
    private float originY;
    private boolean dragging;
    private boolean cancelled;
    private OnMfClickListener onClickListener;

    public MoveFrameLayout(@NonNull Context context) { this(context, null); }

    public MoveFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoveFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MoveFrameLayout);
        try {
            isAttach = a.getBoolean(R.styleable.MoveFrameLayout_IsAttach, true);
            isDrag = a.getBoolean(R.styleable.MoveFrameLayout_IsDrag, true);
        } finally {
            a.recycle();
        }
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private void beginGesture(MotionEvent event) {
        animate().cancel();
        dragging = false;
        cancelled = false;
        downRawX = event.getRawX();
        downRawY = event.getRawY();
        originX = getX();
        originY = getY();
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
    }

    private boolean passedSlop(MotionEvent event) {
        return Math.hypot(event.getRawX() - downRawX, event.getRawY() - downRawY) > touchSlop;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isDrag || !isEnabled()) return super.onInterceptTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                beginGesture(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                cancelled = true;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!cancelled && passedSlop(event)) {
                    dragging = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrag || !isEnabled()) return super.onTouchEvent(event);
        ViewGroup parent = getParent() instanceof ViewGroup ? (ViewGroup) getParent() : null;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                beginGesture(event);
                setPressed(true);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                cancelled = true;
                finishGesture(parent, false);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (cancelled || parent == null) return true;
                if (!dragging && passedSlop(event)) dragging = true;
                if (dragging) {
                    setPressed(false);
                    parent.requestDisallowInterceptTouchEvent(true);
                    // 使用屏幕位移，避免控件移动后局部坐标变化导致抖动。
                    setX(originX + event.getRawX() - downRawX);
                    float minY = parent.getPaddingTop();
                    float maxY = Math.max(minY, parent.getHeight() - getHeight()
                            - parent.getPaddingBottom() - dp2px(15));
                    setY(Math.max(minY, Math.min(maxY, originY + event.getRawY() - downRawY)));
                }
                return true;
            case MotionEvent.ACTION_UP:
                boolean click = !cancelled && !dragging && !passedSlop(event);
                finishGesture(parent, !cancelled);
                if (click) performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelled = true;
                finishGesture(parent, false);
                return true;
            default:
                return true;
        }
    }

    private void finishGesture(@Nullable ViewGroup parent, boolean animateSnap) {
        setPressed(false);
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
            if (dragging && isAttach) {
                float left = parent.getPaddingLeft();
                float right = Math.max(left, parent.getWidth() - getWidth() - parent.getPaddingRight());
                float target = getX() < (left + right) / 2f ? left : right;
                if (animateSnap) {
                    animate().x(target).setDuration(400)
                            .setInterpolator(new OvershootInterpolator()).start();
                } else {
                    setX(target);
                }
            }
        }
        dragging = false;
    }

    @Override
    public boolean performClick() {
        boolean handled = super.performClick();
        if (onClickListener != null) {
            onClickListener.onClick(this);
            return true;
        }
        return handled;
    }

    @Override
    protected void onDetachedFromWindow() {
        animate().cancel();
        cancelled = true;
        finishGesture(getParent() instanceof ViewGroup ? (ViewGroup) getParent() : null, false);
        super.onDetachedFromWindow();
    }

    public int dp2px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    public void setOnMfClickListener(OnMfClickListener listener) {
        onClickListener = listener;
        if (listener != null) {
            setClickable(true);
            setFocusable(true);
        }
    }

    public interface OnMfClickListener {
        void onClick(View view);
    }
}
