package com.kuaigeglobal.epnova.widget.refreshrecycleview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.kuaigeglobal.epnova.widget.refreshrecycleview.listener.OnPullDownListener;
import com.kuaigeglobal.epnova.widget.refreshrecycleview.widget.RefreshHeader;

import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import in.srain.cube.views.ptr.indicator.PtrIndicator;


/**
 * Created by Syehunter on 2015/11/22.
 */
public class RefreshRecyclerView extends PtrFrameLayout {

    private Context mContext;
    private RecyclerView mRecyclerView;
    private LayoutParams params;
    private RefreshHeader mHeaderView;
    private float mDownY;

    public RefreshRecyclerView(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public RefreshRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        init();
    }

    public RefreshRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    private void init() {
        mRecyclerView = new RecyclerView(mContext);
        params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mRecyclerView.setLayoutParams(params);
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
//        mRecyclerView.setScrollbarFadingEnabled(false);
        addView(mRecyclerView);

        setResistance(1.7f);
        setRatioOfHeaderHeightToRefresh(1.2f);
        setDurationToClose(200);
        setDurationToCloseHeader(1000);
        setKeepHeaderWhenRefresh(true);
        setPullToRefresh(false);
        //ViewPager滑动冲突
        disableWhenHorizontalMove(true);

        mHeaderView = new RefreshHeader(mContext) {

            @Override
            public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
                super.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
                if (onPositionChangeListener != null) {
                    onPositionChangeListener.onPositionChange(ptrIndicator.getCurrentPosY() - ptrIndicator.getLastPosY());
                }

            }

            @Override
            public void onUIRefreshBegin(PtrFrameLayout frame) {
                super.onUIRefreshBegin(frame);
                if(onPositionChangeListener!=null){
                    onPositionChangeListener.onRefreshBegin();
                }
            }

            @Override
            public void onUIRefreshComplete(PtrFrameLayout frame) {
                super.onUIRefreshComplete(frame);
                if(onPositionChangeListener!=null){
                    onPositionChangeListener.onRefreshEnd();
                }
            }
        };
        setHeaderView(mHeaderView);
        addPtrUIHandler(mHeaderView);

    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        if (null == adapter) {
            throw new NullPointerException("adapter cannot be null");
        }
        mRecyclerView.setAdapter(adapter);
    }

    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        mRecyclerView.setLayoutManager(layoutManager);
    }

    public void setItemAnimator(RecyclerView.ItemAnimator itemAnimator) {
        if (null == itemAnimator) {
            return;
        }
        mRecyclerView.setItemAnimator(itemAnimator);
    }


    public void addOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (null == listener) {
            return;
        }
        mRecyclerView.addOnScrollListener(listener);
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return mRecyclerView.getLayoutManager();
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decor) {
        if (null == decor) {
            return;
        }
        mRecyclerView.addItemDecoration(decor);
    }


    public void setOnPullDownListener(final OnPullDownListener listener) {

        //当前允许下拉刷新
        setPtrHandler(new PtrHandler() {

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                listener.onPullDown();
            }
        });
    }


    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void onRefreshCompleted() {
        refreshComplete();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {

        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (onTouchListener != null) {
                        onTouchListener.onTouch();
                    }
                    mDownY = e.getY();
                    break;
                case MotionEvent.ACTION_UP:

                    break;
            }
        } else {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (onTouchListener != null) {
                        onTouchListener.onTouch();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
        }
        return super.onTouchEvent(e);
    }

    @Override
    public void offsetTopAndBottom(int offset) {

        super.offsetTopAndBottom(offset);
    }

    OnTouchListener onTouchListener;

    public void setOnTouchListener(OnTouchListener onTouchListener) {
        this.onTouchListener = onTouchListener;
    }

    public interface OnTouchListener {
        public void onTouch();
    }

    OnPositionChangeListener onPositionChangeListener;

    public void setOnPositionChangeListener(OnPositionChangeListener onPositionChangeListener) {
        this.onPositionChangeListener = onPositionChangeListener;
    }

    public void setHeaderEnable(boolean enable){
        mHeaderView.setEnabled(enable);
    }
    public interface OnPositionChangeListener {
        public void onPositionChange(int offset);

        public void onRefreshBegin();
        public void onRefreshEnd();
    }
}
