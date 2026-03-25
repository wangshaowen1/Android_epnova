package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.common.utils.ImageUtils;


public class GestureImageView extends com.alexvasilkov.gestures.views.GestureImageView {
    public GestureImageView(Context context) {
        super(context);
    }

    public GestureImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GestureImageView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
        } catch (Exception e) {
            System.gc();
            if (getDrawable() instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
                int height = bitmap.getHeight() / 2;
                int width = bitmap.getWidth() / 2;
                Bitmap scaled = ImageUtils.scale(bitmap, width, height);
                setImageDrawable(new BitmapDrawable(getResources(), scaled));
            }
            super.onDraw(canvas);
        }
    }
}
