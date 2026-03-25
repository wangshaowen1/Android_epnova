package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.nhsd.basic.ext.CoreUtils;


public class HorSpaceDecoration extends RecyclerView.ItemDecoration {
    public static final int UNSPECIFIED = -1;
    private int spacing;
    private int rowCount = UNSPECIFIED;

    public enum Direction {
        MIDDLE, LEFT, RIGHT, LEFT_WITH_RIGHT
    }

    private Direction direction;

    public HorSpaceDecoration() {
        this(CoreUtils.getDp(8), UNSPECIFIED, Direction.LEFT);
    }

    public HorSpaceDecoration(int spacing) {
        this(spacing, UNSPECIFIED, Direction.LEFT);
    }

    public HorSpaceDecoration(int spacing, Direction direction) {
        this(spacing, UNSPECIFIED, direction);
    }

    public HorSpaceDecoration(int rowCount, int spacing) {
        this(spacing, rowCount, Direction.LEFT);
    }

    public HorSpaceDecoration(int spacing, int rowCount, Direction direction) {
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
            if (direction == Direction.LEFT_WITH_RIGHT) {
                if (position == 0) {
                    outRect.left = spacing;
                }
                outRect.right = spacing;
            } else if (direction == Direction.LEFT || direction == Direction.MIDDLE) {
                if (direction == Direction.MIDDLE && position == 0) {

                } else {
                    outRect.left = spacing;
                }
            } else if (direction == Direction.RIGHT) {
                outRect.right = spacing;
            }
        }
    }
}