package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.kuaigeglobal.epnova.widget.roundedimageview.RoundedImageView;


public class AutoResizeImageView extends RoundedImageView {

    public AutoResizeImageView(Context context) {
        super(context);
    }

    public AutoResizeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoResizeImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY || getDrawable() == null) {
            return;
        }
        int imageWidth = getDrawable().getIntrinsicWidth();
        int imageHeight = getDrawable().getIntrinsicHeight();
        // 占位 Drawable 可能没有固有尺寸，此时保留 ImageView 的测量结果。
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? getMeasuredWidth() : MeasureSpec.getSize(widthMeasureSpec);
        int contentWidth = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        long desiredHeight = (long) contentWidth * imageHeight / imageWidth
                + getPaddingTop() + getPaddingBottom();
        int height = (int) Math.min(MEASURED_SIZE_MASK, Math.max(getSuggestedMinimumHeight(), desiredHeight));
        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0),
                resolveSizeAndState(height, heightMeasureSpec, 0));
    }
}
