/**
 * @(#) z.sye.space.refreshrecyclerview.manager 2015/11/19;
 * <p>
 * Copyright (c), 2009 深圳孔方兄金融信息服务有限公司（Shenzhen kfxiong
 * Financial Information Service Co. Ltd.）
 * <p>
 * 著作权人保留一切权利，任何使用需经授权。
 */
package com.kuaigeglobal.epnova.widget.refreshrecycleview.manager;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kuaigeglobal.epnova.widget.refreshrecycleview.RefreshRecyclerView;
import com.kuaigeglobal.epnova.widget.refreshrecycleview.adapter.RefreshRecyclerViewAdapter;
import com.kuaigeglobal.epnova.widget.refreshrecycleview.listener.OnPullDownListener;


/**
 * The Management Class for RefreshRecyclerViewAdapter
 * Created by Syehunter on 2015/11/19.
 */
public class RefreshRecyclerAdapterManager {

    private RefreshRecyclerView recyclerView;
    private RefreshRecyclerViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private OnPullDownListener mOnPullDownListener;
    private RefreshRecyclerViewAdapter.OnItemClickListener mOnItemClickListener;
    private RefreshRecyclerViewAdapter.OnItemLongClickListener mOnItemLongClickListener;
    private RecyclerView.ItemAnimator mItemAnimator;

    public RefreshRecyclerAdapterManager(
            RecyclerView.Adapter adapter, RecyclerView.LayoutManager layoutManager) {
        this.mAdapter = new RefreshRecyclerViewAdapter(adapter);

        if (null == layoutManager) {
            throw new NullPointerException("Couldn't resolve a null object reference of LayoutManager");
        }
        this.mLayoutManager = layoutManager;
        if (layoutManager instanceof GridLayoutManager) {
            //如果是header或footer，设置其充满整列
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(
                    new HeaderSapnSizeLookUp(mAdapter, ((GridLayoutManager) layoutManager).getSpanCount()));
        }
        this.mLayoutManager = layoutManager;
    }

    private RefreshRecyclerAdapterManager getInstance() {
        return RefreshRecyclerAdapterManager.this;
    }

    public RefreshRecyclerAdapterManager addHeaderView(View v) {
        mAdapter.addHeaderView(v);
        return getInstance();
    }

    public RefreshRecyclerAdapterManager addHeaderView(View v, int position) {
        mAdapter.addHeaderView(v, position);
        return getInstance();
    }


    public RefreshRecyclerAdapterManager removeHeaderView(View v) {
        mAdapter.removeHeader(v);
        return getInstance();
    }

    public RefreshRecyclerViewAdapter getAdapter() {
        return mAdapter;
    }


    public RefreshRecyclerAdapterManager setOnPullDownListener(OnPullDownListener onPullDownListener) {
        this.mOnPullDownListener = onPullDownListener;
        return getInstance();
    }


//    public RefreshRecyclerAdapterManager setLayoutManager(RecyclerView.LayoutManager layoutManager){
//        if (null == layoutManager){
//            throw new NullPointerException("Couldn't resolve a null object reference of LayoutManager");
//        }
//        this.mLayoutManager = layoutManager;
//        if (layoutManager instanceof GridLayoutManager){
//            //如果是header或footer，设置其充满整列
//            ((GridLayoutManager)layoutManager).setSpanSizeLookup(
//                    new HeaderSapnSizeLookUp(mAdapter, ((GridLayoutManager) layoutManager).getSpanCount()));
//        }
//        return getInstance();
//    }

    public RefreshRecyclerAdapterManager setOnItemClickListener
            (RefreshRecyclerViewAdapter.OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
        return getInstance();
    }

    public RefreshRecyclerAdapterManager setOnItemLongClickListener(
            RefreshRecyclerViewAdapter.OnItemLongClickListener onItemLongClickListener) {
        this.mOnItemLongClickListener = onItemLongClickListener;
        return getInstance();
    }

    public void onRefreshCompleted() {
        if (null == recyclerView) {
            throw new NullPointerException("recyclerView is null");
        }
        if (null == mAdapter) {
            throw new NullPointerException("adapter is null");
        }
        recyclerView.refreshComplete();


    }

    public RefreshRecyclerView getRecyclerView() {
        if (null == recyclerView) {
            throw new NullPointerException("Couldn't resolve a null object reference of RefreshRecyclerView");
        }
        return recyclerView;
    }

    public RefreshRecyclerAdapterManager setItemAnimator(RecyclerView.ItemAnimator itemAnimator) {
        this.mItemAnimator = itemAnimator;
        return getInstance();
    }


    public void into(RefreshRecyclerView recyclerView, Context context) {
        if (null == recyclerView) {
            throw new NullPointerException("Couldn't resolve a null object reference of RefreshRecyclerView");
        }

        mAdapter.putLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mAdapter);

        if (null != mOnPullDownListener) {
            recyclerView.setOnPullDownListener(mOnPullDownListener);
        }

        recyclerView.setItemAnimator(mItemAnimator);

        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnItemLongClickListener);
        recyclerView.setLayoutManager(mLayoutManager);
        this.recyclerView = recyclerView;
    }

}
