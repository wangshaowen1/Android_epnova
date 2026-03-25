package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.R;
import com.nhsd.basic.ext.CoreUtils;

public class StrokeTextView extends androidx.appcompat.widget.AppCompatTextView {
    private static final int[] STROKE_ATTRS = new int[]{R.attr.strokeTextColor, R.attr.strokeTextWidth};
    private @ColorInt int strokeColor;
    private float strokeWidth;

    public StrokeTextView(Context context) {
        this(context, null);
    }

    public StrokeTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public StrokeTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, STROKE_ATTRS, defStyleAttr, 0);
        strokeColor = typedArray.getColor(0, 0);
        strokeWidth = typedArray.getDimension(1, CoreUtils.getDp(2));
        typedArray.recycle();
    }

    public void setStrokeColor(@ColorInt int color) {
        strokeColor = color;
        invalidate();
    }

    public void setStrokeWidth(float width) {
        strokeWidth = width;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        TextPaint wkPaint = getLayout().getPaint();
        int preColor = wkPaint.getColor();
        Paint.Style prePaintStyle = wkPaint.getStyle();
        // apply stroke paint
        wkPaint.setColor(strokeColor);
        wkPaint.setStrokeWidth(strokeWidth);
        wkPaint.setStyle(Paint.Style.STROKE);
        // draw text outline
        getLayout().draw(canvas);

        // restore paint
        wkPaint.setColor(preColor);
        wkPaint.setStrokeWidth(0);
        wkPaint.setStyle(prePaintStyle);
        super.onDraw(canvas);
    }

}