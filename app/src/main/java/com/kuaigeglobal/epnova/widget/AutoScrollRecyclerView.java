package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nhsd.basic.ext.CoreUtils;

public class AutoScrollRecyclerView extends RecyclerView {
    private boolean started;
    private boolean touching;
    private long lastFrameTime;
    private float pendingDistance;
    // 保留原来每 10ms 移动 2dp 的速度，位移按实际时间计算。
    private final float pixelsPerMs = CoreUtils.getDp(2f) / 10f;

    public AutoScrollRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoScrollRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private int direction() {
        return getLayoutManager() instanceof LinearLayoutManager
                && ((LinearLayoutManager) getLayoutManager()).getReverseLayout() ? -1 : 1;
    }

    private boolean canRun() {
        return started && !touching && isAttachedToWindow() && isShown()
                && getWindowVisibility() == VISIBLE && getScrollState() == SCROLL_STATE_IDLE
                && getAdapter() != null && getAdapter().getItemCount() > 0
                && (canScrollHorizontally(direction()) || canScrollVertically(direction()));
    }

    private final Runnable runningTask = new Runnable() {
        @Override public void run() {
            if (!canRun()) {
                lastFrameTime = 0;
                return;
            }
            long now = SystemClock.uptimeMillis();
            long elapsed = lastFrameTime == 0 ? 0 : Math.min(50L, now - lastFrameTime);
            lastFrameTime = now;
            pendingDistance += pixelsPerMs * elapsed;
            int distance = (int) pendingDistance;
            pendingDistance -= distance;
            int sign = direction();
            if (distance > 0) {
                scrollBy(canScrollHorizontally(sign) ? sign * distance : 0,
                        canScrollVertically(sign) ? sign * distance : 0);
            }
            if (canRun()) postOnAnimation(this);
        }
    };

    private void updateScheduling() {
        // RecyclerView 构造期间也可能回调 View 生命周期方法。
        if (runningTask == null) return;
        removeCallbacks(runningTask);
        lastFrameTime = 0;
        pendingDistance = 0f;
        if (canRun()) {
            lastFrameTime = SystemClock.uptimeMillis();
            postOnAnimation(runningTask);
        }
    }

    public void start() {
        started = true;
        updateScheduling();
    }

    public void stop() {
        started = false;
        updateScheduling();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touching = true;
            updateScheduling();
        }
        boolean handled = super.dispatchTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL || !handled) {
            touching = false;
            updateScheduling();
        }
        return handled;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        updateScheduling();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // 数据增加或容器尺寸改变后，可以从曾经的边界继续滚动。
        updateScheduling();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateScheduling();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateScheduling();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateScheduling();
    }

    @Override
    protected void onDetachedFromWindow() {
        touching = false;
        // 暂停帧回调，保留 EpNova 已启动控件重新挂载后继续滚动的行为。
        removeCallbacks(runningTask);
        lastFrameTime = 0;
        pendingDistance = 0f;
        super.onDetachedFromWindow();
    }
}
