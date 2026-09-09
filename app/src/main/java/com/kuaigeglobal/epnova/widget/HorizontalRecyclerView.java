package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;


public class HorizontalRecyclerView extends RecyclerView {
    private static final String TAG = "HorizontalRecyclerView";
    private static final int AXIS_UNDECIDED = 0;
    private static final int AXIS_HORIZONTAL = 1;
    private static final int AXIS_VERTICAL = 2;

    float mDownX = 0;
    float mDownY = 0;
    private ViewParent viewPagerParent;
    private Boolean lastDisallowParentIntercept;
    private int moveCount;
    private int gestureAxis = AXIS_UNDECIDED;
    private final int touchSlop;
    private boolean openListOnLeftSwipe;
    private boolean listOpenTriggered;
    private OnReachEndListener onReachEndListener;

    public interface OnReachEndListener {
        void onReachEnd();
    }

    public HorizontalRecyclerView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public HorizontalRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public HorizontalRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setOnReachEndListener(@Nullable OnReachEndListener listener) {
        onReachEndListener = listener;
        openListOnLeftSwipe = false;
        listOpenTriggered = false;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        //dispatch event to child
        boolean handledByRecyclerView = super.dispatchTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 先保证子列表收到后续 MOVE，再根据方向和边界把手势交还给父容器。
                getParent().requestDisallowInterceptTouchEvent(true);
                if (viewPagerParent != null) {
                    viewPagerParent.requestDisallowInterceptTouchEvent(true);
                }
                mDownX = event.getX();
                mDownY = event.getY();
                moveCount = 0;
                gestureAxis = AXIS_UNDECIDED;
                lastDisallowParentIntercept = null;
                openListOnLeftSwipe = isAtRightEdge();
                listOpenTriggered = false;
                viewPagerParent = findViewPagerParent();
                Log.d(TAG, "rv=" + rvId() + " DOWN x=" + event.getX()
                        + " y=" + event.getY()
                        + " itemCount=" + itemCount()
                        + " offset=" + computeHorizontalScrollOffset()
                        + " range=" + computeHorizontalScrollRange()
                        + " parent=" + parentName()
                        + " viewPager=" + viewPagerName()
                        + " horizontallyScrollable=" + hasHorizontalScroll()
                        + " atRightEdge=" + isAtRightEdge()
                        + " openListOnLeftSwipe=" + openListOnLeftSwipe
                        + " handledByRecyclerView=" + handledByRecyclerView
                        + " disallowParent=true");
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX() - mDownX;
                float moveY = event.getY() - mDownY;
                float dx = Math.abs(moveX);
                float dy = Math.abs(moveY);
                moveCount++;
                if (viewPagerParent == null) {
                    viewPagerParent = findViewPagerParent();
                }
                if (gestureAxis == AXIS_UNDECIDED) {
                    if (dx <= touchSlop && dy <= touchSlop) {
                        if (moveCount == 1) {
                            Log.d(TAG, "rv=" + rvId() + " MOVE#" + moveCount
                                    + " waitingForDirection dx=" + dx + " dy=" + dy
                                    + " touchSlop=" + touchSlop
                                    + " disallowParent=true viewPager=" + viewPagerName());
                        }
                        break;
                    }
                    gestureAxis = dx >= dy ? AXIS_HORIZONTAL : AXIS_VERTICAL;
                    Log.d(TAG, "rv=" + rvId() + " axisLocked=" + axisName()
                            + " dx=" + dx + " dy=" + dy
                            + " touchSlop=" + touchSlop
                            + " viewPager=" + viewPagerName());
                }

                if (gestureAxis == AXIS_HORIZONTAL) {
                    // 中间位置由书架消费；右端再次左滑打开列表，左端向右滑仍可交给父 ViewPager2。
                    int direction = moveX > 0 ? -1 : 1;
                    boolean childCanScroll = canScrollHorizontally(direction);
                    boolean atRightEdge = isAtRightEdge();
                    boolean shouldOpenShelfList = direction == 1
                            && atRightEdge
                            && openListOnLeftSwipe
                            && !listOpenTriggered;
                    if (shouldOpenShelfList) {
                        listOpenTriggered = true;
                        Log.d(TAG, "rv=" + rvId()
                                + " openShelfList direction=" + direction
                                + " offset=" + computeHorizontalScrollOffset()
                                + " viewPager=" + viewPagerName());
                        getParent().requestDisallowInterceptTouchEvent(true);
                        if (viewPagerParent != null) {
                            viewPagerParent.requestDisallowInterceptTouchEvent(true);
                        }
                        postOpenShelfList();
                        break;
                    }
                    // 右端第一次左滑只保持在末尾，不能让外层 ViewPager2 抢走这次手势。
                    boolean holdAtRightEdge = direction == 1 && atRightEdge;
                    boolean disallowParent = childCanScroll || holdAtRightEdge;
                    if (lastDisallowParentIntercept == null
                            || lastDisallowParentIntercept != disallowParent) {
                        Log.d(TAG, "rv=" + rvId() + " MOVE#" + moveCount
                                + " dx=" + dx + " dy=" + dy + " moveX=" + moveX
                                + " direction=" + direction
                                + " canScroll=" + childCanScroll
                                + " canLeft=" + canScrollHorizontally(-1)
                                + " canRight=" + canScrollHorizontally(1)
                                + " offset=" + computeHorizontalScrollOffset()
                                + " extent=" + computeHorizontalScrollExtent()
                                + " range=" + computeHorizontalScrollRange()
                                + " horizontallyScrollable=" + hasHorizontalScroll()
                                + " atRightEdge=" + atRightEdge
                                + " holdAtRightEdge=" + holdAtRightEdge
                                + " disallowParent=" + disallowParent
                                + " openListOnLeftSwipe=" + openListOnLeftSwipe
                                + " viewPager=" + viewPagerName());
                    }
                    lastDisallowParentIntercept = disallowParent;
                    getParent().requestDisallowInterceptTouchEvent(disallowParent);
                    if (viewPagerParent != null) {
                        viewPagerParent.requestDisallowInterceptTouchEvent(disallowParent);
                    }
                } else {
                    if (lastDisallowParentIntercept == null || lastDisallowParentIntercept) {
                        Log.d(TAG, "rv=" + rvId() + " MOVE#" + moveCount
                                + " vertical dx=" + dx + " dy=" + dy
                                + " disallowParent=false viewPager=" + viewPagerName());
                    }
                    lastDisallowParentIntercept = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (viewPagerParent != null) {
                        viewPagerParent.requestDisallowInterceptTouchEvent(false);
                    }
                }

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "rv=" + rvId() + " "
                        + (event.getActionMasked() == MotionEvent.ACTION_UP ? "UP" : "CANCEL")
                        + " x=" + event.getX() + " y=" + event.getY()
                        + " moves=" + moveCount
                        + " axis=" + axisName()
                        + " offset=" + computeHorizontalScrollOffset()
                        + " horizontallyScrollable=" + hasHorizontalScroll()
                        + " atRightEdge=" + isAtRightEdge()
                        + " listOpenTriggered=" + listOpenTriggered
                        + " viewPager=" + viewPagerName()
                        + " handledByRecyclerView=" + handledByRecyclerView
                        + " releaseParent=true");
                getParent().requestDisallowInterceptTouchEvent(false);
                if (viewPagerParent != null) {
                    viewPagerParent.requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return true;
    }

