package com.kuaigeglobal.epnova.widget;

import android.view.View;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.nhsd.mvc.ILoadViewFactory;
import com.nhsd.mvc.IRefreshView;
import com.nhsd.mvc.MVCHelper;
import com.kuaigeglobal.epnova.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 注意 ：<br>
 * <2>SwipeRefreshLayout必须有Parent
 *
 * @param <DATA>
 * @author zsy
 */
public class MVCSwipeRefreshHelper<DATA> extends MVCHelper<DATA> {
    public interface OnRefreshListener {
        public void onRefresh();
    }

    public interface OnLoadMoreListener {
        public void onLoadMore();
    }

    private OnRefreshListener onRefreshListener;
    private OnLoadMoreListener onLoadMoreListener;

    public MVCSwipeRefreshHelper(SwipeRefreshLayout swipeRefreshLayout) {
        super(new RefreshView(swipeRefreshLayout));
    }

    public MVCSwipeRefreshHelper(SwipeRefreshLayout swipeRefreshLayout, ILoadViewFactory.ILoadView loadView, ILoadViewFactory.ILoadMoreView loadMoreView) {
        super(new RefreshView(swipeRefreshLayout), loadView, loadMoreView);
    }

    private static class RefreshView implements IRefreshView {
        private SwipeRefreshLayout swipeRefreshLayout;
        private View mTarget;

        public RefreshView(SwipeRefreshLayout swipeRefreshLayout) {
            this.swipeRefreshLayout = swipeRefreshLayout;
            swipeRefreshLayout.setColorSchemeResources(R.color.gplus_color_1, R.color.gplus_color_2, R.color.gplus_color_3, R.color.gplus_color_4);
            if (swipeRefreshLayout.getParent() == null) {
                throw new RuntimeException("PtrClassicFrameLayout 必须有Parent");
            }
            try {
                Method method = swipeRefreshLayout.getClass().getDeclaredMethod("ensureTarget");
                method.setAccessible(true);
                method.invoke(swipeRefreshLayout);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Field field = swipeRefreshLayout.getClass().getDeclaredField("mTarget");
                field.setAccessible(true);
                mTarget = (View) field.get(swipeRefreshLayout);
            } catch (Exception e) {
                e.printStackTrace();
            }
            swipeRefreshLayout.setOnRefreshListener(listener);
        }

        @Override
        public View getListView() {
            return mTarget;
        }

        @Override
        public View getSwitchView() {
            return swipeRefreshLayout;
        }

        private OnRefreshListener onRefreshListener;

        @Override
        public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
            this.onRefreshListener = onRefreshListener;
        }

        @Override
        public void showRefreshComplete() {
            swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void showRefreshing() {
            swipeRefreshLayout.setRefreshing(true);
        }

        private SwipeRefreshLayout.OnRefreshListener listener = new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                if (onRefreshListener != null) {
                    onRefreshListener.onRefresh();
                }
            }
        };

    }


    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (onRefreshListener != null) {
            onRefreshListener.onRefresh();
        }
    }

    @Override
    public void loadMore() {
        super.loadMore();
        if (onLoadMoreListener != null) {
            onLoadMoreListener.onLoadMore();
        }
    }
}
