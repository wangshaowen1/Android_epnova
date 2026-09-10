package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SizeUtils;

/**
 * 只给列表边缘的若干项留白，不改变其它项的间距。尺寸为 px，默认 12dp。
 * LEFT/TOP 作用于前 itemCount 项，RIGHT/BOTTOM 作用于后 itemCount 项。
 * 按整个 Adapter 的位置计算（包括 ConcatAdapter 的头尾项），不推断网格行或可见项。
 * 四个方向均为物理方向，不随 RTL 或 reverseLayout 翻转。
 */
public class EdgeSpaceDecoration extends RecyclerView.ItemDecoration {
    public enum Edge {
        LEFT, TOP, RIGHT, BOTTOM
    }

    private final int spacing;
    private final int itemCount;
    private final Edge edge;

    public EdgeSpaceDecoration(Edge edge) {
        this(SizeUtils.dp2px(12), edge);
    }

    public EdgeSpaceDecoration(int spacing, Edge edge) {
        this(spacing, 1, edge);
    }

    public EdgeSpaceDecoration(int spacing, int itemCount, Edge edge) {
        if (itemCount < 1) {
            throw new IllegalArgumentException("itemCount must be positive");
        }
        if (edge == null) {
            throw new IllegalArgumentException("edge must not be null");
        }
        this.spacing = spacing;
        this.itemCount = itemCount;
        this.edge = edge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
        int position = parent.getChildAdapterPosition(view);
        RecyclerView.Adapter<?> adapter = parent.getAdapter();
        if (position == RecyclerView.NO_POSITION || adapter == null) {
            return;
        }
        int totalCount = adapter.getItemCount();
        if (position >= totalCount) {
            return;
        }
        boolean leading = edge == Edge.LEFT || edge == Edge.TOP;
        if (leading ? position >= itemCount : position < totalCount - itemCount) {
            return;
        }
        switch (edge) {
            case LEFT:
                outRect.left = spacing;
                break;
            case TOP:
                outRect.top = spacing;
                break;
            case RIGHT:
                outRect.right = spacing;
                break;
            case BOTTOM:
                outRect.bottom = spacing;
                break;
        }
    }
}
