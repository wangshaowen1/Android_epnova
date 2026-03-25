package com.kuaigeglobal.epnova.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Created by lu on 2017/5/20.
 */

public class AnimFrameLayout extends FrameLayout {
    public AnimFrameLayout(Context context) {
        super(context);
    }


    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    private boolean mVisible = true;
    private int direction = RIGHT;
    private static final int DEFAULT_TRANSLATE_DURATION_MILLIS = 300;
    private int mDurationMillis = DEFAULT_TRANSLATE_DURATION_MILLIS;

    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public AnimFrameLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public void setDurationMillis(int durationMillis) {
        this.mDurationMillis = durationMillis;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void toggle(boolean animate) {
        toggle(!mVisible,animate,false);
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void show(boolean animate) {
        toggle(true, animate, false);
    }

    public void hide(boolean animate) {
        toggle(false, animate, false);
    }

    private void toggle(final boolean visible, final boolean animate, boolean force) {
        if (mVisible != visible || force) {
            mVisible = visible;
            int height = getHeight();
            int width = getWidth()/2;
            if (((direction < LEFT && height == 0) || (direction >= LEFT && width == 0)) && !force) {
                ViewTreeObserver vto = getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            ViewTreeObserver currentVto = getViewTreeObserver();
                            if (currentVto.isAlive()) {
                                currentVto.removeOnPreDrawListener(this);
                            }
                            toggle(visible, animate, true);
                            return true;
                        }
                    });
                    return;
                }
            }

            int translationY = 0;
            int translationX = 0;
            if (direction < LEFT) {
                translationY = visible ? 0 : (direction == TOP ? -height - getMarginTop() : height + getMarginBottom());
            } else {
                translationX = visible ? 0 : (direction == LEFT ? -width - getMarginLeft() : width + getMarginRight());
            }
            if (animate) {
                ViewPropertyAnimator animator = animate().setInterpolator(mInterpolator).setDuration(mDurationMillis)
                        .setInterpolator(new AccelerateDecelerateInterpolator());
                if (direction < LEFT) {
                    animator.translationY(direction == TOP ? -translationY : translationY);
                } else {
                    animator.translationX(direction == LEFT ? -translationX : translationX);
                }
            } else {
//				ViewHelper.setTranslationY(this, translationY);
                if (direction < LEFT) {
                    setTranslationY(direction == TOP ? -translationY : translationY);
                } else {
                    setTranslationX(direction == LEFT ? -translationX : translationX);
                }
            }

            // On pre-Honeycomb a translated view is still clickable, so we need
            // to disable clicks manually
            if (!hasHoneycombApi()) {
                setClickable(visible);
            }
        }
    }

    private int getMarginBottom() {
        int marginBottom = 0;
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams instanceof MarginLayoutParams) {
            marginBottom = ((MarginLayoutParams) layoutParams).bottomMargin;
        }
        return marginBottom;
    }

    private int getMarginTop() {
        int marginTop = 0;
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams instanceof MarginLayoutParams) {
            marginTop = ((MarginLayoutParams) layoutParams).topMargin;
        }
        return marginTop;
    }

    private int getMarginLeft() {
        int marginLeft = 0;
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams instanceof MarginLayoutParams) {
            marginLeft = ((MarginLayoutParams) layoutParams).leftMargin;
        }
        return marginLeft;
    }

    private int getMarginRight() {
        int marginRight = 0;
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams instanceof MarginLayoutParams) {
            marginRight = ((MarginLayoutParams) layoutParams).rightMargin;
        }
        return marginRight;
    }

    @SuppressLint("NewApi")
    private boolean hasHoneycombApi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public int getDurationMillis() {
        return mDurationMillis;
    }
}
