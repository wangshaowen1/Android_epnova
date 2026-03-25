package com.kuaigeglobal.epnova.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.nhsd.basic.utils.Logger;
import com.kuaigeglobal.epnova.common.utils.AnimateUtils;


/**
 * 该类不能设置forground..
 * 负责显示扩展面板的Container. 可以显示一个扩展面板, 当软键盘弹出时.
 * 则自动缩回扩展面板
 */
public class PanelLayout extends FrameLayout implements Runnable {
    private static final String TAG = PanelLayout.class.getSimpleName();

    /**
     * 无面板弹出
     */
    public static final int NONE = 0;
    /**
     * 软键盘
     */
    public static final int SOFT_INPUT_PANEL = 1;
    //    /**
//     * 表情
//     */
    public static final int EMOTICON_PANEL = 2;

    public static final int EXT_PANEL_PICTURE = 3;

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

    private int diff;
    private boolean requestCaculate;
    private View lastPanel;
    private View dockLayout;


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
        void onPanelChanged(int oldPanel, int newPanel);

        /**
         * 回调, 当showExternalPanel传值不为NONE or SOFT_INPUT_PANEL时, 会通知这货来创建view
         *
         * @param panelId showExternalPanel中panelid
         * @return 对应的view
         */
        View onCreatePanel(int panelId);
    }


