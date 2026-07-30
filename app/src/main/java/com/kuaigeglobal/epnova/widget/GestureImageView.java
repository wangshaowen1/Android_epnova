package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.common.utils.ImageUtils;


public class GestureImageView extends com.alexvasilkov.gestures.views.GestureImageView {
    public static final int TRANSITION_SHAPE_RECT = 0;
    public static final int TRANSITION_SHAPE_ROUNDED = 1;
    public static final int TRANSITION_SHAPE_OVAL = 2;

    private final RectF transitionClipRect = new RectF();
    private int transitionShape = TRANSITION_SHAPE_RECT;
    private float transitionStartRadius;
    private float transitionProgress = 1f;
    private float transitionClipRotation;

    private final ViewOutlineProvider transitionOutlineProvider = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            if (transitionClipRect.isEmpty()) {
                outline.setRect(0, 0, view.getWidth(), view.getHeight());
                return;
            }
            int left = Math.round(transitionClipRect.left);
            int top = Math.round(transitionClipRect.top);
            int right = Math.round(transitionClipRect.right);
            int bottom = Math.round(transitionClipRect.bottom);
            if (right <= left || bottom <= top) {
                outline.setRect(0, 0, view.getWidth(), view.getHeight());
                return;
            }
            outline.setRoundRect(left, top, right, bottom, resolveTransitionRadius());
        }
    };

    public GestureImageView(Context context) {
        super(context);
        initTransitionOutline();
    }

    public GestureImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initTransitionOutline();
    }

    public GestureImageView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initTransitionOutline();
    }

    /** 初始化硬件轮廓裁剪，默认关闭，只有圆角转场期间才启用。 */
    private void initTransitionOutline() {
        setOutlineProvider(transitionOutlineProvider);
        setClipToOutline(false);
    }

    /** 设置来源图片形状及起始圆角，供放大和收回动画复用。 */
    public void setTransitionShape(int shape, float startRadius) {
        transitionShape = shape >= TRANSITION_SHAPE_RECT && shape <= TRANSITION_SHAPE_OVAL
                ? shape : TRANSITION_SHAPE_RECT;
        transitionStartRadius = Float.isFinite(startRadius) ? Math.max(0f, startRadius) : 0f;
        updateTransitionOutline();
    }

    /** 根据位置动画进度把来源圆角平滑过渡到全屏直角。 */
    public void setTransitionShapeProgress(float progress) {
        transitionProgress = Float.isFinite(progress)
                ? Math.max(0f, Math.min(1f, progress)) : 1f;
        updateTransitionOutline();
    }

    /** 清理复用 ViewHolder 上的形状转场状态。 */
    public void clearTransitionShape() {
        transitionShape = TRANSITION_SHAPE_RECT;
        transitionStartRadius = 0f;
        transitionProgress = 1f;
        transitionClipRotation = 0f;
        transitionClipRect.setEmpty();
        updateTransitionOutline();
    }

    @Override
    public void clipView(@Nullable RectF rect, float rotation) {
        super.clipView(rect, rotation);
        if (rect != null) {
            transitionClipRect.set(rect);
            transitionClipRotation = rotation;
        }
        updateTransitionOutline();
    }

    /** 更新轮廓裁剪开关并请求重算当前圆角。 */
    private void updateTransitionOutline() {
        boolean shouldClip = transitionShape != TRANSITION_SHAPE_RECT
                && transitionProgress < 1f
                && !transitionClipRect.isEmpty()
                && Math.abs(transitionClipRotation) < 0.1f;
        if (getClipToOutline() != shouldClip) {
            setClipToOutline(shouldClip);
        }
        invalidateOutline();
    }

    /** 按平滑曲线计算当前圆角；圆形随实时裁剪区域保持自然形变。 */
    private float resolveTransitionRadius() {
        float progress = transitionProgress * transitionProgress
                * (3f - 2f * transitionProgress);
        float shapeAmount = 1f - progress;
        if (transitionShape == TRANSITION_SHAPE_OVAL) {
            return Math.min(transitionClipRect.width(), transitionClipRect.height())
                    * 0.5f * shapeAmount;
        }
        return transitionStartRadius * shapeAmount;
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
