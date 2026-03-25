package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.nhsd.basic.ext.CoreUtils;


public class SpaceDecoration extends RecyclerView.ItemDecoration {
    public static final int UNSPECIFIED = -1;
    private int spacing;
    private int rowCount = UNSPECIFIED;

    public enum Direction {
        MIDDLE, TOP, BOTTOM, TOP_WITH_BOTTOM
    }

    private Direction direction;

    public SpaceDecoration() {
        this(CoreUtils.getDp(8), UNSPECIFIED, Direction.TOP);
    }

    public SpaceDecoration(int spacing) {
        this(spacing, UNSPECIFIED, Direction.TOP);
    }

    public SpaceDecoration(int spacing, Direction direction) {
        this(spacing, UNSPECIFIED, direction);
    }

    public SpaceDecoration(int rowCount, int spacing) {
        this(spacing, rowCount, Direction.TOP);
    }

    public SpaceDecoration(int spacing, int rowCount, Direction direction) {
        this.spacing = spacing;
        this.rowCount = rowCount;
        this.direction = direction;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        if (position < rowCount || rowCount == UNSPECIFIED) {
            if (direction == Direction.TOP_WITH_BOTTOM) {
                if (position == 0) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else if (direction == Direction.TOP || direction == Direction.MIDDLE) {
                if (direction == Direction.MIDDLE && position == 0) {

                } else {
                    outRect.top = spacing;
                }
            } else if (direction == Direction.BOTTOM) {
                outRect.bottom = spacing;
            }
        }
    }
}