//    public XPanelContainer(Context context, AttributeSet attrs, int defStyle)
//    {
//        super(context, attrs, defStyle);
//        mExternalPanelheight = (int) (196 * context.getResources().getDisplayMetrics().density);
//    }

    public PanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mExternalPanelheight = (int) (196 * context.getResources().getDisplayMetrics().density);
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
     * @param panelId {@link PanelLayout#SOFT_INPUT_PANEL}, 或者从外面传入的面板id, 如果值为其他id, 则会触发onShowPanel回调
     */
    public void showExternalPanel(int panelId) {
//        FragmentManager manager = aioFrag.getFragmentManager();
        // 软键盘
        if (panelId == SOFT_INPUT_PANEL) {
            messageInputer.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(messageInputer, 0);
            // 这里不改变状态, 弹软键盘成功的话, 会触发onLayout.在onLayout里再改
            requestCaculate = false;
        }
        // 显示其他扩展面板
        else if (panelId > SOFT_INPUT_PANEL) {
            View panel = mPanels.get(panelId);

            if (panel == null) {
                panel = mListener.onCreatePanel(panelId);
                if (panel != null) {
                    LayoutParams defaultParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    LayoutParams params = defaultParams;
                    if (panel.getLayoutParams() != null) {
                        params = (LayoutParams) panel.getLayoutParams();
                    }
                    addView(panel, params);
                    mPanels.put(panelId, panel);
                }
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
                if (mAnimationPosition != 0) {
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
        requestCaculate = false;
        boolean ret = mStatus > NONE;
        if (mStatus == SOFT_INPUT_PANEL) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        } else if (mStatus > SOFT_INPUT_PANEL) {
            if (mAnimationPosition != 0) {
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

//        Logger.i("Soft", "measure height:" + MeasureSpec.getSize(heightMeasureSpec) + " measure widthMeasureSpec:" + MeasureSpec.getSize(widthMeasureSpec) + ",screenHeight:" + ScreenUtil.getScreenHeight());
        if(dockLayout!=null) {
            measureChild(dockLayout, widthMeasureSpec, heightMeasureSpec);
        }
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
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

//        lastMeasureHeight = mCurPanel.getMeasuredHeight();
//        Logger.i("PannelLayout", " last measure height:" + lastMeasureHeight +
//                ", pannel measure height:" + mCurPanel.getMeasuredHeight() + ",diff:" + diff);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

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
        if (softInputHeight > 0) {
            mExternalPanelheight = softInputHeight;
        } else if (mCurPanel != null) {
            mExternalPanelheight = mCurPanel.getMeasuredHeight();
        }
        final boolean softInputShowing = softInputHeight > 0 ? true : (mNormalSize > height) && (mNormalSize - height > 0.2f * mNormalSize);

        Logger.i("Soft", "softInputShowing:"+softInputShowing+" wrect  top:" + windowVisibleRect.top + ",w rect,bottom:" + windowVisibleRect.bottom+",wheight:"+windowVisibleRect.height());

        View main = getChildAt(0);

        // 软键盘被弹出
//        if (softInputShowing && mStatus != SOFT_INPUT_PANEL) {
//
//        }
        main.layout(left, 0, right, height);
        // 弹出面板(含动画), 手动进行布局, 并返回
        // 当从soft切换成external时, 如果没有changed,则说明layout还没有变成全屏, 此时不用开始重新计算
        if (mPendingStatus > SOFT_INPUT_PANEL && !(softInputShowing && !changed)) {
            if (mStatus != NONE) {
                calcAnimationPositionOfDiff();
            } else {
                calcAnimationPosition();
            }
            final int d = mExternalPanelheight - mAnimationPosition;
//            Logger.i("PannelLayout","up diff:"+diff+", mAnimationPosition:"+mAnimationPosition+",mExternalPanelheight - mAnimationPosition:"+d+", top:"+(height - d));
            mCurPanel.layout(left, height - d, right, height - d + mExternalPanelheight);
            if (mAnimationPosition == 0) {
                setStatus(mPendingStatus);
                mPendingStatus = NO_CHANGED;
            }
            // 重新绘图
            else {
                this.postDelayed(this, FRAME_DELAY);
            }

        }
        // 收回
        else if (mPendingStatus == NONE && mStatus > SOFT_INPUT_PANEL) {
            calcAnimationPosition();

            mCurPanel.layout(left, height - mAnimationPosition, right, height - mAnimationPosition + mExternalPanelheight);
            if (mAnimationPosition == 0) {
                setStatus(mPendingStatus);
                mPendingStatus = NO_CHANGED;
            }
            // 重新绘图
            else {
                this.postDelayed(this, FRAME_DELAY);
            }
        }
        // 弹出面板, 固定住
        else if (mStatus > SOFT_INPUT_PANEL && !softInputShowing) {
            mCurPanel.layout(left, height - mExternalPanelheight, right, height);
        }
        // 否则, 使用默认的布局算法(NONE, SOFT_INPUT)
        else {
            // 当软键盘被显示
            if ((changed || mStatus != SOFT_INPUT_PANEL) && softInputShowing) {
                // 当前正在显示其他面板
                // 直接调super.onLayout重新布局
                finalStatus = SOFT_INPUT_PANEL;
                this.mPendingStatus = NO_CHANGED;
            } else if (this.mPendingStatus == NONE || (mStatus == SOFT_INPUT_PANEL && !softInputShowing)) {
                finalStatus = NONE;
                this.mPendingStatus = NO_CHANGED;
            }
            setStatus(finalStatus);

        }
    if(dockLayout!=null) {
        dockLayout.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.AT_MOST));
        int emojBottom = 0;
        if (mStatus > SOFT_INPUT_PANEL) {
            emojBottom = mCurPanel.getTop();
        } else if (mStatus == NONE) {
            emojBottom = main.getBottom();
        } else if (softInputShowing) {
//            emojBottom = visibleRect.bottom-visibleRect.top-89;
            emojBottom = visibleBottom;
        }
        int emojTop = emojBottom - dockLayout.getMeasuredHeight();

        if (mStatus == NONE && !emojLayoutVisible) {
            dockLayout.setVisibility(View.GONE);
        } else {
            dockLayout.setVisibility(View.VISIBLE);

            if (dockLayout != null) {
                dockLayout.layout(left, emojTop, right, emojBottom);

            }
        }
    }
    }

    //设置hideAllPanel后是否需要隐藏  默认不隐藏 true
    private boolean emojLayoutVisible = true;

    public void setEmojLayoutVisible(boolean emojLayoutVisible) {
        this.emojLayoutVisible = emojLayoutVisible;
    }
    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        requestLayout();
        return super.onApplyWindowInsets(insets);

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
     * @return 值为{@link XPanelContainer#NONE},{@link PanelLayout#SOFT_INPUT_PANEL}, 或者从外面传入的面板id
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
        mPendingStatus = NO_CHANGED;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        setStatus(NONE);
        requestLayout();
    }

    public int getCurrentPanelHeight() {
        if (mStatus != NONE)
            return mExternalPanelheight;
        return 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public int getPanelState() {
        return mStatus;
    }

}
