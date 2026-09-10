package com.kuaigeglobal.epnova.widget.dragfooterview;

import android.view.View;
import android.widget.HorizontalScrollView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Created by dear33 on 2016/11/12.
 */
public class DefaultDragChecker implements IDragChecker {
    @Override
    public boolean canDrag(View childView) {
        if (childView instanceof RecyclerView) {
            return recyclerBottom((RecyclerView) childView);
        } else if (childView instanceof HorizontalScrollView) {
            return ((HorizontalScrollView) childView).getChildAt(0).getMeasuredWidth() - childView.getScrollX()
                    <= childView.getMeasuredWidth();
        } else if (childView instanceof ViewPager2) {
            RecyclerView.Adapter adapter = ((ViewPager2) childView).getAdapter();
            if (adapter == null || adapter.getItemCount() == 0) {
                return false;
            }
            return ((ViewPager2) childView).getCurrentItem() == adapter.getItemCount() - 1;
            /*RecyclerView recyclerView = (RecyclerView) ((ViewPager2) childView).getChildAt(0);
            return recyclerBottom(recyclerView);*/
        }

        return true;
    }

    private boolean recyclerBottom(RecyclerView recyclerView) {
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) {
            return false;
        }

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int lastCompletelyVisibleItemPosition;
        if (layoutManager instanceof GridLayoutManager || layoutManager instanceof LinearLayoutManager) {
            lastCompletelyVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
            ((StaggeredGridLayoutManager) layoutManager).findLastCompletelyVisibleItemPositions(into);
            int max = into[0];
            for (int value : into) {
                if (value > max) {
                    max = value;
                }
            }
            lastCompletelyVisibleItemPosition = max;
        } else {
            throw new IllegalArgumentException("still not support other LayoutManager");
        }

        return lastCompletelyVisibleItemPosition == adapter.getItemCount() - 1;
    }
}
