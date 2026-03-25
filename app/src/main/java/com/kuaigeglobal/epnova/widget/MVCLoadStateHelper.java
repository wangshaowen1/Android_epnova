package com.kuaigeglobal.epnova.widget;

import android.view.View;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.nhsd.mvc.ILoadViewFactory;
import com.nhsd.mvc.IRefreshView;
import com.nhsd.mvc.MVCHelper;
import com.kuaigeglobal.epnova.R;
import com.shizhefei.view.coolrefreshview.CoolRefreshView;
import com.shizhefei.view.coolrefreshview.OnPullListener;
import com.shizhefei.view.coolrefreshview.SimpleOnPullListener;

/**
 * 注意 ：<br>
 * LoadStateViewHolder
 *支持父布局CoolRefreshView，SwipeRefreshLayout，及任意其它布局。
 * @param <DATA>
 * @author zsy
 */
public class MVCLoadStateHelper<DATA> extends MVCHelper<DATA> {

    public MVCLoadStateHelper(LoadStateViewHolder loadStateViewHolder) {
        super(new RefreshView(loadStateViewHolder));
    }

    public MVCLoadStateHelper(LoadStateViewHolder coolRefreshView, ILoadViewFactory.ILoadView loadView, ILoadViewFactory.ILoadMoreView loadMoreView) {
        super(new RefreshView(coolRefreshView), loadView, loadMoreView);
    }

    private static class RefreshView implements IRefreshView {
        private SwipeRefreshLayout swipeRefreshLayout;
        private LoadStateViewHolder loadStateViewHolder;
        private CoolRefreshView coolRefreshView;

        public RefreshView(LoadStateViewHolder loadStateViewHolder) {
            this.loadStateViewHolder = loadStateViewHolder;
            if (loadStateViewHolder.getParent() instanceof CoolRefreshView) {
                coolRefreshView = ((CoolRefreshView) loadStateViewHolder.getParent());
                coolRefreshView.addOnPullListener(onPullListener);
            } else if (loadStateViewHolder.getContent() instanceof SwipeRefreshLayout) {
                swipeRefreshLayout.setColorSchemeResources(R.color.gplus_color_1, R.color.gplus_color_2, R.color.gplus_color_3, R.color.gplus_color_4);
                swipeRefreshLayout.setOnRefreshListener(refreshListener);
            }
        }

        @Override
        public View getListView() {
            return loadStateViewHolder.getContent();
        }

        @Override
        public View getSwitchView() {
            return loadStateViewHolder;
        }

        private OnRefreshListener onRefreshListener;

        @Override
        public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
            this.onRefreshListener = onRefreshListener;
        }

        @Override
        public void showRefreshComplete() {
            if (coolRefreshView != null) {
                coolRefreshView.removeOnPullListener(onPullListener);
                coolRefreshView.setRefreshing(false);
                coolRefreshView.addOnPullListener(onPullListener);
            }
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);

            }
        }

        @Override
        public void showRefreshing() {
            if (coolRefreshView != null) {
                coolRefreshView.removeOnPullListener(onPullListener);
                coolRefreshView.setRefreshing(true);
                coolRefreshView.addOnPullListener(onPullListener);
            }
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
        }

        private OnPullListener onPullListener = new SimpleOnPullListener() {
            @Override
            public void onRefreshing(CoolRefreshView coolRefreshView) {
                if (onRefreshListener != null) {
                    onRefreshListener.onRefresh();
                }
            }
        };


        private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (onRefreshListener != null) {
                    onRefreshListener.onRefresh();
                }
            }
        };
    }
}