package com.kuaigeglobal.epnova.widget;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.nhsd.mvc.ILoadViewFactory;
import com.nhsd.mvc.IRefreshView;
import com.nhsd.mvc.MVCHelper;

/**
 * 注意 ：<br>
 * <2>RecycleView必须有Parent
 * 不需要下拉刷新
 *
 * @param <DATA>
 * @author zsy
 */
public class MVCRecycleViewHelper<DATA> extends MVCHelper<DATA> {
    public MVCRecycleViewHelper(RecyclerView recyclerView) {
        super(new RefreshView(recyclerView));
    }

    private IRefreshView.OnRefreshListener onRefreshListener;

    public MVCRecycleViewHelper(RecyclerView recyclerView, ILoadViewFactory.ILoadView loadView, ILoadViewFactory.ILoadMoreView loadMoreView) {
        super(new RefreshView(recyclerView), loadView, loadMoreView);
    }

    private static class RefreshView implements IRefreshView {
        private View mTarget;

        public RefreshView(RecyclerView recyclerView) {
            if (recyclerView.getParent() == null) {
                throw new RuntimeException("recyclerView 必须有Parent");
            }

            mTarget = recyclerView;
        }

        @Override
        public View getListView() {
            return mTarget;
        }

        @Override
        public View getSwitchView() {
            return mTarget;
        }


        @Override
        public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
//			this.onRefreshListener = onRefreshListener;
        }

        @Override
        public void showRefreshComplete() {
//			swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void showRefreshing() {
//			swipeRefreshLayout.setRefreshing(true);
        }


    }

    public void setOnRefreshListener(IRefreshView.OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (onRefreshListener != null) {
            onRefreshListener.onRefresh();
        }
    }

}
