package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.SizeUtils;
import com.kuaigeglobal.epnova.R;

/**
 * 圆角矩形边框进度条
 */
public class RoundCornerProgressBar extends ProgressBar {

    private Paint mBackgroundPaint;
    private Paint mProgressPaint;
    private float mRadius;
    private boolean mGradientEnable;
    private int mPathWidth;
    private float mStartOffsetPercent;
    private Shader mGradientShader;

    int[] mGradientColors = {Color.YELLOW, Color.RED};
    /**
     * 要和mGradientColors长度保持一致(渐变的起始点和结束点的位置)
     */
    float[] mGradientPositions = {0f, 1f};

    Path mRenderPaths = new Path();
    Path path = new Path();
    PathMeasure mPathMeasure;

    public RoundCornerProgressBar(Context context) {
        this(context, null);
    }

    public RoundCornerProgressBar(Context context, @NonNull AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundCornerProgressBar(Context context, @NonNull AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundCornerProgressBarAttr);

        mBackgroundPaint = new Paint();
        mProgressPaint = new Paint();

        int backgroundColor = typedArray.getColor(R.styleable.RoundCornerProgressBarAttr_countDown_backgroundColor, Color.GRAY);
        int progressColor = typedArray.getColor(R.styleable.RoundCornerProgressBarAttr_countDown_progressColor, Color.RED);
        mPathWidth = typedArray.getDimensionPixelSize(R.styleable.RoundCornerProgressBarAttr_countDown_pathWidth, SizeUtils.dp2px(5f));
        mRadius = typedArray.getDimension(R.styleable.RoundCornerProgressBarAttr_countDown_radius, SizeUtils.dp2px(15f));
        mGradientEnable = typedArray.getBoolean(R.styleable.RoundCornerProgressBarAttr_countDown_gradient_enable, true);
        mStartOffsetPercent = typedArray.getFloat(R.styleable.RoundCornerProgressBarAttr_countDown_startOffset, 0.358f);
        typedArray.recycle();

        mBackgroundPaint.setColor(backgroundColor);
        mBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStrokeWidth(mPathWidth);

        mProgressPaint.setColor(progressColor);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressPaint.setStrokeWidth(mPathWidth);
    }

    public void setGradientColors(int[] gradientColors) {
        this.mGradientColors = gradientColors;
    }

    public void setGradientPositions(float[] gradientPositions) {
        this.mGradientPositions = gradientPositions;
    }

    public void setGradientEnable(boolean mGradientEnable) {
        this.mGradientEnable = mGradientEnable;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        path.reset();
        float halfPathWidth = mPathWidth / 2f;
        RectF rectF = new RectF(halfPathWidth, halfPathWidth, getWidth() - halfPathWidth, getHeight() - halfPathWidth);
        path.addRoundRect(rectF, mRadius, mRadius, Path.Direction.CW);
        if (mPathMeasure == null) {
            mPathMeasure = new PathMeasure(path, false);
        } else {
            mPathMeasure.setPath(path, false);
        }
        if (mGradientEnable && mGradientShader == null) {
            if (mGradientColors != null && mGradientPositions != null && mGradientColors.length == mGradientPositions.length) {
                mGradientShader = getShader();
                // 将渐变应用到Paint对象的shader属性上
                mProgressPaint.setShader(mGradientShader);
            }
        }
    }

    @NonNull
    private Shader getShader() {
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        // 定义两个颜色和一个位置数组，用于创建线性渐变
        int[] colors = mGradientColors;
        float[] positions = mGradientPositions; // 渐变的起始点和结束点的位置
        // 创建线性渐变
        Shader shader = new LinearGradient(
                measuredWidth / 2f, 0,         // 起始点
                measuredWidth / 2f, measuredHeight,     // 结束点
                colors,       // 颜色数组
                positions,    // 位置数组
                Shader.TileMode.CLAMP); // 平铺模式
        return shader;
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawProgress(canvas);
    }

    private void drawBackground(Canvas canvas) {
        RectF rectF = new RectF(mPathWidth, mPathWidth, getWidth() - mPathWidth, getHeight() - mPathWidth);
        canvas.drawRoundRect(rectF, mRadius, mRadius, mBackgroundPaint);
    }

    private void drawProgress(Canvas canvas) {
        mRenderPaths.reset();
        float pathOffset = mPathMeasure.getLength() * (getProgress() / 100f);
        float startOffset = mPathMeasure.getLength() * mStartOffsetPercent;
        if (pathOffset + startOffset < mPathMeasure.getLength()) {
            if (mPathMeasure.getSegment(startOffset, startOffset + pathOffset, mRenderPaths, true)) {
                canvas.drawPath(mRenderPaths, mProgressPaint);
            }
        } else {
            if (mPathMeasure.getSegment(startOffset, mPathMeasure.getLength(), mRenderPaths, true)) {
                canvas.drawPath(mRenderPaths, mProgressPaint);
            }
            if (mPathMeasure.getSegment(0, pathOffset + startOffset - mPathMeasure.getLength(), mRenderPaths, true)) {
                canvas.drawPath(mRenderPaths, mProgressPaint);
            }
        }
    }

}
