package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kuaigeglobal.epnova.R;

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce: 拖拽、吸边控件
 * @Data: 2025/4/17  9:25
 * @Version: 1.0
 * *****************************************
 */
public class MoveFrameLayout extends FrameLayout {
    public MoveFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public MoveFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoveFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    //是否自动吸边
    private boolean isAttach;
    //是否可拖动
    private boolean isDrag;


    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.MoveFrameLayout);
        isAttach = mTypedArray.getBoolean(R.styleable.MoveFrameLayout_IsAttach, true);
        isDrag = mTypedArray.getBoolean(R.styleable.MoveFrameLayout_IsDrag, true);
        mTypedArray.recycle();
    }

    //父控件的宽
    private int mParentWidth = 0;
    //父控件的高
    private int mParentHeight = 0;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取父控件宽高
        ViewGroup mViewGroup = (ViewGroup) getParent();
        if (mViewGroup != null) {
            int[] location = new int[2];
            mViewGroup.getLocationOnScreen(location);
            //获取父布局的高度
            mParentHeight = mViewGroup.getMeasuredHeight();
            mParentWidth = mViewGroup.getMeasuredWidth();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //点击点在控件区域内，接受事件
        return inRangeOfView(this, ev);
    }

    //按下位置x
    private float mLastX;
    //按下位置Y
    private float mLastY;
    //是否是拖动
    private boolean isDrug = false;
    //当前行为 0:点击 1:滑动
    private int mBehavior = 0;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isDrag) {
            ViewGroup parent= (ViewGroup) getParent();
            if (parent!=null){
                // 动态更新父容器尺寸
                mParentWidth = parent.getWidth();
                mParentHeight = parent.getHeight();
            }
            //请求父控件将事件交由自己处理
            getParent().requestDisallowInterceptTouchEvent(true);
            float mX = ev.getX();
            float mY = ev.getY();
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //重置拖动状态
                    isDrug = false;
                    //记录按下的位置
                    mLastX = mX;
                    mLastY = mY;
                    mBehavior = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    //手指X轴滑动距离
                    float differenceValueX = mX - mLastX;
                    //手指Y轴滑动距离
                    float differenceValueY = mY - mLastY;
                    if (Math.abs(differenceValueX) > 10 || Math.abs(differenceValueY) > 10) {
                        mBehavior = 1;
                    }
                    //判断是否为拖动操作
                    if (!isDrug) {
                        isDrug = !(Math.sqrt(differenceValueX * differenceValueX + differenceValueY * differenceValueY) < 2);
                    }
                    //获取手指按下的距离与控件本身X轴的距离
                    float ownX = getX();
                    //获取手指按下的距离与控件本身Y轴的距离
                    float ownY = getY();
                    //理论中X轴拖动的距离
                    float endX = ownX + differenceValueX;
                    //理论中Y轴拖动的距离
                    float endY = ownY + differenceValueY;
                    //X轴可以拖动的最大距离
                    float maxX = mParentWidth - getWidth();

                    //Y轴可以拖动的最大距离
                    float maxY = mParentHeight - getHeight() - getPaddingBottom() - dp2px(15);
                    //X轴边界限制---这里对X轴拖动不做限制
                    //endX = endX < 0 ? 0 : endX > maxX ? maxX : endX;
                    //Y轴边界限制
                    endY = endY < 0 ? 0 : Math.min(endY, maxY);
                    //开始移动
                    setX(endX);
                    setY(endY);
                    break;
                case MotionEvent.ACTION_UP:
                    //判断是否为点击事件
                    if (mBehavior == 0) {
                        if (onClickListener!=null){
                            onClickListener.onClick(this);
                        }
                        //当前是点击事件
                        return true;
                    }
                    //根据自定义属性判断是否需要贴边
                    if (isAttach&&parent!=null) {
                        // 根据控件当前位置判断吸附方向
                        float currentX = getX();
                        int viewWidth = getWidth();
                        int parentPaddingLeft = parent.getPaddingLeft();
                        int parentPaddingRight = parent.getPaddingRight();

                        // 计算吸附位置
                        if (currentX + (float) viewWidth /2 < (float) mParentWidth /2) {
                            // 向左吸附（考虑父容器padding）
                            animate()
                                    .setInterpolator(new OvershootInterpolator())
                                    .setDuration(400)
                                    .x(parentPaddingLeft)
                                    .start();
                        } else {
                            // 向右吸附（考虑父容器padding）
                            animate()
                                    .setInterpolator(new OvershootInterpolator())
                                    .setDuration(400)
                                    .x(mParentWidth - viewWidth - parentPaddingRight)
                                    .start();
                        }
                    }
                    break;
            }
        }
        return true;
    }

    // 修改Y轴边界计算
    private float calculateMaxY() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return 0;

        // 正确计算底部边界（15dp间距 + 父容器padding）
        int parentPaddingBottom = parent.getPaddingBottom();
        int additionalSpace = dp2px(15);
        return mParentHeight - getHeight() - parentPaddingBottom - additionalSpace;
    }

    public int dp2px(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 点击点是否在控件区域
     */
    private boolean inRangeOfView(View view, MotionEvent ev) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        int rawX = (int) ev.getRawX();
        int rawY = (int) ev.getRawY();
        return rect.contains(rawX, rawY);
    }

    //点击事件回调
    private OnMfClickListener onClickListener;
    public void setOnMfClickListener(OnMfClickListener l) {
        onClickListener = l;
    }
    public interface OnMfClickListener {
        void onClick(View v);
    }
}
