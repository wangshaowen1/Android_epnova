package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;


public class RightSpaceDecoration extends RecyclerView.ItemDecoration {

    private int spacing;
    private int spanCount=1;

    public RightSpaceDecoration() {
        this.spacing = SizeUtils.dp2px(12);
    }
    public RightSpaceDecoration(int spacing) {
        this.spacing = spacing;
    }

    public RightSpaceDecoration(int spacing, int spanCount) {
        this.spacing = spacing;
        this.spanCount=spanCount;
    }
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position

        int totalCount = parent.getAdapter().getItemCount();
        if (position >=totalCount-spanCount) {
            outRect.right = spacing;
        }
    }
}