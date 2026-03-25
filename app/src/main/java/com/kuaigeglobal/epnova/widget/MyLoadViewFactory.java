package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.fragment.app.FragmentActivity;

import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.core.ext.ErrorUtils;
import com.kuaigeglobal.epnova.core.network.apis.ApiException;
import com.kuaigeglobal.epnova.databinding.LayoutTrailingLoadMoreBinding;
import com.nhsd.basic.ext.CoreUtils;
import com.nhsd.mvc.ILoadViewFactory;


public class MyLoadViewFactory implements ILoadViewFactory {

    @Override
    public ILoadMoreView madeLoadMoreView() {
        return new LoadMoreHelper();
    }

    @Override
    public ILoadView madeLoadView() {
        return new LoadViewHelper();
    }

    public static class LoadMoreHelper implements ILoadMoreView {

        protected LayoutTrailingLoadMoreBinding footBinding;

        protected OnClickListener onClickRefreshListener;
        private int textColor = 0;
        private int footViewVisibility = View.VISIBLE;
        private int singlePageVisibility = View.GONE;
        private boolean showNoMore = true;
        private String noMoreText ="已经到底了...";
        private int footViewPadding;
//        private boolean showNormal;


        public void setSinglePageVisibility(int singlePageVisibility) {
            this.singlePageVisibility = singlePageVisibility;
        }


        @Override
        public void init(FootViewAdder footViewHolder, OnClickListener onClickRefreshListener) {
            footBinding = LayoutTrailingLoadMoreBinding.bind(footViewHolder.addFootView(R.layout.layout_trailing_load_more));
//            if (textColor != 0) {
//                footView.setTextColor(textColor);
//            }
            if (footViewPadding != 0) {
                footBinding.getRoot().setPadding(footBinding.getRoot().getPaddingLeft(), footBinding.getRoot().getPaddingTop(), footBinding.getRoot().getPaddingRight(), footViewPadding);
            }

            footBinding.getRoot().setVisibility(footViewVisibility);
            this.onClickRefreshListener = onClickRefreshListener;
            hideLoadMore();
        }

        public void setFootViewPadding(int footViewPadding) {
            this.footViewPadding = footViewPadding;
        }

        @Override
        public void showNormal() {
            hideLoadMore();
//            showNormal=true;
//            App.getInstance().postDelay(() -> {
//                if(!hasVisible()&&showNormal) {
//                    footBinding.loadMoreLoadCompleteView.setVisibility(View.VISIBLE);
//                }
//            },200);
        }



        @Override
        public void showLoading() {
//            showNormal=false;
            hideLoadMore();
            footBinding.loadMoreLoadingView.setVisibility(View.VISIBLE);

        }

        @Override
        public void showFailed(Throwable exception) {
//            showNormal=false;
            if (exception instanceof ApiException) {
                if (((ApiException) exception).getErrorCode() == 110) {
                    showError(footBinding.getRoot().getContext(), exception);
                }
            }
            hideLoadMore();
            footBinding.loadMoreLoadFailSmallView.setVisibility(View.VISIBLE);
            footBinding.loadMoreLoadFailSmallView.setOnClickListener(onClickRefreshListener);
        }

        public void setVisibility(int visibility) {
            if (footBinding != null) {
                footBinding.getRoot().setVisibility(visibility);
            }
            this.footViewVisibility = visibility;
        }

        public void setShowNoMore(boolean isShow) {
            showNoMore = isShow;
        }


        @Override
        public void showNomore(int pageCount) {
//            showNormal=false;
            hideLoadMore();
            footBinding.loadMoreLoadEndViewText.setText(noMoreText);
            if (showNoMore && pageCount > 2) {
                footBinding.loadMoreLoadEndView.setVisibility(View.VISIBLE);
            } else {
                footBinding.loadMoreLoadEndView.setVisibility(View.GONE);
            }

        }

