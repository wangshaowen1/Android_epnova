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

    // For more information, see https://code.google.com/p/android/issues/detail?id=5497
    // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.

    public static void assistActivity(Activity activity) {
        new AndroidBug5497Workaround(activity);
    }

    private View mChildOfContent;
    private int usableHeightPrevious;
    private FrameLayout.LayoutParams frameLayoutParams;

    private AndroidBug5497Workaround(Activity activity) {
        FrameLayout content = activity.findViewById(android.R.id.content);
        mChildOfContent = content.getChildAt(0);
        mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(this::possiblyResizeChildOfContent);
        frameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();
    }

    private void possiblyResizeChildOfContent() {
        int usableHeightNow = computeUsableHeight();
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
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
                if (contentView!=null) {
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