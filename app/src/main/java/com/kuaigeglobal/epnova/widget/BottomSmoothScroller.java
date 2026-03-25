package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.recyclerview.widget.LinearSmoothScroller;

public class BottomSmoothScroller extends LinearSmoothScroller {
    private float speed;
    public BottomSmoothScroller(Context context,float speed) {
        super(context);
        this.speed=speed;
    }

    @Override
    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
        // 关键逻辑：强制将目标 item 的底部对齐到 RecyclerView 底部
        return boxEnd - viewEnd;
    }

    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics metrics) {
        return speed; // 调整滚动速度（值越小越快）
    }
}