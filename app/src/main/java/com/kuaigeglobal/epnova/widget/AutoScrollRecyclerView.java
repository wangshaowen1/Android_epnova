package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhsd.basic.ext.CoreUtils;


public class AutoScrollRecyclerView extends RecyclerView {
    private boolean started;

    private static final int TIME_INTERVAL = 10;//控制滚动的速度，值越大速度越慢
    private boolean reverse;

    private int moveDistance= CoreUtils.getDp(2f);

    public AutoScrollRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
 
    public AutoScrollRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
 
    private final Runnable runningTask=new Runnable(){
        @Override
        public void run() {
            if (started){
                scrollBy(reverse?-moveDistance:moveDistance,reverse?-moveDistance:moveDistance);
                postDelayed(this, TIME_INTERVAL);
            }
        }
    };
    //开始滚动
    public void start(){
        if (started)
            stop();
        if(getLayoutManager() instanceof  LinearLayoutManager){
             reverse=((LinearLayoutManager) getLayoutManager()).getReverseLayout();
        }
        started = true;
        postDelayed(runningTask, TIME_INTERVAL);
    }
    //停止滚动
    public void stop() {
        started = false;
        removeCallbacks(runningTask);
    }

}