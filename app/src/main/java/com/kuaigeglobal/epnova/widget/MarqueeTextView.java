package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import com.kuaigeglobal.epnova.R;

import java.util.Locale;

public class MarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {

    private final static int DEFAULT_SPACE = 48;

    private final static float DEFAULT_SPEED = 1;

    private TextView mTextView;

    private Bitmap mBitmap;

    private Paint mPaint;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public MarqueeTextViewScrollListener marqueeTextViewScrollListener;

    /**
     * 记录跑马灯的次数
     */
    private int repeatCount = 0;

    /**
     * 每一步的间隔
     */
    private static final int DELAY = 16;

    /**
     * 每次跑马的暂停间隔
     */
    private int mRepeatDelay = 500;

    private float mLeftX = 0;

    /**
     * 文字滚动时，头尾的最小间隔距离
     */
    private int mSpace = DEFAULT_SPACE;

    /**
     * 文字滚动速度
     */
    private float mSpeed = DEFAULT_SPEED;

    /**
     * 跑马灯滚动一次的时间与{@link #mSpeed}d只能有一个起效
     */
    private int mDuration = 0;

    private final Runnable mMarqueeRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
            int runDelay = DELAY;

            if (isRtl()) {
                if (mLeftX == getWidth() - mTextView.getMeasuredWidth() || mLeftX >= getWidth() + mSpace) {
                    runDelay = mRepeatDelay;
                }
                mLeftX += mSpeed;
            } else {
                if (mLeftX == 0 || mLeftX <= -mTextView.getMeasuredWidth() - mSpace) {
                    runDelay = mRepeatDelay;
                }
                mLeftX -= mSpeed;
                if (marqueeTextViewScrollListener != null)
                    marqueeTextViewScrollListener.onMarqueeTextViewScroll(true);
            }

            int marqueeRepeatLimit = getMarqueeRepeatLimit();

