package com.kuaigeglobal.epnova.widget;

import android.view.View;

import com.nhsd.mvc.ILoadViewFactory;
import com.nhsd.mvc.IRefreshView;
import com.nhsd.mvc.MVCHelper;
import com.shizhefei.view.coolrefreshview.CoolRefreshView;
import com.shizhefei.view.coolrefreshview.OnPullListener;
import com.shizhefei.view.coolrefreshview.SimpleOnPullListener;

/**
 * 注意 ：<br>
 * CoolRefreshView在 xml布局外面必须有一层layout
 *
 * @param <DATA>
 * @author zsy
 */
public class MVCCoolHelper<DATA> extends MVCHelper<DATA> {

    public MVCCoolHelper(CoolRefreshView coolRefreshView) {
        super(new RefreshView(coolRefreshView));
    }

    public MVCCoolHelper(CoolRefreshView coolRefreshView, ILoadViewFactory.ILoadView loadView, ILoadViewFactory.ILoadMoreView loadMoreView) {
        super(new RefreshView(coolRefreshView), loadView, loadMoreView);
    }

    private static class RefreshView implements IRefreshView {
        private CoolRefreshView coolRefreshView;

        public RefreshView(CoolRefreshView coolRefreshView) {
            this.coolRefreshView = coolRefreshView;
            coolRefreshView.addOnPullListener(onPullListener);
        }

        @Override
        public View getListView() {
            return coolRefreshView.getContentView();
        }

        @Override
        public View getSwitchView() {
            return coolRefreshView;
        }

        private OnRefreshListener onRefreshListener;

        @Override
        public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
            this.onRefreshListener = onRefreshListener;
        }

        @Override
        public void showRefreshComplete() {
            coolRefreshView.removeOnPullListener(onPullListener);
            coolRefreshView.setRefreshing(false);
            coolRefreshView.addOnPullListener(onPullListener);
        }

        @Override
        public void showRefreshing() {
            coolRefreshView.removeOnPullListener(onPullListener);
            coolRefreshView.setRefreshing(true);
            coolRefreshView.addOnPullListener(onPullListener);
        }

        private OnPullListener onPullListener = new SimpleOnPullListener() {
            @Override
            public void onRefreshing(CoolRefreshView coolRefreshView) {
                if (onRefreshListener != null) {
                    onRefreshListener.onRefresh();
                }
            }
        };
    }
}