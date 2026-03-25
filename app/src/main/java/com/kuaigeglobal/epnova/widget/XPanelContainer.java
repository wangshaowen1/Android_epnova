package com.kuaigeglobal.epnova.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kuaigeglobal.epnova.common.utils.AnimateUtils;
import com.nhsd.basic.utils.Logger;


/**
 * 该类不能设置forground..
 * 负责显示扩展面板的Container. 可以显示一个扩展面板, 当软键盘弹出时.
 * 则自动缩回扩展面板
 */
public class XPanelContainer extends FrameLayout implements Runnable {
    private static final String TAG = XPanelContainer.class.getSimpleName();

    /**
     * 无面板弹出
     */
    public static final int NONE = 0;

    /**
     * 软键盘
     */
    public static final int SOFT_INPUT_PANEL = 1;


    // showExternalPanel参数
    public final static int EXT_PANEL_EMOTCATION = 2;
    public final static int EXT_PANEL_PLUS = 3;
    public final static int EXT_PANEL_VOICE = 4;
    public static final int EXT_PANEL_GIFT = 5;

//    /**
//     * 表情
//     */
//    public static final int EMOTICON_PANEL = 2;
//    /**
//     * 扩展输入
//     */
//    public static final int EXTERNAL_INPUT_PANEL = 3;

    /**
     * 扩展面板高度
     */
    private int mExternalPanelheight;

    /**
     * 当前的状态
     */
    private int mStatus = NONE;

    /**
     * 给PendingStatus, 表明状态没变化
     */
    private static final int NO_CHANGED = -1;

    /**
     * 即将变化的状态
     */
    private int mPendingStatus = NO_CHANGED;

    private int mNormalSize = -1;
//    private boolean mNormalSizeSet = false;

//    private int currFacePage = 0;

    private TextView messageInputer;

    /**
     * 监听器
     */
    private PanelCallback mListener;

    private View mCurPanel;

    private SparseArray<View> mPanels = new SparseArray<View>(4);

    private int orientation = -1;
    private int screenWidthDp = -1;
    private int screenHeightDp = -1;
    private View lastPanel;
    private boolean requestCaculate;
    private int diff;

    /**
     * 当面板改变时,触发此监听器
     */
    public interface PanelCallback {
        /**
         * 面板被改变
         *
         * @param oldPanel
         * @param newPanel
         */
        public void onPanelChanged(int oldPanel, int newPanel);

        /**
         * 回调, 当showExternalPanel传值不为NONE or SOFT_INPUT_PANEL时, 会通知这货来创建view
         *
         * @param panelId showExternalPanel中panelid
         * @return 对应的view
         */
        public View onCreatePanel(int panelId);

        public void onLayoutChange(int oldPanel, int newPanel);
    }


//    public XPanelContainer(Context context, AttributeSet attrs, int defStyle)
//    {
//        super(context, attrs, defStyle);
//        mExternalPanelheight = (int) (196 * context.getResources().getDisplayMetrics().density);
//    }

    public XPanelContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mExternalPanelheight = (int) (237 * context.getResources().getDisplayMetrics().density);
    }