            if (marqueeRepeatLimit == -1 || repeatCount < marqueeRepeatLimit) {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, runDelay);
            } else {
                stopScroll();
            }
        }
    };

    public void addMarqueeTextViewScrollListener(MarqueeTextViewScrollListener marqueeTextViewScrollListener) {
        this.marqueeTextViewScrollListener = marqueeTextViewScrollListener;
    }

    public MarqueeTextView(@NonNull Context context) {
        super(context, null);
    }

    public MarqueeTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MarqueeTextView);
            mSpace = typedArray.getDimensionPixelSize(R.styleable.MarqueeTextView_mtv_space, DEFAULT_SPACE);
            mSpeed = typedArray.getFloat(R.styleable.MarqueeTextView_mtv_speed, DEFAULT_SPEED);
            mDuration = typedArray.getInteger(R.styleable.MarqueeTextView_mtv_duration, 0);
            mRepeatDelay = typedArray.getInteger(R.styleable.MarqueeTextView_mtv_repeat_delay, 500);
            typedArray.recycle();
        }

        mTextView = new TextView(getContext(), attrs);
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mTextView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        mTextView.setMaxWidth(Integer.MAX_VALUE);
        mTextView.setSingleLine(false);
        mTextView.setMaxLines(1);
        mTextView.setLayoutDirection(isRtl() ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR);
        setSingleLine(false);
        setMaxLines(1);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    public void setDuration(int duration) {
        mDuration = duration;
        mSpeed = DEFAULT_SPEED;
        if (duration <= DELAY) {
            return;
        }
        int measuredWidth = mTextView.getMeasuredWidth();
        if (measuredWidth <= 0) {
            return;
        }
        mSpeed = measuredWidth / (1f * duration / DELAY);
        mSpeed = Math.max(mSpeed, 1); // 确保最小速度为1
        Log.i("Marquee", "measuredWidth = " + measuredWidth + " duration =" + duration + " mSpeed =" + mSpeed);
    }

    public void setRepeatDelay(int repeatDelay) {
        this.mRepeatDelay = repeatDelay;
    }

    private void setFadingEdge() {
        RectF rectF = new RectF(getPaddingStart(), getPaddingTop()
                , getWidth() - getPaddingEnd(), getHeight() - getPaddingBottom());
        float rate = getTextSize() / rectF.width();
        mPaint.setShader(new LinearGradient(rectF.left, 0, rectF.right, 0, new int[]{Color.TRANSPARENT,
                getCurrentTextColor(),
                getCurrentTextColor(),
                Color.TRANSPARENT}, new float[]{0f, rate, 1 - rate, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        if (mTextView != null) {
            mTextView.setLayoutParams(params);
            requestLayout();
        }
    }

    @Override
    public void setWidth(int pixels) {
        super.setWidth(pixels);
        if (mTextView != null) {
            mTextView.setWidth(pixels);
            requestLayout();
        }
    }

    @Override
    public void setHeight(int pixels) {
        super.setHeight(pixels);
        if (mTextView != null) {
            mTextView.setHeight(pixels);
            requestLayout();
        }
    }

    public void setMarqueeText(CharSequence text) {
        if (mTextView != null) {
            mTextView.setText(text);
            setText(text);
            requestLayout();
            // 确保布局完成后检查是否需要滚动
            post(() -> {
                if (mTextView != null) {
                    updateBitmap();
                    setDuration(mDuration);
                    int textWidth = mTextView.getMeasuredWidth();
                    int viewWidth = getWidth();
                    if (textWidth > viewWidth) {
                        // 需要滚动
                        restartScroll();
                    } else {
                        // 不需要滚动，重置位置
                        mLeftX = isRtl() ? viewWidth - textWidth : 0;
                        invalidate();
                    }
                }
            });
        }
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        //执行父类构造函数时，如果AttributeSet中有textSize参数会先调用setTextSize，此时mTextView尚未初始化
        if (mTextView != null) {
            mTextView.setTextSize(size);
            requestLayout();
        }
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        //执行父类构造函数时，如果AttributeSet中有textSize参数会先调用setTextSize，此时mTextView尚未初始化
        if (mTextView != null) {
            mTextView.setTextColor(color);
            requestLayout();
        }
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        super.setTextColor(colors);
        //执行父类构造函数时，如果AttributeSet中有textSize参数会先调用setTextSize，此时mTextView尚未初始化
        if (mTextView != null) {
            mTextView.setTextColor(colors);
            requestLayout();
        }
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);

        //执行父类构造函数时，如果AttributeSet中有textSize参数会先调用setTextSize，此时mTextView尚未初始化
        if (mTextView != null) {
            mTextView.setTextAppearance(context, resId);
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mTextView.measure(MeasureSpec.UNSPECIFIED, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mTextView.layout(left, top, left + mTextView.getMeasuredWidth(), bottom);
        if (isRtl()) {
            mLeftX = getWidth() - mTextView.getMeasuredWidth();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            int delHeight = mTextView.getMeasuredHeight();
            int height = getHeight();
            float topOffset = 0;
            if (delHeight != height) {
                topOffset = ((float) (height - delHeight)) / 2;
            }

            if (mTextView.getMeasuredWidth() <= getWidth()) {
                mLeftX = isRtl() ? getWidth() - mTextView.getMeasuredWidth() : 0;
                stopScroll();
                canvas.drawBitmap(mBitmap, mLeftX, topOffset, mPaint);
            } else {

                if (isRtl()) {
                    if (mLeftX > getWidth() + mSpace) {
                        repeatCount++;
                        mLeftX -= mTextView.getMeasuredWidth() + mSpace;
                    }
                    canvas.drawBitmap(mBitmap, mLeftX, topOffset, mPaint);
                    if (mLeftX - mSpace > 0) {
                        canvas.drawBitmap(mBitmap, mLeftX - mSpace - mTextView.getMeasuredWidth(), topOffset, mPaint);
                    }
                } else {
                    if (mLeftX < -mTextView.getMeasuredWidth() - mSpace) {
                        repeatCount++;
                        mLeftX += mTextView.getMeasuredWidth() + mSpace;
                    }

                    canvas.drawBitmap(mBitmap, mLeftX, topOffset, mPaint);
                    if (mLeftX + (mTextView.getMeasuredWidth() - getWidth()) < 0) {
                        canvas.drawBitmap(mBitmap, mTextView.getMeasuredWidth() + mLeftX + mSpace, topOffset, mPaint);
                    }
                }

            }
        }
    }

    private boolean isRtl() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
    }

    private void updateBitmap() {
        try {
            if (mTextView != null && mTextView.getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                mBitmap = Bitmap.createBitmap(mTextView.getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mBitmap);
                mTextView.draw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
    }

    public void startScroll() {
        mHandler.removeCallbacks(mMarqueeRunnable);
        mHandler.post(mMarqueeRunnable);
    }

    public void startScrollDelay(long delayTime) {
        mHandler.postDelayed(mMarqueeRunnable, delayTime);
    }

    public void pauseScroll() {
        mHandler.removeCallbacks(mMarqueeRunnable);
        if (marqueeTextViewScrollListener != null)
            marqueeTextViewScrollListener.onMarqueeTextViewScroll(false);
    }

    public void stopScroll() {
        mLeftX = isRtl() ? getWidth() - mTextView.getMeasuredWidth() : 0;
        repeatCount = 0;
        mHandler.removeCallbacks(mMarqueeRunnable);
        if (marqueeTextViewScrollListener != null)
            marqueeTextViewScrollListener.onMarqueeTextViewScroll(false);
    }

    public void restartScroll() {
        mLeftX = isRtl() ? getWidth() - mTextView.getMeasuredWidth() : 0;
        repeatCount = 0;
        startScroll();
    }

    public void bindToLifeCycle(Lifecycle lifecycle) {
        lifecycle.addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_START) {
                startScroll();
            } else if (event == Lifecycle.Event.ON_RESUME) {
                restartScroll();
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                pauseScroll();
            } else if (event == Lifecycle.Event.ON_STOP) {
                stopScroll();
            }
        });
    }
}
