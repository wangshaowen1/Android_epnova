package com.kuaigeglobal.epnova.common.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import com.blankj.utilcode.util.ScreenUtils;
import com.kuaigeglobal.epnova.App;

public class KeyboardUtils {

    public enum ShownModel {
        Delay, AfterLayout, None
    }

    public static void showKeyboard(View view, ShownModel shownModel) {
        if (shownModel == ShownModel.None) {
            showKeyboard(view);
        } else if (shownModel == ShownModel.AfterLayout) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    showKeyboard(view);
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        } else if (shownModel == ShownModel.Delay) {
            App.getInstance().postDelay(() -> showKeyboard(view), 100);
        }
    }

    public static void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            view.requestFocus();
            imm.showSoftInput(view, 0);
        }
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static int getKeyboardHeight(Activity context) {
        Rect rect = new Rect();
        context.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int screenHeight = ScreenUtils.getAppScreenHeight();
        int keyboardHeight = screenHeight - rect.bottom; // 输入法的高度
        if (Math.abs(keyboardHeight) > screenHeight / 5) {
            return keyboardHeight;
        }
        return 0;
    }
}
