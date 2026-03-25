package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.kuaigeglobal.epnova.R;
import com.shizhefei.view.coolrefreshview.CoolRefreshView;
import com.shizhefei.view.coolrefreshview.PullHeader;

public class LottieHeader extends FrameLayout implements PullHeader {
        private View view;
        private PullHeader.DefaultConfig config = new PullHeader.DefaultConfig();

        public LottieHeader(Context context) {
            super(context);
        }

        public View createHeaderView(final CoolRefreshView refreshView) {
            this.view = LayoutInflater.from(refreshView.getContext()).inflate(R.layout.customized_refresh_header_view, refreshView, false);
            return this.view;
        }

        public PullHeader.Config getConfig() {
            return this.config;
        }

        public void onPullBegin(CoolRefreshView refreshView) {
//            this.mTextView.setText("onPullBegin :");
        }

        public void onPositionChange(CoolRefreshView refreshView, int status, int dy, int currentDistance) {
//            this.mTextView2.setText("onPositionChange status:" + status + " dy:" + dy + " currentDistance:" + currentDistance);
        }

        public void onRefreshing(CoolRefreshView refreshView) {
//            this.mTextView.setText("onRefreshing");
        }

        public void onReset(CoolRefreshView refreshView, boolean pullRelease) {
//            this.mTextView.setText("onReset pullRelease:" + pullRelease);
        }

        public void onPullRefreshComplete(CoolRefreshView frame) {
//            this.mTextView.setText("onPullRefreshComplete");
        }
    }