//    public XPanelContainer(Context context)
//    {
//    	this(context, null);
//    }


    public void bindInputer(TextView inputer) {
        this.messageInputer = inputer;
    }

    /**
     * 显示扩展面板
     *
     * @param panelId {@link XPanelContainer#SOFT_INPUT_PANEL}, 或者从外面传入的面板id, 如果值为其他id, 则会触发onShowPanel回调
     */
    public void showExternalPanel(int panelId) {
//        FragmentManager manager = aioFrag.getFragmentManager();
        // 软键盘
        if (panelId == SOFT_INPUT_PANEL) {
            messageInputer.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(messageInputer, 0);
            // 这里不改变状态, 弹软键盘成功的话, 会触发onLayout.在onLayout里再改
        }
        // 显示其他扩展面板
        else if (panelId > SOFT_INPUT_PANEL) {
            View panel = mPanels.get(panelId);

            if (panel == null) {
                panel = mListener.onCreatePanel(panelId);

                addView(panel);
                mPanels.put(panelId, panel);
            }

            if (panel != mCurPanel) {
                if (mCurPanel != null) {
                    mCurPanel.setVisibility(View.GONE);
                }
                panel.setVisibility(View.VISIBLE);

                lastPanel = mCurPanel;
                mCurPanel = panel;
                if (lastPanel != null) {
                    requestCaculate = true;
                }
            } else {
                requestCaculate = false;
            }

            if (mStatus == NONE) {
                // 还在动画中
                if (mAnimationPosition > 0) {
                    return;
                }
                // // child全部上移
                // offsetChildTopAndBottom(-mExternalPanelheight);
                // // 设置并通知状态改变
                // setStatus(externalId);
                this.mPendingStatus = panelId;
                mAnimationPosition = mExternalPanelheight;
                mAnimationStart = AnimationUtils.currentAnimationTimeMillis();
                // 申请重新布局
                requestLayout();
            } else if (mStatus == SOFT_INPUT_PANEL) {
                // 设置pending status. 在下次layout时会用到
                this.mPendingStatus = panelId;
//                URLDrawable.pause();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
            } else if (mStatus != panelId) {
                // 还在动画中
                if (mAnimationPosition != 0) {
                    return;
                }
                // // child全部上移
                // offsetChildTopAndBottom(-mExternalPanelheight);
                // // 设置并通知状态改变
                // setStatus(externalId);
                this.mPendingStatus = panelId;
                Logger.i("PanelLayout", " diff:" + diff);
                mAnimationPosition = diff;
                mAnimationStart = AnimationUtils.currentAnimationTimeMillis();
                // 申请重新布局
                requestLayout();
//                setStatus(panelId);
//                setStatus(panelId);
            }
        } else {
            throw new IllegalArgumentException("wrong argument..must be SOFT_INPUT,EXTERNAL_EMOTICON,EXTERNAL_INPUT");
        }
    }

    /**
     * 隐藏全部面板
     *
     * @return
     */
    public boolean hideAllPanel() {
        boolean ret = mStatus > NONE;

        if (mStatus == SOFT_INPUT_PANEL) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        } else if (mStatus > SOFT_INPUT_PANEL) {
            if (mAnimationPosition > 0) {
                return true;
            }
            this.mPendingStatus = NONE;
            mAnimationPosition = mExternalPanelheight;
            mAnimationStart = AnimationUtils.currentAnimationTimeMillis();
            // 申请重新布局
            requestLayout();
        }

        return ret;
    }

    /**
     * 当前的Animation position 值为0~mExternalPanelheight
     */
    private int mAnimationPosition = 0;
    private long mAnimationStart = -1;
    private static final int ANIMATION_DURATION = 300;
    private static final int ANIMATION_DURATION_DIFF = 300;
    private static final long FRAME_DELAY = 1;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View main = getChildAt(0);
        measureChild(main, widthMeasureSpec, heightMeasureSpec);
        if (mCurPanel != null) {
            measureChild(mCurPanel, widthMeasureSpec, heightMeasureSpec);
//            Logger.i("Soft","current pannel:"+mCurPanel+", pannel measure height:"+mCurPanel.getMeasuredHeight());
            if (requestCaculate && lastPanel != null) {
//            Logger.i("PannelLayout","last panel:"+lastPanel+",current :"+mCurPanel);
//            Logger.i("PannelLayout","last measure height:"+lastPanel.getMeasuredHeight()+",current measure height:"+mCurPanel.getMeasuredHeight());
                diff = mCurPanel.getMeasuredHeight() - lastPanel.getMeasuredHeight();
            }
        }
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        int height = bottom - top;
        int finalStatus = mStatus;

        // 初次布局, 记录下布局大小
        if (mNormalSize < 0 && changed && height > 0) {
            Configuration config = getResources().getConfiguration();
            orientation = config.orientation;
            mNormalSize = height;
        } else if (height > mNormalSize) {
            mNormalSize = height;
        }
//        int softInputHeight = softInputCatcher != null ? softInputCatcher.getSoftInputHeight() : 0;
        Rect windowVisibleRect = new Rect();
        getWindowVisibleDisplayFrame(windowVisibleRect);
        int[] point = new int[2];
        getLocationOnScreen(point);
//        getLocalVisibleRect()
        int topInScreen = point[1];

        int visibleBottom = windowVisibleRect.bottom - topInScreen;
        int softInputHeight = bottom - visibleBottom;
        if (mCurPanel != null) {
            mExternalPanelheight = mCurPanel.getMeasuredHeight();
            Logger.i("PannelLayout", "pannel Height:" + mExternalPanelheight);
        } else if (softInputHeight > 0) {
            Logger.i("PannelLayout", "softInputHeight:" + softInputHeight);

            mExternalPanelheight = softInputHeight;
        }
        final boolean softInputShowing = (mNormalSize > height) && (mNormalSize - height > 0.2f * mNormalSize);

        // 计算一下padding
