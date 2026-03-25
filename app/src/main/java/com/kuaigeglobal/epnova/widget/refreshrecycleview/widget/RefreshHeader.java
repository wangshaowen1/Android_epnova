package com.kuaigeglobal.epnova.widget.refreshrecycleview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.kuaigeglobal.epnova.R;

import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrUIHandler;
import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * Created by Syehunter on 2016/1/5.
 */
public class RefreshHeader extends FrameLayout implements PtrUIHandler {

    private int mRotateAniTime = 150;
    private View mProgressBar;


    public RefreshHeader(Context context) {
        super(context);
        initViews(null);
    }

    public RefreshHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(attrs);
    }

    public RefreshHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews(attrs);
    }

    protected void initViews(AttributeSet attrs) {
        TypedArray arr = getContext().obtainStyledAttributes(attrs, in.srain.cube.views.ptr.R.styleable.PtrClassicHeader, 0, 0);
        if (arr != null) {
            mRotateAniTime = arr.getInt(in.srain.cube.views.ptr.R.styleable.PtrClassicHeader_ptr_rotate_ani_time, mRotateAniTime);
        }
        View header = LayoutInflater.from(getContext()).inflate(R.layout.pull_to_refresh_header_vertical, this);
        mProgressBar = header.findViewById(R.id.pull_to_refresh_progress);

        resetView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }


    private void resetView() {
//        mProgressBar.setVisibility(INVISIBLE);
    }


    @Override
    public void onUIReset(PtrFrameLayout frame) {
        resetView();
    }

    @Override
    public void onUIRefreshPrepare(PtrFrameLayout frame) {


    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        mProgressBar.setVisibility(VISIBLE);


    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {

        mProgressBar.setVisibility(VISIBLE);

    }


    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {

        final int mOffsetToRefresh = frame.getOffsetToRefresh();
        final int currentPos = ptrIndicator.getCurrentPosY();
        final int lastPos = ptrIndicator.getLastPosY();

        if (currentPos < mOffsetToRefresh && lastPos >= mOffsetToRefresh) {
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {

            }
        } else if (currentPos > mOffsetToRefresh && lastPos <= mOffsetToRefresh) {
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {

            }
        }
    }


}
