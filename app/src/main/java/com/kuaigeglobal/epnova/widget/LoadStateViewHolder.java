package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ScreenUtils;
import com.kuaigeglobal.epnova.common.utils.NetworkUtil;
import com.nhsd.basic.utils.Logger;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.core.network.apis.ApiException;
import com.kuaigeglobal.epnova.databinding.ContentViewHolderBinding;
import com.kuaigeglobal.epnova.widget.expandtextview.OverLinkMovementMethod;


public class LoadStateViewHolder extends ViewAnimator {

    public static final int LOADING = 0;
    public static final int RETRY = 1;
    public static final int NO_DATA = 2;
    public static final int CONTENT = 3;
    private boolean retryInNoData = false;
    private static final int CHILD_SIZE = 4;
    private ContentViewHolderBinding binding;

    //网络异常提示内容
    private String networkAnomaly;
    //请求异常提示内容
    private String requestException;
    //无数据提示内容
    private String noData;
    //网络异常提示图片,无网络
    private int networkAnomalySrc;
    //请求异常提示图片，服务器异常
    private int requestExceptionSrc;
    //无数据提示图片
    private int noDataSrc;

    public LoadStateViewHolder(Context context) {
        super(context);
        initView();
    }

    //private ViewCo
    public LoadStateViewHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        initAttrs(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            setDisplayedChild(CONTENT);
        }
    }

    public void showNoData() {
        if (retryInNoData) {
            binding.errorPromptView.setText(noData);
            setDisplayedChild(RETRY);
        } else {
            setDisplayedChild(NO_DATA);
        }
    }

    public ContentViewHolderBinding getBinding() {
        return binding;
    }

    public View getContent() {
        return getChildAt(CONTENT);

    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadStateViewHolder);
        //网络异常提示内容
        networkAnomaly = typedArray.getString(R.styleable.LoadStateViewHolder_networkAnomaly);
        //请求异常提示内容
        requestException = typedArray.getString(R.styleable.LoadStateViewHolder_requestException);
        //无数据提示内容
        noData = typedArray.getString(R.styleable.LoadStateViewHolder_noData);
        if (TextUtils.isEmpty(noData)) {
            noData = context.getString(R.string.page_empty);
        } else {
            binding.noData.setText(noData);
        }
        //网络异常提示图片,无网络
        networkAnomalySrc = typedArray.getResourceId(
                R.styleable.LoadStateViewHolder_requestExceptionSrc,
                R.drawable.empty_default_network
        );
        //请求异常提示图片，服务器异常
        requestExceptionSrc = typedArray.getResourceId(
                R.styleable.LoadStateViewHolder_requestExceptionSrc,
                R.drawable.ic_not_song
        );
        //无数据提示图片
        noDataSrc = typedArray.getResourceId(R.styleable.LoadStateViewHolder_noDataSrc, R.drawable.ic_not_song);
        if (noDataSrc > 0) {
            binding.noDataIcon.setImageResource(noDataSrc);
        }
        boolean scrollable = typedArray.getBoolean(R.styleable.LoadStateViewHolder_scrollable, false);
        if (scrollable) {
           makeScrollable();
        }
        boolean lightMode = typedArray.getBoolean(R.styleable.LoadStateViewHolder_lightMode, false);
        if (lightMode) {
//            binding.loadingView.setAnimation(R.raw.lottie_loading);
            binding.loadingText.setTextColor(getResources().getColor(R.color.color_30_000000));
            binding.noDataIcon.setAlpha(.5f);
            binding.icon.setAlpha(.5f);
            binding.errorPromptView.setTextColor(getResources().getColor(R.color.color_30_000000));
            binding.noData.setTextColor(getResources().getColor(R.color.color_30_000000));
            binding.retryBtn.setTextColor(getResources().getColor(R.color.color_30_000000));
            binding.retryBtn.setBackgroundResource(R.drawable.bg_empty_button_light);
            binding.noDataBtn.setTextColor(getResources().getColor(R.color.color_30_000000));
            binding.noDataBtn.setBackgroundResource(R.drawable.bg_empty_button_light);
        }
        int stateViewTopPadding = typedArray.getDimensionPixelSize(R.styleable.LoadStateViewHolder_stateViewTopPadding, -1);
        if (stateViewTopPadding >= 0) {
            setStateViewTopPadding(stateViewTopPadding);
        }
        typedArray.recycle();
        if (isInEditMode()) {
            setDisplayedChild(CONTENT);
        }
    }

    public void makeScrollable() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!(child instanceof NestedScrollView) && !(child instanceof RecyclerView) && !(child instanceof ListView)) {
                removeView(child);
                NestedScrollView nestedScrollView = new NestedScrollView(getContext());
                child.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                child.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                child.setMinimumHeight(ScreenUtils.getScreenWidth());
                nestedScrollView.addView(child);
                addView(nestedScrollView, i, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
    }


    protected void initView() {
        binding = inflateLayout();
        setMeasureAllChildren(false);
    }

    public void setGravityHorizontalTop(int topPadding) {
        int gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        setGravity(gravity, topPadding);
    }

    public void setStateViewTopPadding(int topPadding) {
        int gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        binding.loadingLayout.setGravity(gravity);
        binding.retryLayout.setGravity(gravity);
        binding.emptyLayout.setGravity(gravity);
        setChildGravity(binding.loadingLayout, gravity);
        setChildGravity(binding.retryLayout, gravity);
        setChildGravity(binding.emptyLayout, gravity);
        setFixedTopSpace(binding.loadingTopSpace, topPadding);
        setFixedTopSpace(binding.retryTopSpace, topPadding);
        setFixedTopSpace(binding.emptyTopSpace, topPadding);
    }

    public void setGravity(int gravity, int topPadding) {
        binding.loadingLayout.setGravity(gravity);
        binding.retryLayout.setGravity(gravity);
        binding.emptyLayout.setGravity(gravity);
        LayoutParams ladingLayoutParams = (LayoutParams) binding.loadingLayout.getLayoutParams();
        ladingLayoutParams.gravity = gravity;
        binding.loadingLayout.setPadding(binding.loadingLayout.getPaddingLeft(), topPadding, binding.loadingLayout.getPaddingRight(), binding.loadingLayout.getPaddingBottom());
        LayoutParams retryLayoutParams = (LayoutParams) binding.retryLayout.getLayoutParams();
        retryLayoutParams.gravity = gravity;
        binding.retryLayout.setPadding(binding.loadingLayout.getPaddingLeft(), topPadding, binding.loadingLayout.getPaddingRight(), binding.loadingLayout.getPaddingBottom());
        LayoutParams emptyLayoutParams = (LayoutParams) binding.emptyLayout.getLayoutParams();
        emptyLayoutParams.gravity = gravity;
        binding.emptyLayout.setPadding(binding.loadingLayout.getPaddingLeft(), topPadding, binding.loadingLayout.getPaddingRight(), binding.loadingLayout.getPaddingBottom());
    }

    private void setChildGravity(View child, int gravity) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        layoutParams.gravity = gravity;
        child.setLayoutParams(layoutParams);
        child.setPadding(child.getPaddingLeft(), 0, child.getPaddingRight(), child.getPaddingBottom());
    }

    private void setFixedTopSpace(View topSpace, int topPadding) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) topSpace.getLayoutParams();
        layoutParams.height = topPadding;
        layoutParams.weight = 0f;
        topSpace.setLayoutParams(layoutParams);
    }

    public void setContent(View content) {
        if (getChildCount() == CHILD_SIZE) {
            removeViewAt(CONTENT);
        }
        ViewGroup parent = (ViewGroup) content.getParent();
        ViewGroup.LayoutParams contentLayoutParams = content.getLayoutParams();
        int index = parent.indexOfChild(content);
        parent.removeView(content);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(content, CONTENT, params);
        parent.addView(this, index, contentLayoutParams);
    }

    public void setEmptyView(View emptyView) {
        removeViewAt(NO_DATA);
        if (emptyView.getParent() != null) {
            throw new IllegalArgumentException("child already has parent");
        }
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(emptyView, NO_DATA, params);
    }

    public void setRetryView(View retryView) {
        removeViewAt(RETRY);
        if (retryView.getParent() != null) {
            throw new IllegalArgumentException("child already has parent");
        }
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(retryView, RETRY, params);
    }

    protected ContentViewHolderBinding inflateLayout() {
        return ContentViewHolderBinding.inflate(LayoutInflater.from(getContext()), this);
    }

    public void showContent() {
        setDisplayedChild(CONTENT);
    }

    public void showLoading() {
        setDisplayedChild(LOADING);
    }

    public void showRetry(Throwable e) {
        setDisplayedChild(RETRY);
        //异常提示文本
        String exceptionText;
        //异常提示图片
        int exceptionPicture;
        if (NetworkUtil.isNetworkAvailable()) {
            if (e instanceof ApiException) {
                exceptionText = e.getMessage();
            } else {
                if (requestException == null || requestException.isEmpty()) {
                    requestException = getContext().getString(R.string.page_load_error);
                }
                exceptionText = requestException;
            }
            if (requestExceptionSrc == 0) {
                requestExceptionSrc = R.drawable.empty_load_error;
            }
            exceptionPicture = requestExceptionSrc;
        } else {
            if (networkAnomaly == null || networkAnomaly.isEmpty()) {
                networkAnomaly = getContext().getString(R.string.page_network_error);
            }
            exceptionText = networkAnomaly;
            if (networkAnomalySrc == 0) {
                networkAnomalySrc = R.drawable.empty_default_network;
            }
            exceptionPicture = networkAnomalySrc;
        }
        binding.errorPromptView.setText(exceptionText);
        binding.icon.setImageResource(exceptionPicture);
    }

    public void showEmpty() {
        setDisplayedChild(NO_DATA);
    }

    public void showEmpty(String noDataPrompt) {
        setDisplayedChild(NO_DATA);
        setNoDataPrompt(noDataPrompt);
    }

    public void setNoDataIcon(int resId) {
        binding.noDataIcon.setImageResource(resId);
    }

    public ImageView getNoDataIcon() {
        return binding.noDataIcon;
    }

    public void setRetryListener(final OnClickListener listener) {
        binding.retryBtn.setOnClickListener(listener);
    }

    public void setDefaultEmptyImage(int resId) {
        binding.noDataIcon.setImageResource(resId);
    }

    public void setDefaultEmptyImage(int resId, int topMargin) {
        binding.noDataIcon.setImageResource(resId);
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) binding.noDataIcon.getLayoutParams();
        Logger.i("top_margin", "before top margin=" + marginLayoutParams.topMargin);
        marginLayoutParams.topMargin = topMargin;
        Logger.i("top_margin", "after top margin=" + topMargin);
        binding.noDataIcon.setLayoutParams(marginLayoutParams);
    }


    public void setNoDataPrompt(CharSequence noDataPrompt) {
        this.noData = noDataPrompt.toString();
        binding.noData.setText(noDataPrompt);
        if (noDataPrompt instanceof SpannableString) {
            binding.noData.setMovementMethod(OverLinkMovementMethod.getInstance());
        }
    }

    public TextView getNoDataTextView() {
        return binding.noData;
    }

    public void setNoDataButton(CharSequence buttonText, View.OnClickListener onClickListener) {
        binding.noDataBtn.setText(buttonText);
        binding.noDataBtn.setVisibility(View.VISIBLE);
        binding.noDataBtn.setOnClickListener(onClickListener);
    }

    public void setNoDataButton(CharSequence buttonText, int color, View.OnClickListener onClickListener) {
        binding.noDataBtn.setTextColor(color);
        setNoDataButton(buttonText, onClickListener);
    }


    public void setErrorPrompt(CharSequence errorPrompt) {
        binding.errorPromptView.setText(errorPrompt);
    }

    public int getCurrentViewIndex() {
        return indexOfChild(getCurrentView());
    }
}