//        right = right - left - mPaddingRight;
//        left = mPaddingLeft;
//        bottom = bottom - top - mPaddingBottom;
//        top = mPaddingTop;

        View main = getChildAt(0);

        // 软键盘被弹出
        if (softInputShowing && mStatus != SOFT_INPUT_PANEL) {
            main.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY));
            main.layout(left, 0, right, height);
        }
        // 弹出面板(含动画), 手动进行布局, 并返回
        // 当从soft切换成external时, 如果没有changed,则说明layout还没有变成全屏, 此时不用开始重新计算
        else if (mPendingStatus > SOFT_INPUT_PANEL && !(softInputShowing && !changed)) {
            if (mStatus != NONE) {
                calcAnimationPositionOfDiff();
            } else {
                calcAnimationPosition();
            }
            final int d = mExternalPanelheight - mAnimationPosition;
            mCurPanel.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mExternalPanelheight, MeasureSpec.EXACTLY));
            mCurPanel.layout(left, height - d, right, height - d + mExternalPanelheight);

            main.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(bottom - top - d, MeasureSpec.EXACTLY));
            main.layout(left, 0, right, height - d);
//            layoutChild(mCurPanel, left, top, right, bottom - mExternalPanelheight
//                    + mAnimationPosition, 0);
//            layoutChild(main, left, top, right, bottom - mExternalPanelheight + mAnimationPosition,
//            		main.getMeasuredHeight() - mExternalPanelheight + mAnimationPosition);
            if (mAnimationPosition == 0) {
                setStatus(mPendingStatus);
                mPendingStatus = NO_CHANGED;
            }
            // 重新绘图
            else {
                if (mListener != null) {
                    mListener.onLayoutChange(mStatus, mPendingStatus);
                }
                this.postDelayed(this, FRAME_DELAY);
            }

            return;
        }
        // 收回
        else if (mPendingStatus == NONE && mStatus > SOFT_INPUT_PANEL) {
            calcAnimationPosition();

            mCurPanel.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mExternalPanelheight, MeasureSpec.EXACTLY));
            mCurPanel.layout(left, height - mAnimationPosition, right, height - mAnimationPosition + mExternalPanelheight);
            main.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(bottom - top - mAnimationPosition, MeasureSpec.EXACTLY));
            main.layout(left, 0, right, height - mAnimationPosition);
            if (mAnimationPosition == 0) {
                setStatus(mPendingStatus);
                mPendingStatus = NO_CHANGED;

            }
            // 重新绘图
            else {
                if (mListener != null) {
                    mListener.onLayoutChange(mStatus, mPendingStatus);
                }
                this.postDelayed(this, FRAME_DELAY);
            }
            return;
        }
        // 弹出面板, 固定住
        else if (mStatus > SOFT_INPUT_PANEL && !softInputShowing) {
            mCurPanel.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mExternalPanelheight, MeasureSpec.EXACTLY));
            mCurPanel.layout(left, height - mExternalPanelheight, right, height);
            main.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(bottom - top - mExternalPanelheight, MeasureSpec.EXACTLY));
            main.layout(left, 0, right, height - mExternalPanelheight);
//            layoutChild(mCurPanel, left, top, right, bottom - mExternalPanelheight, 0);
//            layoutChild(main, left, top, right, bottom - mExternalPanelheight, main.getMeasuredHeight()
//                    - mExternalPanelheight);
        }
        // 否则, 使用默认的布局算法(NONE, SOFT_INPUT)
        else {
            main.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY));
