package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.kuaigeglobal.epnova.R;

public class RotateFrameLayout extends FrameLayout {

    private Camera camera;
    private Matrix matrix;
    private float degrees;
    private float zDistance= 0;

    public RotateFrameLayout(Context context) {
        super(context);
        init(null);
    }

    public RotateFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RotateFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init( AttributeSet attrs) {
        if(attrs!=null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RotateFrameLayout);
            if (a.hasValue(R.styleable.RotateFrameLayout_rotateDegrees)) {
                degrees= a.getInt(R.styleable.RotateFrameLayout_rotateDegrees, 0);
                setRotateDegree(degrees);
            }
            if(a.hasValue(R.styleable.RotateFrameLayout_zDistance)){
                zDistance= a.getDimension(R.styleable.RotateFrameLayout_zDistance, 0);
            }
            a.recycle();
        }
        camera = new Camera();
        matrix = new Matrix();
    }


    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {


        // 保存canvas状态
        canvas.save();

        // 通过Matrix来应用Camera的效果
        camera.save();
        camera.translate(0, 0, zDistance); // 设置Z轴的距离
        camera.rotateY(degrees);
        camera.getMatrix(matrix);
        camera.restore();

        // 将Matrix应用到Canvas上
        matrix.preTranslate((float) -getWidth() / 2, (float) -getHeight() / 2);
        matrix.postTranslate((float) getWidth() / 2, (float) getHeight() / 2);
        canvas.concat(matrix);
        super.dispatchDraw(canvas);

        // 绘制你的View内容
        // ...

        // 恢复canvas状态
        canvas.restore();
    }

    // 新增方法，用于设置旋转角度
    public void setRotateDegree(float degrees) {
        this.degrees = degrees;
        invalidate(); // 请求重绘View
    }
}
