package com.kuaigeglobal.epnova.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.TextView.BufferType;

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

        // 🌟 修复关键点 1：强制将基础文字颜色设为完全不透明
        // 防止带有透明度的默认 textColor 压暗 Shader 的颜色
        setTextColor(getCurrentTextColor() | 0xFF000000);

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

        a.recycle(); // 记得回收 TypedArray

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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            gradientText();
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        postGradientText();
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
        invalidate(); // 更改状态后重绘
    }

    protected void setGradientText(Integer[] colorList) {
        if (!gradientEnable || colorList == null || colorList.length == 0) return;
        int width = getMeasuredWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        // 🌟 修复关键点 2：将 Integer[] 转换为 int[]
        // 直接使用支持 int[] 的 LinearGradient 构造函数，兼容所有 Android 版本
        // 移除了原来冗余的 Build.VERSION 判断和 long[] 转换
        int[] colors = new int[colorList.length];
        for (int i = 0; i < colorList.length; i++) {
            colors[i] = colorList[i];
        }

        // 使用多颜色的构造函数，即使是 Android 10 以下也不会丢失 middleColor
        getPaint().setShader(new LinearGradient(
                0, 0, width, height,
                colors,
                null, // positions 为 null 表示颜色均匀分布
                Shader.TileMode.CLAMP));
        invalidate();
    }

    private void postGradientText() {
        post(new Runnable() {
            @Override
            public void run() {
                gradientText();
            }
        });
    }

    public void setColorList(Integer[] mColorList) {
        this.mColorList = mColorList;
        // 如果在代码中动态改变颜色，记得重新触发
        if (getWidth() > 0 && getHeight() > 0) {
            gradientText();
            invalidate();
        }
    }

    public void setColorList(int... colors) {
        this.mColorList = new Integer[colors.length];
        for (int i = 0; i < colors.length; i++) {
            mColorList[i] = colors[i];
        }
        if (getWidth() > 0 && getHeight() > 0) {
            gradientText();
            invalidate();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        gradientText();
        invalidate(); // 状态改变时确保重绘
    }
}
