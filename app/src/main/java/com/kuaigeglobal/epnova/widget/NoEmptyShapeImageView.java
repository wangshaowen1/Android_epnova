package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;


public class NoEmptyShapeImageView extends CropShapeImageView {
    public NoEmptyShapeImageView(Context context) {
        super(context);
    }

    public NoEmptyShapeImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoEmptyShapeImageView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        if(bm!=null) {
            super.setImageBitmap(bm);
        }
    }
    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if(drawable!=null) {
            super.setImageDrawable(drawable);
        }
    }
}
