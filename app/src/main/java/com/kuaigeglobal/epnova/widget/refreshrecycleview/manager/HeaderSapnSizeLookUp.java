package com.kuaigeglobal.epnova.widget.refreshrecycleview.manager;

import androidx.recyclerview.widget.GridLayoutManager;

import com.kuaigeglobal.epnova.widget.refreshrecycleview.adapter.RefreshRecyclerViewAdapter;


/**
 * Created by Syehunter on 2015/11/22.
 */
public class HeaderSapnSizeLookUp extends GridLayoutManager.SpanSizeLookup {

    private RefreshRecyclerViewAdapter mAdapter;
    private int mSpanSize;

    public HeaderSapnSizeLookUp(RefreshRecyclerViewAdapter adapter, int spanSize){
        this.mAdapter = adapter;
        this.mSpanSize = spanSize;
    }

    @Override
    public int getSpanSize(int position) {
        boolean isHeaderOrFooter = mAdapter.isHeader(position) ;
        return isHeaderOrFooter ? mSpanSize : 1;
    }
}
