package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SizeUtils;

import java.util.Objects;

/**
 * 可单独配置首项两侧的线性列表间距，构造参数均为 dp，默认横向。
 * 首项 start 使用 firstSpacingDp，首项 end 使用 secondSpacingDp；其它项仅 end 留白 spacingDp。
 * secondSpacingDp 是首项的右/下间距，不是列表末尾间距。
 * start/end 按物理左/右或上/下计算，不随 RTL 或 reverseLayout 翻转。
 * 普通等间距使用 SpaceDecoration；仅首尾若干项留白使用 EdgeSpaceDecoration。
 */
public class LinearLayoutManagerSpacingDecoration extends RecyclerView.ItemDecoration {
    private final int spacing;
    private final int firstSpacing;
    private final int secondSpacing;
    private final Orientation orientation;

    public enum Orientation {
        Horizontal, Vertical
    }

    public LinearLayoutManagerSpacingDecoration(int spacingDp) {
        this(spacingDp, Orientation.Horizontal);
    }

    public LinearLayoutManagerSpacingDecoration(int spacingDp, Orientation orientation) {
        this(spacingDp, spacingDp, spacingDp, orientation);
    }

    public LinearLayoutManagerSpacingDecoration(int spacingDp, int firstSpacingDp, Orientation orientation) {
        this(spacingDp, firstSpacingDp, spacingDp, orientation);
    }

    public LinearLayoutManagerSpacingDecoration(int spacingDp, int firstSpacingDp, int secondSpacingDp,
                                               Orientation orientation) {
        if (orientation == null) {
            throw new IllegalArgumentException("orientation must not be null");
        }
        this.spacing = SizeUtils.dp2px(spacingDp);
        this.firstSpacing = SizeUtils.dp2px(firstSpacingDp);
        this.secondSpacing = SizeUtils.dp2px(secondSpacingDp);
        this.orientation = orientation;
    }

    @Override
    public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
        int position = parent.getChildAdapterPosition(child);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        int start = position == 0 ? firstSpacing : 0;
        int end = position == 0 ? secondSpacing : spacing;
        if (orientation == Orientation.Horizontal) {
            outRect.left = start;
            outRect.right = end;
        } else {
            outRect.top = start;
            outRect.bottom = end;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinearLayoutManagerSpacingDecoration that = (LinearLayoutManagerSpacingDecoration) o;
        return spacing == that.spacing && firstSpacing == that.firstSpacing
                && secondSpacing == that.secondSpacing && orientation == that.orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(spacing, firstSpacing, secondSpacing, orientation);
    }
}
