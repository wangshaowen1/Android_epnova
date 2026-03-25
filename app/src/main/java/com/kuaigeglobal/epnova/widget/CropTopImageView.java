package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;


public class CropTopImageView extends AppCompatImageView {


    private int imgWidth;
    private int imgHeight;
    private Context context;
    private Matrix matrix;
    public static final int TYPE_START_CROP = 1;
    public static final int TYPE_END_CROP = 0;
    private int cropType = TYPE_START_CROP;
    private Drawable skinDrawable;

    public CropTopImageView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public CropTopImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public CropTopImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        this.imgWidth = r - l;
        this.imgHeight = b - t;
        matrix = null;
        calculationMatrix(skinDrawable);
        return super.setFrame(l, t, r, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        final int pleft = getPaddingLeft();
//        final int pright = getPaddingRight();
//        final int ptop = getPaddingTop();
//        final int pbottom = getPaddingBottom();

//        int widthSize=MeasureSpec.getSize(widthMeasureSpec);
//        int heightSize=MeasureSpec.getSize(heightMeasureSpec);
//
//        setMeasuredDimension(widthSize,heightSize);
    }
    
    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        calculationMatrix(drawable);
        super.setImageDrawable(drawable);
    }

    /**
     * 计算matrix
     *
     * @param drawable
     */
    public void calculationMatrix(Drawable drawable) {
        if (drawable == null)
            return;
        //设置并应用矩阵
        if (matrix == null) {
            skinDrawable = drawable;
            int dwidth = drawable.getIntrinsicWidth();
            int dheight = drawable.getIntrinsicHeight();

            if (dwidth <= 0 || dheight <= 0 || imgWidth <= 0) {
                super.setImageDrawable(drawable);
                return;
            }
            matrix = new Matrix();           //创建一个单位矩阵
            float scale = (float) imgWidth / (float) dwidth;
            float dy = imgHeight - dheight * scale;
            matrix.setScale(scale, scale);
            if (cropType != TYPE_START_CROP) {
                float dx = 0;
                if (dy > 0) {
                    scale = (float) imgHeight / (float) dheight;
                    dy = 0;
                    dx = imgWidth - dwidth * scale;
                }
                matrix.setScale(scale, scale);
                matrix.postTranslate(Math.round(dx), Math.round(dy));
            }

        }
        setImageMatrix(matrix);       //设置并应用矩阵
    }

    public void setCropType(int type) {
        this.cropType = type;
        matrix = null;
    }
}
