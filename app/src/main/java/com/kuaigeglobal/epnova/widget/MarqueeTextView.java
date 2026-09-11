package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import com.kuaigeglobal.epnova.R;

/** 单行循环文字。直接绘制 TextView，避免为整段长文本创建超宽 Bitmap。 */
public class MarqueeTextView extends AppCompatTextView {
    private TextView renderer;
    private int space = 48; // 兼容原属性默认值：px。
    private float speed = 1f; // 兼容原属性单位：每 16ms 移动的 px。
    private int duration;
    private int repeatDelay = 500;
    private float distance;
    private int repeatCount;
    private boolean requested;
    private boolean scheduled;
    private boolean observerRegistered;
    private boolean lifecycleActive = true;
    private boolean resetOnLayout;
    private boolean lastRtl;
    private long lastFrame;
    private long waitUntil;
    private Lifecycle lifecycle;
    public MarqueeTextViewScrollListener marqueeTextViewScrollListener;

    private final LifecycleEventObserver observer = (owner, event) -> {
        if (event == Lifecycle.Event.ON_DESTROY) {
            stopScroll();
            unobserve();
            lifecycle = null;
            lifecycleActive = false;
        } else if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
            lifecycleActive = true;
            if (event == Lifecycle.Event.ON_RESUME) restartScroll(); else startScroll();
        } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
            lifecycleActive = false;
            updateScheduling();
            notifyScroll(false);
        }
    };
    private final Runnable frame = this::advance;

    public MarqueeTextView(Context context) { this(context, null); }
    public MarqueeTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }
    public MarqueeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MarqueeTextView, defStyleAttr, 0);
        space = Math.max(0, a.getDimensionPixelSize(R.styleable.MarqueeTextView_mtv_space, 48));
        speed = a.getFloat(R.styleable.MarqueeTextView_mtv_speed, 1f);
        if (!Float.isFinite(speed) || speed <= 0f) speed = 1f;
        duration = a.getInt(R.styleable.MarqueeTextView_mtv_duration, 0);
        repeatDelay = Math.max(0, a.getInt(R.styleable.MarqueeTextView_mtv_repeat_delay, 500));
        a.recycle();
        renderer = new AppCompatTextView(context, attrs, defStyleAttr) {
            @Override public void invalidateDrawable(android.graphics.drawable.Drawable drawable) {
                super.invalidateDrawable(drawable);
                MarqueeTextView.this.invalidate();
            }
            @Override public void scheduleDrawable(android.graphics.drawable.Drawable drawable, Runnable what, long when) {
                MarqueeTextView.this.scheduleDrawable(drawable, what, when);
            }
            @Override public void unscheduleDrawable(android.graphics.drawable.Drawable drawable, Runnable what) {
                MarqueeTextView.this.unscheduleDrawable(drawable, what);
            }
        };
        // renderer 不加入父容器，需自行提供参数供 TextView 重复测量时重排文字/提示。
        renderer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        renderer.setMaxWidth(Integer.MAX_VALUE);
        renderer.setSingleLine(false);
        renderer.setMaxLines(1);
        renderer.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        setSingleLine(false);
        setMaxLines(1);
    }

    public void addMarqueeTextViewScrollListener(MarqueeTextViewScrollListener listener) {
        marqueeTextViewScrollListener = listener;
    }
    public void setMarqueeText(CharSequence text) { setText(text); }
    @Override protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        if (renderer != null) {
            resetOnLayout = true;
            requested = true;
            requestLayout();
        }
    }
    @Override public void setTextColor(int color) {
        super.setTextColor(color);
        if (renderer != null) { renderer.setTextColor(color); invalidate(); }
    }
    @Override public void setTextColor(android.content.res.ColorStateList colors) {
        super.setTextColor(colors);
        if (renderer != null) { renderer.setTextColor(colors); invalidate(); }
    }
    @Override public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (renderer != null) { renderer.setTextAppearance(context, resId); requestLayout(); }
    }
    @Override public void setTextAppearance(int resId) {
        super.setTextAppearance(resId);
        if (renderer != null) { renderer.setTextAppearance(resId); requestLayout(); }
    }
    public void setDuration(int duration) {
        this.duration = duration;
        // 保留旧 setter 的语义：无效 duration 回到默认速度。
        speed = 1f;
        lastFrame = 0;
        updateScheduling();
    }
    public void setRepeatDelay(int delay) { repeatDelay = Math.max(0, delay); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        // 保留完整 TextView 的文字、Span、字体、padding、compound drawable 能力。
        renderer.setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize());
        renderer.setTypeface(getTypeface());
        renderer.getPaint().set(getPaint());
        renderer.setTextColor(getTextColors());
        renderer.setHintTextColor(getHintTextColors());
        renderer.setHint(getHint());
        renderer.setIncludeFontPadding(getIncludeFontPadding());
        renderer.setTransformationMethod(getTransformationMethod());
        renderer.setTextDirection(getTextDirection());
        renderer.setLayoutDirection(getLayoutDirection());
        renderer.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        android.graphics.drawable.Drawable[] icons = getCompoundDrawablesRelative();
        renderer.setCompoundDrawablesRelative(icons[0], icons[1], icons[2], icons[3]);
        renderer.setCompoundDrawablePadding(getCompoundDrawablePadding());
        renderer.setText(getText());
        renderer.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int oldWidth = renderer.getWidth();
        renderer.layout(0, 0, renderer.getMeasuredWidth(), getHeight());
        boolean rtl = isRtl();
        if (resetOnLayout || oldWidth != renderer.getWidth() || lastRtl != rtl) {
            resetPosition();
            resetOnLayout = false;
        }
        lastRtl = rtl;
        if (!overflows()) {
            resetPosition();
            notifyScroll(false);
        }
        updateScheduling();
    }
    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (renderer != null) {
            renderer.setEnabled(isEnabled());
            renderer.setSelected(isSelected());
            renderer.setPressed(isPressed());
            renderer.setTextColor(getTextColors());
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        if (renderer == null) return;
        float x = isRtl() ? getWidth() - renderer.getWidth() + distance : -distance;
        int save = canvas.save();
        try {
            canvas.clipRect(0, 0, getWidth(), getHeight());
            drawTextCopy(canvas, x);
            if (overflows()) {
                float cycle = renderer.getWidth() + (float) space;
                drawTextCopy(canvas, x + (isRtl() ? -cycle : cycle));
            }
        } finally { canvas.restoreToCount(save); }
    }
    private void drawTextCopy(Canvas canvas, float x) {
        if (x >= getWidth() || x + renderer.getWidth() <= 0) return;
        int save = canvas.save();
        try { canvas.translate(x, 0); renderer.draw(canvas); }
        finally { canvas.restoreToCount(save); }
    }
    private boolean isRtl() { return getLayoutDirection() == LAYOUT_DIRECTION_RTL; }
    private boolean overflows() { return renderer != null && renderer.getWidth() > getWidth() && getWidth() > 0; }
    private boolean canRun() {
        return requested && lifecycleActive && isAttachedToWindow() && isShown()
                && getWindowVisibility() == VISIBLE && overflows() && getMarqueeRepeatLimit() != 0;
    }
    private void advance() {
        scheduled = false;
        if (!canRun()) { lastFrame = 0; return; }
        long now = SystemClock.uptimeMillis();
        if (now < waitUntil) { schedule(waitUntil - now); return; }
        if (lastFrame == 0) lastFrame = now;
        float pixelsPerMs = duration > 16 ? Math.max(1f / 16f, renderer.getWidth() / (float) duration) : speed / 16f;
        distance += (now - lastFrame) * pixelsPerMs;
        lastFrame = now;
        float cycle = renderer.getWidth() + (float) space;
        if (distance >= cycle) {
            distance %= cycle;
            repeatCount++;
            if (getMarqueeRepeatLimit() >= 0 && repeatCount >= getMarqueeRepeatLimit()) {
                stopScroll();
                return;
            }
            waitUntil = now + repeatDelay;
            lastFrame = 0;
        }
        invalidate();
        notifyScroll(true);
        // 监听器可重入 stop/pause；以最新状态决定是否安排下一帧。
        if (canRun() && !scheduled) schedule(Math.max(0, waitUntil - now));
    }
    private void schedule(long delay) {
        removeCallbacks(frame);
        scheduled = true;
        if (delay > 0) postDelayed(frame, delay); else postOnAnimation(frame);
    }
    private void updateScheduling() {
        if (frame == null) return; // View 构造期间可能分发可见性回调。
        removeCallbacks(frame);
        scheduled = false;
        lastFrame = 0;
        if (canRun()) schedule(Math.max(0, waitUntil - SystemClock.uptimeMillis()));
    }
    private void resetPosition() {
        distance = 0;
        repeatCount = 0;
        lastFrame = 0;
        waitUntil = SystemClock.uptimeMillis() + repeatDelay;
        invalidate();
    }
    public void startScroll() { requested = true; updateScheduling(); }
    public void startScrollDelay(long delayTime) {
        requested = true;
        waitUntil = SystemClock.uptimeMillis() + Math.max(0, delayTime);
        updateScheduling();
    }
    public void pauseScroll() { requested = false; updateScheduling(); notifyScroll(false); }
    public void stopScroll() { requested = false; resetPosition(); updateScheduling(); notifyScroll(false); }
    public void restartScroll() { resetPosition(); startScroll(); }
    private void notifyScroll(boolean scrolling) {
        if (marqueeTextViewScrollListener != null) marqueeTextViewScrollListener.onMarqueeTextViewScroll(scrolling);
    }

    public void bindToLifeCycle(@Nullable Lifecycle lifecycle) {
        if (this.lifecycle == lifecycle) return;
        unobserve();
        this.lifecycle = lifecycle;
        lifecycleActive = lifecycle == null || lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        observe();
        updateScheduling();
    }
    private void observe() {
        if (lifecycle != null && !observerRegistered && isAttachedToWindow()) {
            observerRegistered = true;
            lifecycle.addObserver(observer);
        }
    }
    private void unobserve() {
        if (lifecycle != null && observerRegistered) lifecycle.removeObserver(observer);
        observerRegistered = false;
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); observe(); updateScheduling(); }
    @Override protected void onDetachedFromWindow() {
        unobserve();
        removeCallbacks(frame);
        scheduled = false;
        lastFrame = 0;
        super.onDetachedFromWindow();
    }
    @Override protected void onVisibilityChanged(View view, int visibility) {
        super.onVisibilityChanged(view, visibility);
        updateScheduling();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateScheduling();
    }
}
