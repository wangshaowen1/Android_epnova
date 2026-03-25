package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SizeUtils;

public class TopSpaceDecoration extends RecyclerView.ItemDecoration {

    private int spacing;
    private int spanCount=1;

    public TopSpaceDecoration() {
        this.spacing = SizeUtils.dp2px(12);
    }
    public TopSpaceDecoration(int spacing) {
        this.spacing = spacing;
    }

    public TopSpaceDecoration(int spacing, int spanCount) {
        this.spacing = spacing;
        this.spanCount=spanCount;
    }
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        if (position <spanCount) {
            outRect.top = spacing;
        }
    }
}