//            layoutChild(main, left, top, right, bottom, main.getMeasuredHeight());
            main.layout(left, 0, right, height);
        }

        // 当软键盘被显示
        if (changed && softInputShowing) {
            // 当前正在显示其他面板
            // 直接调super.onLayout重新布局
            finalStatus = SOFT_INPUT_PANEL;
            this.mPendingStatus = NO_CHANGED;
        } else if (this.mPendingStatus == NONE || (changed && !softInputShowing)) {
            finalStatus = NONE;
            this.mPendingStatus = NO_CHANGED;
        }
        setStatus(finalStatus);
    }


    /**
     * 计算动画位置
     */
    private void calcAnimationPositionOfDiff() {
        long currentTime = AnimationUtils.currentAnimationTimeMillis();
        if (currentTime >= mAnimationStart + ANIMATION_DURATION_DIFF) {
            mAnimationPosition = 0;
        } else {
            float factor = (float) (AnimationUtils.currentAnimationTimeMillis() - mAnimationStart) / ANIMATION_DURATION_DIFF;
            mAnimationPosition = (int) (diff - AnimateUtils.viscousFluid(factor) * diff);
        }
    }

    /**
     * 计算动画位置
     */
    private void calcAnimationPosition() {
        long currentTime = AnimationUtils.currentAnimationTimeMillis();
        if (currentTime >= mAnimationStart + ANIMATION_DURATION) {
            mAnimationPosition = 0;
        } else {
            float factor = (float) (AnimationUtils.currentAnimationTimeMillis() - mAnimationStart) / ANIMATION_DURATION;
            mAnimationPosition = (int) (mExternalPanelheight - AnimateUtils.viscousFluid(factor) * mExternalPanelheight);
        }
    }

//    private void layoutChild(View child, int parentLeft, int parentTop, int parentRight, int parentBottom, int height)
//    {
//        if (child.getVisibility() != GONE)
//        {
//            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
//
//            final int width = child.getMeasuredWidth();
//
//            if (height == 0)
//            {
//                height = child.getMeasuredHeight();
//            }
//
//            int childLeft = parentLeft + lp.leftMargin;
//            int childTop;
//
//            int gravity = lp.gravity;
//            if (gravity == -1)
//            {
//                gravity = Gravity.TOP | Gravity.LEFT;
//            }
//
//            final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
//
//            switch (verticalGravity)
//            {
//            case Gravity.TOP:
//                childTop = parentTop + lp.topMargin;
//                break;
//            case Gravity.CENTER_VERTICAL:
//                childTop = parentTop + (parentBottom - parentTop - height) / 2 + lp.topMargin - lp.bottomMargin;
//                break;
//            case Gravity.BOTTOM:
//                childTop = parentBottom - height - lp.bottomMargin;
//                break;
//            default:
//                childTop = parentTop + lp.topMargin;
//            }
//
//            child.layout(childLeft, childTop, childLeft + width, childTop + height);
//        }
//    }

    /**
     * 偏移子view
     *
     * @param offset
     */
    public void offsetChildTopAndBottom(int offset) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).offsetTopAndBottom(offset);
        }
        // 在2.2以下系统, offsetTopAndBottom不会触发重绘, 所以需要手动触发重绘
        // 只要重绘自己就好. 不用invalidate子view
        this.invalidate();
    }

    private void setStatus(final int status) {
        if (status != mStatus) {
            final int old = mStatus;
            this.mStatus = status;
            if (mListener != null) {
                mListener.onPanelChanged(old, mStatus);
            }
            // 解除暂停
//            URLDrawable.resume();

            if (mCurPanel != null && status <= SOFT_INPUT_PANEL) {
                mCurPanel.setVisibility(View.GONE);
                mCurPanel = null;
            }
        }
    }

    /**
     * 设置监听器
     *
     * @param listener
     */
    public void setOnPanelChangeListener(PanelCallback listener) {
        this.mListener = listener;
    }

    /**
     * 返回当前的面板status
     *
     * @return 值为{@link XPanelContainer#NONE},{@link XPanelContainer#SOFT_INPUT_PANEL}, 或者从外面传入的面板id
     */
    public int getCurrentPanel() {
        return mStatus;
    }

    @Override
    public void run() {
        requestLayout();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

//    	if(VersionUtils.isHoneycombMR2())
//    	{
//    		// 横竖屏切换
//    		if(newConfig.orientation != orientation && (screenWidthDp != newConfig.screenWidthDp || screenHeightDp != newConfig.screenHeightDp))
//    		{
//    			orientation = -1;
//    			screenHeightDp = -1;
//    			screenHeightDp = -1;
//    			mNormalSize = -1;
//    			InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
//    		}
//    	}
//    	else
//    	{
        // 横竖屏切换
        if (newConfig.orientation != orientation) {
            orientation = -1;
            mNormalSize = -1;
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        }
//    	}
    }

    // 还原
    public void reset() {
        orientation = -1;
        mNormalSize = -1;
        mPendingStatus = NO_CHANGED;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        setStatus(NONE);
        requestLayout();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        requestLayout();
        return super.onApplyWindowInsets(insets);
    }

    public int getPanelState() {
        return mStatus;
    }

}