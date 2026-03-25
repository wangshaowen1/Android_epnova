package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.blankj.utilcode.util.KeyboardUtils;
import com.nhsd.basic.utils.Logger;


public class ChatLayout extends LinearLayout {

    private static final String TAG = "ChatLayout";
    private int mNormalSize;
    private boolean softInputShowing;
    private EditText messageInputer;

    private View panel;
    private boolean detectSoftStatus=true;
    public ChatLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int height = bottom - top;

        // 初次布局, 记录下布局大小
        if (mNormalSize < 0 && changed && height > 0) {
            Configuration config = getResources().getConfiguration();
            mNormalSize = height;
        } else if (height > mNormalSize) {
            mNormalSize = height;
        }
        Rect windowVisibleRect = new Rect();
        getWindowVisibleDisplayFrame(windowVisibleRect);
        int[] point = new int[2];
        getLocationOnScreen(point);
//        getLocalVisibleRect()
        int topInScreen = point[1];

        int visibleBottom = windowVisibleRect.bottom - topInScreen;
        int softInputHeight = bottom - visibleBottom;
        boolean softInputShowing = (mNormalSize > height) && (mNormalSize - height > 0.2f * mNormalSize);
        if (detectSoftStatus&&this.softInputShowing != softInputShowing) {
            Logger.i(TAG, "on layout change");
            notifySoftInputChange(softInputShowing);
        }

    }

    public void notifySoftInputChange(boolean softInputShowing) {
        Logger.i(TAG, "notifySoftInputChange:" + softInputShowing + ", last status:" + this.softInputShowing);
        this.softInputShowing = softInputShowing;
        if (onLayoutListener != null) {
            Logger.i(TAG, "on soft change");
            onLayoutListener.onSoftInputChange(softInputShowing);
        }
    }

    public void setDetectSoftStatus(boolean detectSoftStatus) {
        this.detectSoftStatus = detectSoftStatus;
    }

    public interface OnLayoutListener {
        void onSoftInputChange(boolean softInputShowing);
    }

    private OnLayoutListener onLayoutListener;

    public void setOnLayoutListener(OnLayoutListener onLayoutListener) {
        this.onLayoutListener = onLayoutListener;
    }

    public boolean isSoftInputShowing() {
        return softInputShowing;
    }


    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        requestLayout();
        return super.onApplyWindowInsets(insets);
    }

    public void bindInputer(EditText messageInputer) {
        this.messageInputer = messageInputer;
    }

    public void bindPanel(View panel) {
        this.panel = panel;
    }

    public void showSoftInput() {
        if (softInputShowing) {
            return;
        }
        Logger.i(TAG, "showSoftInput");
        if (panel.getVisibility() == View.VISIBLE) {
            panel.setVisibility(View.GONE);
        }
        KeyboardUtils.showSoftInput(messageInputer);

    }

    public void hideSoftInput() {
        if (!softInputShowing) {
            return;
        }
        Logger.i(TAG, "hideSoftInput");
        KeyboardUtils.hideSoftInput(messageInputer);
        messageInputer.clearFocus();

    }


    public void showPanel() {
        if (panel.getVisibility() != View.VISIBLE) {
            panel.setVisibility(View.VISIBLE);
//            ViewGroup vg= (ViewGroup) panel.getParent();
//            vg.getLayoutParams().height= ViewGroup.LayoutParams.WRAP_CONTENT;
            requestLayout();
            if (softInputShowing) {
                hideSoftInput();
            }
        }
    }

    public void hidePanel() {
        if (panel.getVisibility() != View.GONE) {
            panel.setVisibility(View.GONE);
            requestLayout();

//            ViewGroup vg= (ViewGroup) panel.getParent();
//            vg.getLayoutParams().height= KotUtils.getDp(15);

        }
    }
}