        private void hideLoadMore(){
//            load_more_load_empty_view
//                load_more_load_fail_view
            footBinding.loadMoreLoadEmptyView.setVisibility(View.GONE);
            footBinding.loadMoreLoadFailView.setVisibility(View.GONE);
            footBinding.loadMoreLoadingView.setVisibility(View.INVISIBLE);
            footBinding.loadMoreLoadCompleteView.setVisibility(View.GONE);
            footBinding.loadMoreLoadEndView.setVisibility(View.GONE);
            footBinding.loadMoreLoadFailSmallView.setVisibility(View.GONE);
        }
        private boolean hasVisible() {
            if(footBinding.loadMoreLoadEmptyView.getVisibility()==View.VISIBLE){
                return true;
            }
            if(footBinding.loadMoreLoadFailView.getVisibility()==View.VISIBLE){
                return true;
            }
            if(footBinding.loadMoreLoadingView.getVisibility()==View.VISIBLE){
                return true;
            }
            if(footBinding.loadMoreLoadCompleteView.getVisibility()==View.VISIBLE){
                return true;
            }
            if(footBinding.loadMoreLoadEndView.getVisibility()==View.VISIBLE){
                return true;
            }
            if(footBinding.loadMoreLoadFailSmallView.getVisibility()==View.VISIBLE){
                return true;
            }
            return false;
        }
        public void setNoMoreText(String noMoreText) {
            this.noMoreText = noMoreText;
        }
    }

    public static class LoadViewHelper implements ILoadView {
        //private VaryViewHelper helper;
        private LoadStateViewHolder holder;
        private Context context;
        private View emptyView;
        private CharSequence emptyText="暂无内容";
        private int emptyResId;
        private Integer gravity =null;
        private int topPadding = 0;
        private View retryView;


        public void setGravity(int gravity) {
            setGravity(gravity, CoreUtils.getDp(10));
        }

        public void setGravity(int gravity, int topPadding) {
            this.gravity = gravity;
            this.topPadding = topPadding;
            if(holder!=null){
                holder.setGravity(gravity, topPadding);
            }
        }

        public void setVisibility(int visibility) {
            if (holder != null) {
                holder.setVisibility(visibility);
            }
        }

        @Override
        public void init(View switchView, OnClickListener onClickRefreshListener) {
            this.context = switchView.getContext();
            if(switchView instanceof LoadStateViewHolder) {
                holder = (LoadStateViewHolder) switchView;
            }else {
                holder = new LoadStateViewHolder(context);
                holder.setContent(switchView);
            }
            if (emptyView != null) {
                holder.setEmptyView(emptyView);
            }
            if (retryView != null) {
                holder.setRetryView(retryView);
            }
            if (emptyText != null) {
                holder.setNoDataPrompt(emptyText);
            }
            if(gravity!=null) {
                holder.setGravity(gravity, topPadding);
            }
            if (emptyResId > 0) {
                holder.setDefaultEmptyImage(emptyResId);
            }
            holder.setRetryListener(onClickRefreshListener);
        }

        @Override
        public void showContent() {
            holder.showContent();
        }

        @Override
        public void showLoading() {
            holder.showLoading();
        }

        @Override
        public void onLoadMoreError(Throwable exception) {
            //Toast.makeText(context, "网络加载失败", Toast.LENGTH_SHORT).show();
        }


        @Override
        public void showFailed(Throwable exception) {
            holder.showRetry(exception);
            if (exception instanceof ApiException) {
                if (((ApiException) exception).getErrorCode() == ApiException.USER_ADD_TO_BLACK_LIST) {
                    showError(context, exception);
                }
            }
        }
        public void setRetryView(View retryView) {
            if (holder != null) {
                holder.setRetryView(retryView);
            }
            this.retryView = retryView;
        }

        @Override
        public void showEmpty() {
            holder.showEmpty();
        }

        public void setEmptyView(View emptyView) {
            if (holder != null) {
                holder.setEmptyView(emptyView);
            }
            this.emptyView = emptyView;
        }

        public void setEmptyText(CharSequence emptyText) {
            if (holder != null) {
                holder.setNoDataPrompt(emptyText);
            }
            this.emptyText = emptyText;
        }

        public void setEmptyIcon(int resId) {
            if (holder != null) {
                holder.setDefaultEmptyImage(resId);
            }
            this.emptyResId = resId;
        }


        public void setOnClickListener(OnClickListener l) {
            holder.setOnClickListener(l);
        }
    }

    private static void showError(Context context, Throwable ex) {
        if (context instanceof FragmentActivity) {
            ErrorUtils.showError((FragmentActivity) context, ex);
        }
    }
}
