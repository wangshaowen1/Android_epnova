package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhsd.basic.utils.Logger;

/**
 * 不会导致item宽高不一致
 * 自适应任意列的 GridLayoutManager 的分割块 ItemDecoration
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spanCount;
    private final int dividerWidth;
    private final int dividerHeight;

    private int dividerHeightTop;
    private int dividerHeightBottom;
    Direction direction;

    public enum Direction {
        Top, Bottom, Middle, All
    }

    public GridSpacingItemDecoration(int dividerWidth, int dividerHeight) {
        this(dividerWidth, dividerHeight, Direction.Middle);
    }

    public GridSpacingItemDecoration(int dividerWidth, int dividerHeight, Direction direction) {
        this(1, dividerWidth, dividerHeight, direction);
    }

    /**
     * @param spanCount      gridLayoutManager 列数
     * @param dividerWidth 分割块宽,单位:
     */
    public GridSpacingItemDecoration(int spanCount, int dividerWidth, int dividerHeight) {
        this(spanCount, dividerWidth, dividerHeight, Direction.Middle);
    }


    /**
     * @param spanCount      gridLayoutManager 列数
     * @param dividerWidth 分割块宽,单位:
     */
    public GridSpacingItemDecoration(int spanCount, int dividerWidth, int dividerHeight, Direction direction) {
        this.spanCount = spanCount;
        this.dividerWidth =dividerWidth;
        this.dividerHeight = dividerHeight;
        if (direction == Direction.Middle) {
            this.dividerHeightTop = dividerHeight / 2;
            this.dividerHeightBottom = dividerHeightTop;
        } else if (direction == Direction.Top) {
            this.dividerHeightTop = dividerHeight;
            this.dividerHeightBottom = 0;
        } else if (direction == Direction.Bottom) {
            this.dividerHeightTop = 0;
            this.dividerHeightBottom = dividerHeight;
        } else if (direction == Direction.All) {
            this.dividerHeightTop = dividerHeight;
            this.dividerHeightBottom = dividerHeight;
        }
        this.direction = direction;
    }

    public void setDividerHeightBottom(int dividerHeightBottom) {
        this.dividerHeightBottom = dividerHeightBottom;
    }

    public void setDividerHeightTop(int dividerHeightTop) {
        this.dividerHeightTop = dividerHeightTop;
    }

    @Override
    public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, child, parent, state);

        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            setGridLayoutManagerOffsets(outRect, child, parent);
            return;
        }

        int position = parent.getChildAdapterPosition(child);
        int totalCount = parent.getAdapter().getItemCount();

        int lastRow = (totalCount + spanCount - 1) / spanCount - 1;
        // 获取第几列
        int column = position % spanCount;
        // 第几行
        int row = position / spanCount;
        if (row == 0 && direction != Direction.Middle && direction != Direction.Bottom) { // 设置top
            outRect.top = dividerHeightTop;
        } else if (row == lastRow && direction != Direction.Middle && direction != Direction.Top) {
            outRect.bottom = dividerHeightBottom;
        } else if (row != 0) {
            outRect.top = dividerHeight;
        }
        outRect.left = (column * dividerWidth / spanCount);
        outRect.right = dividerWidth - (column + 1) * dividerWidth / spanCount;
    }

    private void setGridLayoutManagerOffsets(Rect outRect, View child, RecyclerView parent) {
        GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
        int gridSize = layoutManager.getSpanCount();
        int totalCount = parent.getAdapter().getItemCount();
        int spaceH = dividerWidth;
        int spaceV = dividerHeight;
        int edgeH = 0; // 网格两边的间距  没有设置暂时
        int position = parent.getChildAdapterPosition(child);
        int lastRow = (totalCount   + spanCount- 1) / gridSize-1;
        // 获取第几列
        int column = position % gridSize;
        // 第几行
        int row = position / gridSize;
        Logger.i("---------------","row="+row+",last row="+lastRow);
        if (row == 0 && direction != Direction.Middle && direction != Direction.Bottom) { // 设置top
            outRect.top = dividerHeightTop;
        } else if (row == lastRow && direction != Direction.Middle && direction != Direction.Top) {
            outRect.top=spaceV;
            outRect.bottom = dividerHeightBottom;
        } else if (row != 0) {
            outRect.top = spaceV;
        }
        // p为每个Item都需要减去的间距
        float p = (2 * edgeH + (gridSize - 1) * spaceH) * 1f / gridSize;
        float left = edgeH + column * (spaceH - p);
        float right = p - left;
        outRect.left = Math.round(left);
        outRect.right = Math.round(right);
    }
}
