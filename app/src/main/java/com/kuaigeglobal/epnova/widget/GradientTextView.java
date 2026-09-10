package com.kuaigeglobal.epnova.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.kuaigeglobal.epnova.R;

import java.util.ArrayList;
import java.util.List;

public class GradientTextView extends AppCompatTextView {


    private Integer[] mColorList;//存放颜色的数组
    private Integer[] mDisabledColorList;//存放颜色的数组
    private boolean gradientEnable = true;

    public GradientTextView(Context context) {
        this(context, null);
    }

    public GradientTextView(Context context,
                            AttributeSet attrs) {
        super(context, attrs);
        // 保留 EpNova 原有不透明文字效果，同时保留主题的颜色状态。
        setTextColor(getTextColors().withAlpha(255));
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GradientTextView);
        Integer startColor = null;
        if (a.hasValue(R.styleable.GradientTextView_startColor)) {
            startColor = a.getColor(R.styleable.GradientTextView_startColor, Color.TRANSPARENT);
        }
        Integer middleColor = null;
        if (a.hasValue(R.styleable.GradientTextView_middleColor)) {
            middleColor = a.getColor(R.styleable.GradientTextView_middleColor, Color.TRANSPARENT);
        }
        Integer endColor = null;
        if (a.hasValue(R.styleable.GradientTextView_endColor)) {
            endColor = a.getColor(R.styleable.GradientTextView_endColor, Color.TRANSPARENT);
        }
        gradientEnable = a.getBoolean(R.styleable.GradientTextView_gradientEnable, true);
        Integer disabledStartColor = null;
        if (a.hasValue(R.styleable.GradientTextView_disabledStartColor)) {
            disabledStartColor = a.getColor(R.styleable.GradientTextView_disabledStartColor, Color.TRANSPARENT);
        }
        Integer disabledMiddleColor = null;
        if (a.hasValue(R.styleable.GradientTextView_disabledMiddleColor)) {
            disabledMiddleColor = a.getColor(R.styleable.GradientTextView_disabledMiddleColor, Color.TRANSPARENT);
        }
        Integer disabledEndColor = null;
        if (a.hasValue(R.styleable.GradientTextView_disabledEndColor)) {
            disabledEndColor = a.getColor(R.styleable.GradientTextView_disabledEndColor, Color.TRANSPARENT);
        }
        if (startColor != null || middleColor != null || endColor != null) {
            List<Integer> colors = new ArrayList<>();
            if (startColor != null) {
                colors.add(startColor);
            }
            if (middleColor != null) {
                colors.add(middleColor);
            }
            if (endColor != null) {
                colors.add(endColor);
            }
            mColorList = new Integer[colors.size()];
            colors.toArray(mColorList);

        }
        if (disabledStartColor != null || disabledMiddleColor != null || disabledEndColor != null) {
            List<Integer> colors = new ArrayList<>();
            if (disabledStartColor != null) {
                colors.add(disabledStartColor);
            }
            if (disabledMiddleColor != null) {
                colors.add(disabledMiddleColor);
            }
            if (disabledEndColor != null) {
                colors.add(disabledEndColor);
            }
            mDisabledColorList = new Integer[colors.size()];
            colors.toArray(mDisabledColorList);
        }
        a.recycle();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            gradientText();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            gradientText();
        }
    }

    protected void gradientText() {
        if (!isEnabled()) {
            if (mDisabledColorList != null) {
                setGradientText(mDisabledColorList);
                return;
            }
        }
        setGradientText(mColorList);
    }

    public void setGradientEnable(boolean gradientEnable) {
        this.gradientEnable = gradientEnable;
        gradientText();
        invalidate();
    }

    protected void setGradientText(Integer[] colorList) {
        if (!gradientEnable || colorList == null || colorList.length == 0) {
            getPaint().setShader(null);
        } else {
            // 单色用两个相同色值，不覆盖 TextView 原本的 ColorStateList。
            int[] colors = new int[Math.max(2, colorList.length)];
            for (int i = 0; i < colors.length; i++) {
                Integer color = colorList[Math.min(i, colorList.length - 1)];
                colors[i] = color == null ? Color.TRANSPARENT : color;
            }
            getPaint().setShader(new LinearGradient(0, 0, Math.max(1, getMeasuredWidth()),
                    Math.max(1, getHeight()), colors, null, Shader.TileMode.CLAMP));
        }
        invalidate();
    }

    public void setColorList(Integer[] mColorList) {
        this.mColorList = mColorList == null ? null : mColorList.clone();
        gradientText();
        invalidate();
    }

    public void setColorList(int... colors) {
        this.mColorList = new Integer[colors.length];
        for (int i = 0; i < colors.length; i++) {
            mColorList[i] = colors[i];
        }
        gradientText();
        invalidate();
    }

    public static long pack(int color) {
        return (color & 0xffffffffL) << 32;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        gradientText();
    }
}
