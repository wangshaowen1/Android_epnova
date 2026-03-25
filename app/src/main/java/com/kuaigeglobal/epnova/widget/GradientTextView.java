package com.kuaigeglobal.epnova.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Build;
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
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
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
        if (mColorList != null) {
            setGradientText(mColorList);
        }
    }

    public void setGradientEnable(boolean gradientEnable) {
        this.gradientEnable = gradientEnable;
        invalidate();
    }

    protected void setGradientText(Integer[] colorList) {
        if (!gradientEnable) return;
        long[] gradientColorList = new long[colorList.length];
        for (int i = 0; i < colorList.length; i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                gradientColorList[i] = Color.pack(colorList[i]);
            } else {
                gradientColorList[i] = pack(colorList[i]);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getPaint().setShader(new LinearGradient(0, 0, getMeasuredWidth(), getHeight(),
                    gradientColorList, null,
                    Shader.TileMode.CLAMP));
        } else {
            getPaint().setShader(new LinearGradient(0, 0, getMeasuredWidth(), getHeight(),
                    colorList[0],
                    colorList[colorList.length - 1],
                    Shader.TileMode.CLAMP));
        }
    }

    public void setColorList(Integer[] mColorList) {
        this.mColorList = mColorList;
    }

    public void setColorList(int... colors) {
        this.mColorList = new Integer[colors.length];
        for (int i = 0; i < colors.length; i++) {
            mColorList[i] = colors[i];
        }
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
