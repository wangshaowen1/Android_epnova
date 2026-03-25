package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.nhsd.basic.ext.ViewUtils;

/**
 * @author xiaoowuu
 */
public class NavBarView extends View {

    public NavBarView(Context context) {
        super(context);
    }

    public NavBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NavBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NavBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //if (isInEditMode()) {
        //    setMeasuredDimension(getMeasuredWidth(), 80);
        //} else {
        setMeasuredDimension(getMeasuredWidth(), navBarHeight());
        //}
    }

    public int navBarHeight() {
        return ViewUtils.isSupportNavBar(getContext()) ? ViewUtils.getNavigationBarHeight(getContext()) : 0;
    }
}
