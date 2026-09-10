package com.kuaigeglobal.epnova.widget.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.nhsd.basic.ext.CoreUtils;

/**
 * 线性列表间距，所有尺寸均为 px。默认纵向，每项顶部留白。
 * 横向的 start/end 对应左/右，纵向对应上/下；保持物理方向，不随 RTL 或 reverseLayout 翻转。
 * 网格和瀑布流请使用各自的 ItemDecoration。
 */
public class SpaceDecoration extends RecyclerView.ItemDecoration {
    public enum Direction {
        /** 仅相邻项之间留白，首项 start 不留白。 */
        MIDDLE,
        /** 每项 start 留白。 */
        START,
        /** 每项 end 留白。 */
        END,
        /** 首项 start 和每项 end 留白，项间距仍为一份 spacing。 */
        BOTH
    }

    private final int spacing;
    private final int orientation;
    private final Direction direction;

    public SpaceDecoration() {
        this(CoreUtils.getDp(8));
    }

    public SpaceDecoration(int spacing) {
        this(spacing, Direction.START);
    }

    public SpaceDecoration(int spacing, Direction direction) {
        this(spacing, RecyclerView.VERTICAL, direction);
    }

    public SpaceDecoration(int spacing, int orientation, Direction direction) {
        if (orientation != RecyclerView.HORIZONTAL && orientation != RecyclerView.VERTICAL) {
            throw new IllegalArgumentException("orientation must be HORIZONTAL or VERTICAL");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        this.spacing = spacing;
        this.orientation = orientation;
        this.direction = direction;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        int start = 0;
        int end = 0;
        switch (direction) {
            case MIDDLE:
                start = position == 0 ? 0 : spacing;
                break;
            case START:
                start = spacing;
                break;
            case END:
                end = spacing;
                break;
            case BOTH:
                start = position == 0 ? spacing : 0;
                end = spacing;
                break;
        }
        if (orientation == RecyclerView.HORIZONTAL) {
            outRect.left = start;
            outRect.right = end;
        } else {
            outRect.top = start;
            outRect.bottom = end;
        }
    }
}
