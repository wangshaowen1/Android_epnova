package com.kuaigeglobal.epnova.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.nhsd.crop.util.ViewUtils;

public class AndroidBug5497Workaround {

    public static void assistActivity(Activity activity) {
        new AndroidBug5497Workaround(activity);
    }

    private View mChildOfContent;
    private int usableHeightPrevious;
    private int usableWidthPrevious; // 新增：用于记录上一次的宽度
    private FrameLayout.LayoutParams frameLayoutParams;

    private AndroidBug5497Workaround(Activity activity) {
        FrameLayout content = activity.findViewById(android.R.id.content);
        mChildOfContent = content.getChildAt(0);
        mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(this::possiblyResizeChildOfContent);
        frameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();
        // 初始化时记录当前宽度
        usableWidthPrevious = mChildOfContent.getRootView().getWidth();
    }

    private void possiblyResizeChildOfContent() {
        int usableHeightNow = computeUsableHeight();
        int usableWidthNow = mChildOfContent.getRootView().getWidth();

        // 【核心改造 1】：判断是否发生了屏幕旋转（宽度发生剧变）
        // 如果宽度变了，说明是横竖屏切换，绝不是键盘弹出。此时必须重置为 MATCH_PARENT。
        if (usableWidthNow != usableWidthPrevious) {
            usableWidthPrevious = usableWidthNow;
            usableHeightPrevious = usableHeightNow;
            if (frameLayoutParams.height != FrameLayout.LayoutParams.MATCH_PARENT) {
                frameLayoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
                mChildOfContent.requestLayout();
            }
            return;
        }

        // 【核心改造 2】：横屏下（通常是全屏看视频），不需要处理键盘遮挡，强制重置高度
        int orientation = mChildOfContent.getContext().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (frameLayoutParams.height != FrameLayout.LayoutParams.MATCH_PARENT) {
                frameLayoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
                mChildOfContent.requestLayout();
            }
            usableHeightPrevious = usableHeightNow;
            return;
        }

        // 原有的键盘高度计算逻辑
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;

            // 键盘高度通常大于屏幕高度的1/4
            boolean showKeyBoard = heightDifference > (usableHeightSansKeyboard / 4);

            if (showKeyBoard) {
                // keyboard probably just became visible
                frameLayoutParams.height = usableHeightSansKeyboard - heightDifference + ViewUtils.getStatusBarHeight(mChildOfContent.getContext());
            } else {
                // keyboard probably just became hidden
                frameLayoutParams.height = usableHeightSansKeyboard - getNavigationBarHeight(mChildOfContent.getContext());
            }

            if (showKeyBoard) {
                mChildOfContent.postDelayed(() -> mChildOfContent.requestLayout(), 150);
            } else {
                mChildOfContent.requestLayout();
            }
            usableHeightPrevious = usableHeightNow;
        }
    }

    private int computeUsableHeight() {
        Rect r = new Rect();
        mChildOfContent.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);
    }

    private int getNavigationBarHeight(Context context) {
        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        if (!hasMenuKey) {
            // This device has a navigation bar
            int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (checkNavigationBarShow(mChildOfContent.getContext()) && resourceId > 0) {
                return context.getResources().getDimensionPixelSize(resourceId);
            } else {
                return 0;
            }
        }
        return 0;
    }

    private boolean checkNavigationBarShow(@NonNull Context context) {
        boolean show = false;
        if (context instanceof Activity) {
            Window window = ((Activity) context).getWindow();
            Display display = window.getWindowManager().getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);

            View decorView = window.getDecorView();
            Configuration conf = context.getResources().getConfiguration();
            if (Configuration.ORIENTATION_LANDSCAPE == conf.orientation) {
                View contentView = decorView.findViewById(android.R.id.content);
                if (contentView != null) {
                    show = (point.x != contentView.getWidth());
                }
            } else {
                Rect rect = new Rect();
                decorView.getWindowVisibleDisplayFrame(rect);
                show = (rect.bottom != point.y);
            }
        }
        return show;
    }
}