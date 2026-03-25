package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

public class NoBottomFadeScrollView extends NestedScrollView {
    public NoBottomFadeScrollView(@NonNull Context context) {
        super(context);
    }

    public NoBottomFadeScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoBottomFadeScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return 0f;
    }
}
