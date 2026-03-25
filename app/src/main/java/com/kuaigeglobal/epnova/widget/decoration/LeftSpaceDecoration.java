package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SizeUtils;

public class LeftSpaceDecoration extends RecyclerView.ItemDecoration {

    private int spacing;
    private int spanCount=1;

    public LeftSpaceDecoration() {
        this.spacing = SizeUtils.dp2px(12);
    }
    public LeftSpaceDecoration(int spacing) {
        this.spacing = spacing;
    }

    public LeftSpaceDecoration(int spacing, int spanCount) {
        this.spacing = spacing;
        this.spanCount=spanCount;
    }
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        if (position <spanCount) {
            outRect.left = spacing;
        }
    }
}