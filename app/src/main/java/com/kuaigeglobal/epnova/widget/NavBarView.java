package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.kuaigeglobal.epnova.R;
import com.nhsd.basic.ext.ViewUtils;

/** 底部导航栏占位。窗口模式读取分发到本控件的剩余 Insets，不重复补父容器已消费的空间。 */
public class NavBarView extends View {
    public static final int INSET_LEGACY = 0;
    public static final int INSET_WINDOW = 1;
    private int insetMode;
    private int navigationBottom;

    public NavBarView(Context context) { this(context, null); }
    public NavBarView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public NavBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public NavBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NavBarView, defStyleAttr, defStyleRes);
        insetMode = a.getInt(R.styleable.NavBarView_navBarInsetMode, INSET_LEGACY);
        a.recycle();
        validateMode(insetMode);
        ViewCompat.setOnApplyWindowInsetsListener(this, (view, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            if (bottom != navigationBottom) {
                navigationBottom = bottom;
                if (insetMode == INSET_WINDOW) requestLayout();
            }
            return insets;
        });
    }

    public int getInsetMode() { return insetMode; }
    public void setInsetMode(int mode) {
        validateMode(mode);
        if (mode == insetMode) return;
        insetMode = mode;
        requestLayout();
        ViewCompat.requestApplyInsets(this);
    }
    private static void validateMode(int mode) {
        if (mode != INSET_LEGACY && mode != INSET_WINDOW) throw new IllegalArgumentException("Unknown inset mode");
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewCompat.requestApplyInsets(this);
    }
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = navBarHeight();
        // 旧模式保留既有占位约定；新模式遵守父容器的固定高度和最大高度。
        setMeasuredDimension(getMeasuredWidth(), insetMode == INSET_LEGACY
                ? height : resolveSizeAndState(height, heightMeasureSpec, 0));
    }
    public int navBarHeight() {
        return insetMode == INSET_WINDOW ? navigationBottom
                : (ViewUtils.isSupportNavBar(getContext()) ? ViewUtils.getNavigationBarHeight(getContext()) : 0);
    }
}
