package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

public class ViewFlipperWithListener extends ViewFlipper {


    public ViewFlipperWithListener(Context context) {
        super(context);
    }

    public ViewFlipperWithListener(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void showNext() {
        super.showNext();
        if(onViewFlipperListener!=null) {
            onViewFlipperListener.onDisplay(getDisplayedChild());
        }
    }

    @Override
    public void showPrevious() {
        super.showPrevious();
        if(onViewFlipperListener!=null) {
            onViewFlipperListener.onDisplay(getDisplayedChild());
        }
    }
    private  OnViewFlipperListener onViewFlipperListener;

    public void setOnViewFlipperListener(OnViewFlipperListener onViewFlipperListener) {
        this.onViewFlipperListener = onViewFlipperListener;
    }

    public  interface OnViewFlipperListener{
        void onDisplay(int position);
    }
}