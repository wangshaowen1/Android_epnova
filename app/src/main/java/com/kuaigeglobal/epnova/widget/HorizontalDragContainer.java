package com.kuaigeglobal.epnova.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.widget.dragfooterview.DefaultDragChecker;
import com.kuaigeglobal.epnova.widget.dragfooterview.DragListener;
import com.kuaigeglobal.epnova.widget.dragfooterview.IDragChecker;
import com.kuaigeglobal.epnova.widget.dragfooterview.footer.BaseFooterDrawer;
import com.kuaigeglobal.epnova.widget.dragfooterview.footer.BezierFooterDrawer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Administrator on 2016/10/31.
 */
public class HorizontalDragContainer extends FrameLayout {

    private static final String TAG = "DragContainer";

    public static final int DRAG_OUT = 10;
    public static final int DRAG_IN = 11;
    public static final int RELEASE = 12;
    private int textOffsetTop;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DRAG_OUT, DRAG_IN, RELEASE})
    public @interface DragState {

    }

    private View contentView;
    private DragListener dragListener;
    private IDragChecker dragChecker;
    private int containerWidth, containerHeight;

    private static final int DEFAULT_BACKGROUND_COLOR = 0xffffffff;

    private static final int DEFAULT_RESET_DURATION = 700;
    private static final int DEFAULT_FOOTER_COLOR = 0xffcdcdcd;
    private static final float DEFAULT_DRAG_DAMP = 0.5f;
    private static final int AXIS_UNDECIDED = 0;
    private static final int AXIS_HORIZONTAL = 1;
    private static final int AXIS_VERTICAL = 2;

    //user define params in xml
    private int footerColor;
    private int resetDuration;
    private float dragDamp;

    private boolean shouldResetContentView;
    private ValueAnimator resetAnimator;

    private float downX, downY;
    private float dragDx;
    private float lastMoveX;
    private int touchSlop;
    private int gestureAxis = AXIS_UNDECIDED;
    private boolean dragging;

    private BaseFooterDrawer footerDrawer;

    public HorizontalDragContainer(Context context) {
        this(context, null);
    }

    public HorizontalDragContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalDragContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setDragListener(DragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setIDragChecker(IDragChecker dragChecker) {
        this.dragChecker = dragChecker;
    }

    private void init(Context context, AttributeSet attrs) {
        setIDragChecker(new DefaultDragChecker());
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        setBackgroundColor();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DragContainer);
        resetDuration = ta.getInteger(R.styleable.DragContainer_dc_reset_animator_duration, DEFAULT_RESET_DURATION);
        footerColor = ta.getColor(R.styleable.DragContainer_dc_footer_color, DEFAULT_FOOTER_COLOR);
        dragDamp = ta.getFloat(R.styleable.DragContainer_dc_drag_damp, DEFAULT_DRAG_DAMP);
        textOffsetTop=ta.getDimensionPixelOffset(R.styleable.DragContainer_dc_text_top_margin,0);
        String normalText = ta.getString(R.styleable.DragContainer_dc_text_normal);
        String eventText = ta.getString(R.styleable.DragContainer_dc_text_event);
        ta.recycle();
        footerColor = Color.TRANSPARENT;
        // 外部可通过 @string 配置提示，未配置时使用当前语言的默认文案。
        if (normalText == null) {
            normalText = context.getString(R.string.swipe_left_more);
        }
        if (eventText == null) {
            eventText = normalText;
        }
        setFooterDrawer(defaultFooterDrawer(normalText, eventText));

        setDragState(RELEASE);
    }

    private BaseFooterDrawer defaultFooterDrawer(String normalText, String eventText) {
        //ic_left
        return new BezierFooterDrawer.Builder(getContext(), footerColor)
                .setIconDrawable(getContext().getResources().getDrawable(R.drawable.ic_left2))
                .setNormalString(normalText)
                .setEventString(eventText)
                .build();
    }

    public void setFooterDrawer(BaseFooterDrawer footerDrawer) {
        this.footerDrawer = footerDrawer;
    }

    private void setBackgroundColor() {
        Drawable drawable = getBackground();
        if (drawable == null) {
            setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
        }
    }

    private void setDragState(@DragState int dragState) {
        if (footerDrawer != null) {
            footerDrawer.updateDragState(dragState);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        checkChildren();
        contentView = getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int width, height;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = contentView.getMeasuredWidth();
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = contentView.getMeasuredHeight();
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        containerWidth = w;
        containerHeight = h;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        contentView.layout(0, 0, containerWidth, containerHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (footerDrawer != null) {
            footerDrawer.drawFooter(canvas, contentView.getRight(), -textOffsetTop, containerWidth, containerHeight);
        }
    }

    private void setContentViewPosition(int left, int top, int right, int bottom) {
        if (right > containerWidth) {
            return;
        }

        shouldResetContentView = right < containerWidth;
        contentView.layout(left, top, right, bottom);
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (resetAnimator != null) {
                    resetAnimator.cancel();
                }
                setContentViewPosition(0, 0, containerWidth, containerHeight);
                dragging = false;
                gestureAxis = AXIS_UNDECIDED;
                dragDx = 0;
                downX = event.getX();
                downY = event.getY();
                lastMoveX = downX;
                viewParent = findViewPagerParent();
                super.dispatchTouchEvent(event);
                // 内容未超过一屏时直接允许 ViewPager2 接管，左滑切换到下一个 Tab。
                requestParentDisallowIntercept(hasHorizontalScroll());
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                if (gestureAxis == AXIS_UNDECIDED) {
                    if (Math.max(dx, dy) <= touchSlop) {
                        super.dispatchTouchEvent(event);
                        break;
                    }
                    gestureAxis = dx >= dy ? AXIS_HORIZONTAL : AXIS_VERTICAL;
                }
                if (gestureAxis == AXIS_VERTICAL) {
                    super.dispatchTouchEvent(event);
                    requestParentDisallowIntercept(false);
                    break;
                }

                float moveX = event.getX() - lastMoveX;
                int direction = moveX > 0 ? -1 : 1;
                boolean canStartDrag = moveX < 0 && hasHorizontalScroll() && dragChecker.canDrag(contentView);
                if (!dragging && canStartDrag) {
                    dragging = true;
                    // 只在末尾接管拖拽时取消子列表触摸，防止松手误点作品。
                    sendCancelEvent(event);
                }
                if (dragging) {
                    // 使用到达末尾后的增量，避免把此前正常横滑的距离算入拖拽阈值。
                    dragDx = Math.min(0, dragDx + moveX);
                    updateDragState(event);
                    float realDragDistance = dragDx * dragDamp;
                    setContentViewPosition((int) realDragDistance, 0, containerWidth + (int) realDragDistance, containerHeight);
                    requestParentDisallowIntercept(true);
                } else {
                    lastMoveX = event.getX();
                    super.dispatchTouchEvent(event);
                    boolean childCanScroll = contentView.canScrollHorizontally(direction);
                    boolean atDragEdge = direction == 1 && hasHorizontalScroll() && dragChecker.canDrag(contentView);
                    // 不足一屏时交给父容器；可横滑的列表在末尾保留拖动提示。
                    requestParentDisallowIntercept(childCanScroll || atDragEdge);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    super.dispatchTouchEvent(event);
                }
                boolean triggerEvent = dragging && event.getActionMasked() == MotionEvent.ACTION_UP;
                dragging = false;
                gestureAxis = AXIS_UNDECIDED;
                requestParentDisallowIntercept(false);
                // 父容器接管或系统取消手势时只回弹，不打开书架。
                resetContentView(triggerEvent);
                break;
            default:
                super.dispatchTouchEvent(event);
                break;
        }
        return true;
    }

    private boolean hasHorizontalScroll() {
        return contentView.canScrollHorizontally(-1) || contentView.canScrollHorizontally(1);
    }

    private ViewParent viewParent;

    private ViewParent findViewPagerParent() {
        ViewParent parent = getParent();
        while (parent != null) {
            if (parent instanceof ViewPager || parent instanceof ViewPager2) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void requestParentDisallowIntercept(boolean disallow) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(disallow);
        }
        if (viewParent != null) {
            viewParent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    private void sendCancelEvent(MotionEvent event) {
        MotionEvent cancel = MotionEvent.obtain(event);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(cancel);
        cancel.recycle();
    }

    private void updateDragState(MotionEvent event) {
        if (event.getX() < lastMoveX) {
            setDragState(DRAG_OUT);
        }
        if (event.getX() > lastMoveX && contentView.getRight() < containerWidth) {
            setDragState(DRAG_IN);
        }
        lastMoveX = event.getX();
    }

    private void resetContentView(boolean triggerEvent) {
        setDragState(RELEASE);

        if (!shouldResetContentView) {
            return;
        }

        resetAnimator = ValueAnimator.ofFloat(0, 1);
        resetAnimator.setDuration(resetDuration);

        final int left = contentView.getLeft();
        final int right = contentView.getRight();
        final int top = contentView.getTop();
        final int bottom = contentView.getBottom();
        final float totalDx = containerWidth - right;

        resetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (float) animation.getAnimatedValue();
                float currentDx;
                currentDx = totalDx * progress;
                setContentViewPosition(left + (int) currentDx, top, right + (int) currentDx, bottom);
            }
        });
        resetAnimator.start();

        if (triggerEvent && dragListener != null && footerDrawer != null && footerDrawer.shouldTriggerEvent(totalDx)) {
            dragListener.onDragEvent();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (resetAnimator != null) {
            resetAnimator.cancel();
        }
        dragging = false;
        viewParent = null;
        if (contentView != null) {
            setContentViewPosition(0, 0, containerWidth, containerHeight);
        }
        setDragState(RELEASE);
        super.onDetachedFromWindow();
    }

    private void checkChildren() {
        int childCount = getChildCount();
        if (childCount != 1) {
            throw new IllegalStateException("DragContainer must hold only one child, check how many child you put in DragContainer");
        }
    }
}
