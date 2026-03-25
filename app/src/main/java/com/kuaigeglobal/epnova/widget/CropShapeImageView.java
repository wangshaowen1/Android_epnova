package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.common.utils.ImageUtils;


public class CropShapeImageView extends com.google.android.material.imageview.ShapeableImageView {
    public CropShapeImageView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
    }

    public CropShapeImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.CENTER_CROP);
    }

    public CropShapeImageView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
        }catch (Exception e){
            System.gc();
            //设置为软件加速。
            if (getDrawable() instanceof BitmapDrawable) {
                Bitmap bitmap =((BitmapDrawable) getDrawable()).getBitmap();
                if(!bitmap.isRecycled()) {
                    int height = bitmap.getHeight() / 2;
                    int width = bitmap.getWidth() / 2;
                    Bitmap scaled = ImageUtils.scale(bitmap, width, height);
                    setImageDrawable(new BitmapDrawable(scaled));
                }
            }
            super.onDraw(canvas);
        }
    }
}
