package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;

public class SelectorGradientTextView extends GradientTextView {

    private boolean mGradientColorEnabled = true;


    public SelectorGradientTextView(Context context) {
        super(context);
        setGradientColorEnabled(false);
    }

    public SelectorGradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setGradientColorEnabled(false);
    }


    @Override
    protected void gradientText() {
        if (mGradientColorEnabled) {
            super.gradientText();
        } else {
            getPaint().setShader(null);
        }
    }

    public void setGradientColorEnabled(boolean gradientColorEnabled) {
        this.mGradientColorEnabled = gradientColorEnabled;
        gradientText();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setGradientColorEnabled(selected);
    }
}
