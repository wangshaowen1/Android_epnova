package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * 可观测大小变化的ConstraintLayout
 */
public class SizeObservableConstraintLayout extends ConstraintLayout {

    private int cw = 0;
    private int ch = 0;
    @Nullable
    private SizeObserver sizeObserver;

    public SizeObservableConstraintLayout(@NonNull Context context) {
        super(context);
    }

    public SizeObservableConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SizeObservableConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SizeObservableConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != cw && h != ch) {
            cw = w;
            ch = h;
            if (sizeObserver != null) {
                sizeObserver.onSizeChanged(w, h, oldw, oldh);
            }
        }
    }

    public void setSizeObserver(@Nullable SizeObserver sizeObserver) {
        this.sizeObserver = sizeObserver;
    }

    public interface SizeObserver {
        void onSizeChanged(int w, int h, int oldw, int oldh);
    }
}