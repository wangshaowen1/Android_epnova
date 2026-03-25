package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;


import com.blankj.utilcode.util.SizeUtils;

import java.util.Objects;

/**
 * 不会导致item宽高不一致
 * 自适应任意列的 S 的分割块 ItemDecoration
 */
public class LinearLayoutManagerSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int spacingDp, firstSpacingDp, secondSpacingDp;
    private Orientation orientation;

    public enum Orientation {
        Horizontal, Vertical
    }

    /**
     * @param spacingDp 分割块宽,单位:dp
     */
    public LinearLayoutManagerSpacingDecoration(int spacingDp) {
        this.spacingDp = SizeUtils.dp2px(spacingDp);
        this.firstSpacingDp = this.spacingDp;
        this.secondSpacingDp = this.spacingDp;
        this.orientation = Orientation.Horizontal;
    }

    public LinearLayoutManagerSpacingDecoration(int spacingDp, Orientation orientation) {
        this.spacingDp = SizeUtils.dp2px(spacingDp);
        this.firstSpacingDp = this.spacingDp;
        this.secondSpacingDp = this.spacingDp;
        this.orientation = orientation;
    }

    public LinearLayoutManagerSpacingDecoration(int spacingDp, int firstSpacingDp, Orientation orientation) {
        this.spacingDp = SizeUtils.dp2px(spacingDp);
        this.firstSpacingDp = SizeUtils.dp2px(firstSpacingDp);
        this.secondSpacingDp = this.spacingDp;
        this.orientation = orientation;
    }

    public LinearLayoutManagerSpacingDecoration(int spacingDp, int firstSpacingDp, int secondSpacingDp, Orientation orientation) {
        this.spacingDp = SizeUtils.dp2px(spacingDp);
        this.firstSpacingDp = SizeUtils.dp2px(firstSpacingDp);
        this.secondSpacingDp = SizeUtils.dp2px(secondSpacingDp);
        this.orientation = orientation;
    }

    @Override
    public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, child, parent, state);

        int pos = parent.getChildAdapterPosition(child);

        if (orientation == Orientation.Horizontal) {
            if (pos == 0) {
                outRect.left = firstSpacingDp;
                outRect.right = secondSpacingDp;
            } else {
                outRect.right = spacingDp;
            }
        } else {
            if (pos == 0) {
                outRect.top = firstSpacingDp;
                outRect.bottom = secondSpacingDp;
            } else {
                outRect.bottom = spacingDp;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinearLayoutManagerSpacingDecoration that = (LinearLayoutManagerSpacingDecoration) o;
        return spacingDp == that.spacingDp && firstSpacingDp == that.firstSpacingDp && secondSpacingDp == that.secondSpacingDp && orientation == that.orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(spacingDp, firstSpacingDp, secondSpacingDp, orientation);
    }
}