    private void postOpenShelfList() {
        OnReachEndListener listener = onReachEndListener;
        if (listener == null) {
            return;
        }
        post(() -> {
            if (isAttachedToWindow()) {
                listener.onReachEnd();
            }
        });
    }

    private String axisName() {
        switch (gestureAxis) {
            case AXIS_HORIZONTAL:
                return "HORIZONTAL";
            case AXIS_VERTICAL:
                return "VERTICAL";
            default:
                return "UNDECIDED";
        }
    }

    private String rvId() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    private int itemCount() {
        return getAdapter() == null ? 0 : getAdapter().getItemCount();
    }

    private boolean isAtRightEdge() {
        return hasHorizontalScroll() && !canScrollHorizontally(1);
    }

    private boolean hasHorizontalScroll() {
        return itemCount() > 0
                && (canScrollHorizontally(-1) || canScrollHorizontally(1));
    }

    private String parentName() {
        ViewParent parent = getParent();
        return parent == null ? "null" : parent.getClass().getSimpleName();
    }

    private String viewPagerName() {
        if (viewPagerParent instanceof ViewPager2) {
            ViewPager2 viewPager2 = (ViewPager2) viewPagerParent;
            return "ViewPager2(current=" + viewPager2.getCurrentItem() + ")";
        }
        if (viewPagerParent instanceof ViewPager) {
            ViewPager viewPager = (ViewPager) viewPagerParent;
            return "ViewPager(current=" + viewPager.getCurrentItem() + ")";
        }
        return "none";
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
