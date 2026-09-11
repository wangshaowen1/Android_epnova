package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.Nullable;

/** 保留 CENTER_CROP 和 ShapeableImageView 的形状能力，只恢复超出 Canvas 限制的位图。 */
public class CropShapeImageView extends com.google.android.material.imageview.ShapeableImageView {
    private Drawable pendingSource;
    private int fallbackWidth;
    private int fallbackHeight;
    private final Runnable resizeOversizedImage = () -> {
        Drawable source = pendingSource;
        pendingSource = null;
        if (getDrawable() != source || !(source instanceof BitmapDrawable)) return;
        Bitmap bitmap = ((BitmapDrawable) source).getBitmap();
        if (bitmap.isRecycled()) return;
        // 离开绘制过程后才生成降采样图；不回收图片加载器拥有的原图。
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, fallbackWidth, fallbackHeight, true);
        if (getDrawable() == source) {
            setImageDrawable(new BitmapDrawable(getResources(), scaled));
        }
    };

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

    @Override protected void onDraw(Canvas canvas) {
        Drawable source = getDrawable();
        if (source instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) source).getBitmap();
            if (bitmap.isRecycled()) return;
            if (bitmap.getWidth() > canvas.getMaximumBitmapWidth()
                    || bitmap.getHeight() > canvas.getMaximumBitmapHeight()) {
                scheduleResize(source, bitmap, canvas);
                return;
            }
        }
        int save = canvas.save();
        try {
            super.onDraw(canvas);
        } catch (RuntimeException error) {
            // 其它绘制错误不能靠缩图掩盖，也不在内存紧张时强制 GC 后重试分配。
            if (source instanceof BitmapDrawable && error.getMessage() != null
                    && error.getMessage().contains("trying to draw too large")) {
                scheduleResize(source, ((BitmapDrawable) source).getBitmap(), canvas);
            } else {
                throw error;
            }
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void scheduleResize(Drawable source, Bitmap bitmap, Canvas canvas) {
        if (pendingSource == source) return;
        removeCallbacks(resizeOversizedImage);
        float scale = Math.min(0.5f, Math.min(
                (float) canvas.getMaximumBitmapWidth() / bitmap.getWidth(),
                (float) canvas.getMaximumBitmapHeight() / bitmap.getHeight()));
        fallbackWidth = Math.max(1, (int) (bitmap.getWidth() * scale));
        fallbackHeight = Math.max(1, (int) (bitmap.getHeight() * scale));
        if (fallbackWidth == bitmap.getWidth() && fallbackHeight == bitmap.getHeight()) return;
        pendingSource = source;
        post(resizeOversizedImage);
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(resizeOversizedImage);
        pendingSource = null;
        super.onDetachedFromWindow();
    }
}
