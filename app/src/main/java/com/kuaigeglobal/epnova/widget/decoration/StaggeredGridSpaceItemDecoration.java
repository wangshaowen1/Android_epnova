package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class StaggeredGridSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int horizontalSpacing;
    private final int verticalSpacing;
    private final int spanCount;
    private final int sideSpacing;
    public StaggeredGridSpaceItemDecoration(int spanCount,int horizontalSpacing, int verticalSpacing,int sideSpacing) {
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
        this.sideSpacing=sideSpacing;
        this.spanCount=spanCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();


        if(!layoutParams.isFullSpan()) {
            // item position
            int position = layoutParams.getViewLayoutPosition();
            // span index
            int spanIndex = layoutParams.getSpanIndex();
            // span size
            int spanSize =spanCount;
            if (spanIndex == 0) { // First item in the row
                outRect.left = sideSpacing;
            } else { // Not the first item, so we add half the spacing to avoid double spacing
                outRect.left = horizontalSpacing / 2;
            }
            if (spanIndex == spanSize - 1) { // Last item in the row
                outRect.right = sideSpacing;
            } else { // Not the last item, so we add half the spacing to avoid double spacing
                outRect.right = horizontalSpacing / 2;
            }

            // 设置顶部间距
            if (position < spanSize) {
                outRect.top = verticalSpacing;
            }

            // 设置底部间距
            outRect.bottom = verticalSpacing;
        }
    }
}
