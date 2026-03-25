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
        if(MeasureSpec.getMode(heightMeasureSpec)!=MeasureSpec.EXACTLY && getDrawable()!=null) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int imageWidth = getDrawable().getIntrinsicWidth();
            int imageHeight = getDrawable().getIntrinsicHeight();
            int height = width * imageHeight / imageWidth;
//            Logger.i("AutoResizeImageView:","height:"+height);
            setMeasuredDimension(width, height);
        }else {
//            Logger.i("AutoResizeImageView:","drawable:"+getDrawable());
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
