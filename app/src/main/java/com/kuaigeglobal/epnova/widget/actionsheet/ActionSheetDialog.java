package com.kuaigeglobal.epnova.widget.actionsheet;

import android.view.View;
import android.widget.AdapterView;

import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.databinding.LayoutActionsheetBinding;
import com.nhsd.basic.BaseBottomPushDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 精仿iOSAlertViewController控件
 * 点击取消按钮返回 －1，其他按钮从0开始算
 */
public class ActionSheetDialog extends BaseBottomPushDialogFragment<LayoutActionsheetBinding> {
    private Builder builder;


    public static final int CANCEL_POSITION = -1;//点击取消按钮返回 －1，其他按钮从0开始算

    private String title;
    private String msg;
    private List<String> mDestructive;
    private List<String> mOthers;
    private String cancel;
    private ArrayList<String> mDatas = new ArrayList<String>();


    private OnItemClickListener onItemClickListener;
    private OnHeaderClickListener onHeaderClickListener;

    public ActionSheetDialog(Builder builder) {
        this.builder = builder;
    }


    @Override
    public void onPrepareView() {
        super.onPrepareView();
        initData(builder.title, builder.msg, builder.cancel, builder.destructive, builder.others);
        this.onItemClickListener = builder.onItemClickListener;
        this.onHeaderClickListener = builder.onHeaderClickListener;
        initEvents();
        initActionSheetViews();
        LayoutActionsheetBinding binding = getBindingNullable();
        if (binding != null && cancel != null) {
            binding.tvAlertCancel.setVisibility(View.VISIBLE);
            binding.tvAlertCancel.setText(cancel);
        }
    }

    /**
     * 获取数据
     */
    protected void initData(String title, String msg, String cancel, String[] destructive, String[] others) {

        this.title = title;
        this.msg = msg;
        if (destructive != null) {
            this.mDestructive = Arrays.asList(destructive);
            this.mDatas.addAll(mDestructive);
        }
        if (others != null) {
            this.mOthers = Arrays.asList(others);
            this.mDatas.addAll(mOthers);
        }
        if (cancel != null) {
            this.cancel = cancel;

        }

    }


    protected void initHeaderView() {
        LayoutActionsheetBinding binding = getBindingNullable();
        if (binding == null) return;
        if (title != null) {
            binding.tvAlertTitle.setText(title);
            binding.tvAlertTitle.setVisibility(View.VISIBLE);
        } else {
            binding.tvAlertTitle.setVisibility(View.GONE);
        }
        if (msg != null) {
            binding.tvAlertMsg.setText(msg);
            binding.tvAlertMsg.setVisibility(View.VISIBLE);
        } else {
            binding.tvAlertMsg.setVisibility(View.GONE);
        }
        if (mOthers != null && mOthers.size() > 0) {
            binding.loAlertHeader.setBackgroundResource(R.drawable.bg_actionsheet_item_top);
        }
    }

    protected void initListView() {
        ActionSheetAdapter adapter = new ActionSheetAdapter(mDatas, mDestructive, title != null);
        LayoutActionsheetBinding binding = getBindingNullable();
        if (binding == null) return;
        binding.alertButtonListView.setAdapter(adapter);
        binding.alertButtonListView.setOnItemClickListener((adapterView, view, position, l) -> {
            if (onItemClickListener != null)
                onItemClickListener.onItemClick(ActionSheetDialog.this, position);
            dismissAllowingStateLoss();
        });
        if (onHeaderClickListener != null) {
            binding.loAlertHeader.setOnClickListener(v -> {
                onHeaderClickListener.onHeaderClick(ActionSheetDialog.this);
                dismissAllowingStateLoss();
            });
        }
    }

    protected void initActionSheetViews() {
        initHeaderView();
        initListView();
    }


    protected void initEvents() {
        LayoutActionsheetBinding binding = getBindingNullable();
        if (binding == null) return;
        binding.tvAlertCancel.setOnClickListener(new OnTextClickListener(CANCEL_POSITION));
    }

    public ActionSheetDialog addExtView(View extView) {
        LayoutActionsheetBinding binding = getBindingNullable();
        if (binding == null) return this;
        binding.loAlertHeader.addView(extView);
        return this;
    }

    class OnTextClickListener implements View.OnClickListener {

        private int position;

        public OnTextClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            if (onItemClickListener != null)
                onItemClickListener.onItemClick(ActionSheetDialog.this, position);
            dismissAllowingStateLoss();
        }
    }


    /**
     * Builder for arguments
     */
    public static class Builder {
        private String title;

        private String msg;
        private String cancel;
        private String[] destructive;
        private String[] others;
        private OnItemClickListener onItemClickListener;
        private OnHeaderClickListener onHeaderClickListener;

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(String msg) {
            this.msg = msg;
            return this;
        }

        public Builder setCancelText(String cancel) {
            this.cancel = cancel;
            return this;
        }

        public Builder setDestructive(String... destructive) {
            this.destructive = destructive;
            return this;
        }

        public Builder setOthers(String[] others) {
            this.others = others;
            return this;
        }

        public Builder setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
            return this;
        }

        public Builder setOnHeaderClickListener(OnHeaderClickListener onHeaderClickListener) {
            this.onHeaderClickListener = onHeaderClickListener;
            return this;
        }

        public ActionSheetDialog build() {
            return new ActionSheetDialog(this);
        }
    }
}
