package com.kuaigeglobal.epnova.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.kuaigeglobal.epnova.R;

public class ExpandableTextView extends LinearLayout {

    // 默认值和常量
    private static final int DEFAULT_COLLAPSE_LINES = 1;
    private static final int DEFAULT_ANIMATION_DURATION = 300;
    private static final int DIRECTION_TOP_TO_BOTTOM = 0;
    private static final int DIRECTION_BOTTOM_TO_TOP = 1;
    private static final int POSITION_LEFT = 0;
    private static final int POSITION_RIGHT = 1;
    private static final int ICON_POSITION_LEFT = 0;
    private static final int ICON_POSITION_RIGHT = 1;
    private static final int ICON_POSITION_TOP = 2;
    private static final int ICON_POSITION_BOTTOM = 3;

    // View
    private TextView tvContent;
    private TextView btnExpandLeft;
    private TextView btnExpandRight;
    private LinearLayout rootLayout;

    // 状态
    private boolean isExpanded = false;
    private boolean isExpandable = false;

    // 属性
    private int collapseMaxLines = DEFAULT_COLLAPSE_LINES;
    private String expandText;
    private String collapseText;
    private int expandDirection = DIRECTION_TOP_TO_BOTTOM;
    private Drawable expandIcon;
    private Drawable collapseIcon;
    private int buttonPosition = POSITION_RIGHT;
    private int animationDuration = DEFAULT_ANIMATION_DURATION;
    private int iconPosition = ICON_POSITION_RIGHT;
    private int iconSize = -1;
    private int iconTextSpacing = dpToPx(4);

    // 尺寸
    private int originalHeight = 0;
    private int collapsedHeight = 0;
    private int currentHeight = 0;
    private ValueAnimator heightAnimator;
    private Integer heightBeforeAnimation;
    private final Runnable contentCheck = this::checkIfExpandable;

    private void scheduleContentCheck() {
        removeCallbacks(contentCheck);
        post(contentCheck);
    }

    private void cancelHeightAnimation(boolean restoreHeight) {
        if (heightAnimator != null) {
            heightAnimator.removeAllListeners();
            heightAnimator.cancel();
            heightAnimator = null;
        }
        if (restoreHeight) restoreLayoutHeight();
    }

    private void restoreLayoutHeight() {
        if (heightBeforeAnimation != null) {
            ViewGroup.LayoutParams params = getLayoutParams();
            if (params != null) {
                params.height = heightBeforeAnimation;
                setLayoutParams(params);
            }
            heightBeforeAnimation = null;
        }
    }

    private void animateHeight(int startHeight, int endHeight, boolean expanded) {
        cancelHeightAnimation(false);
        if (heightBeforeAnimation == null) {
            ViewGroup.LayoutParams params = getLayoutParams();
            heightBeforeAnimation = params == null ? ViewGroup.LayoutParams.WRAP_CONTENT : params.height;
        }
        heightAnimator = ValueAnimator.ofInt(Math.max(0, startHeight), Math.max(0, endHeight));
        heightAnimator.addUpdateListener(animation -> setViewHeight((int) animation.getAnimatedValue()));
        heightAnimator.setDuration(Math.max(0, animationDuration));
        heightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        heightAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                heightAnimator = null;
                tvContent.setMaxLines(expanded ? Integer.MAX_VALUE : collapseMaxLines);
                restoreLayoutHeight();
                scheduleContentCheck();
            }
        });
        heightAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(contentCheck);
        cancelHeightAnimation(true);
        tvContent.setMaxLines(isExpanded ? Integer.MAX_VALUE : collapseMaxLines);
        super.onDetachedFromWindow();
    }

    public ExpandableTextView(Context context) {
        super(context);
        init(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.layout_expandable_text_view, this, true);
        tvContent = findViewById(R.id.tvContent);
        btnExpandLeft = findViewById(R.id.btnExpandLeft);
        btnExpandRight = findViewById(R.id.btnExpandRight);
        rootLayout = findViewById(R.id.rootLayout);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableTextView);

            collapseMaxLines = Math.max(1, a.getInt(R.styleable.ExpandableTextView_collapseMaxLines, DEFAULT_COLLAPSE_LINES));
            expandText = a.getString(R.styleable.ExpandableTextView_expandText);
            collapseText = a.getString(R.styleable.ExpandableTextView_collapseText);
            expandDirection = a.getInt(R.styleable.ExpandableTextView_expandDirection, DIRECTION_TOP_TO_BOTTOM);
            expandIcon = a.getDrawable(R.styleable.ExpandableTextView_expandIcon);
            collapseIcon = a.getDrawable(R.styleable.ExpandableTextView_collapseIcon);
            buttonPosition = a.getInt(R.styleable.ExpandableTextView_buttonPosition, POSITION_RIGHT);
            animationDuration = a.getInt(R.styleable.ExpandableTextView_animationDuration, DEFAULT_ANIMATION_DURATION);
            iconPosition = a.getInt(R.styleable.ExpandableTextView_iconPosition, ICON_POSITION_RIGHT);
            iconSize = a.getDimensionPixelSize(R.styleable.ExpandableTextView_iconSize, -1);
            iconTextSpacing = a.getDimensionPixelSize(R.styleable.ExpandableTextView_iconTextSpacing, dpToPx(4));

            // 文本相关属性
            String text = a.getString(R.styleable.ExpandableTextView_text);
            if (text != null) {
                tvContent.setText(text);
            }
            int textColor = a.getColor(R.styleable.ExpandableTextView_textColor, Color.BLACK);
            tvContent.setTextColor(textColor);
            float textSize = a.getDimension(R.styleable.ExpandableTextView_textSize, 16);
            tvContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            // 按钮文字相关属性
            int buttonTextColor = a.getColor(R.styleable.ExpandableTextView_buttonTextColor, Color.BLUE);
            btnExpandLeft.setTextColor(buttonTextColor);
            btnExpandRight.setTextColor(buttonTextColor);
            float buttonTextSize = a.getDimension(R.styleable.ExpandableTextView_buttonTextSize, 14);
            btnExpandLeft.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize);
            btnExpandRight.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize);

            a.recycle();
        }

        setupButton();
        setupClickListeners();
        setCollapsedState();
    }

    // 在 setupButton 中使用新的布局调整方法
    private void setupButton() {
        if (buttonPosition == POSITION_LEFT) {
            btnExpandLeft.setVisibility(VISIBLE);
            btnExpandRight.setVisibility(GONE);
        } else {
            btnExpandLeft.setVisibility(GONE);
            btnExpandRight.setVisibility(VISIBLE);
        }

        // 设置图标与文字间距
        btnExpandLeft.setCompoundDrawablePadding(iconTextSpacing);
        btnExpandRight.setCompoundDrawablePadding(iconTextSpacing);

        // 初始布局调整
        adjustContentLayout(false);

        updateButtonAppearance();
    }


    // 修改 updateButtonAppearance 方法
    private void updateButtonAppearance() {
        TextView button = getCurrentButton();
        if (button == null) return;

        // 如果不可展开，直接隐藏按钮并调整布局
        if (!isExpandable) {
            button.setVisibility(GONE);
            adjustContentLayout(true);
            updateClickability();
            return;
        }

        String currentText;
        Drawable currentIcon;

        if (isExpanded) {
            // 收起状态
            currentText = collapseText;
            currentIcon = collapseIcon;

            // 如果只设置了 expandIcon 而没有设置 collapseIcon，展开后隐藏按钮
            if (expandIcon != null && collapseIcon == null) {
                button.setVisibility(GONE);
                adjustContentLayout(true);
                updateClickability();
                return;
            }
        } else {
            // 展开状态
            currentText = expandText;
            currentIcon = expandIcon;
        }

        // 按钮显示时的布局调整
        adjustContentLayout(false);

        // 设置图标
        setButtonIcon(button, currentIcon);

        // 设置文字
        if (currentText != null && !currentText.isEmpty()) {
            button.setText(currentText);
        } else {
            button.setText("");
        }

        // 处理图标大小
        if (iconSize > 0 && currentIcon != null) {
            setIconSize(button, currentIcon);
        }

        // 检查是否需要显示按钮 - 移除了重复的布局调整
        checkButtonVisibility(button, currentText, currentIcon);

        // 强制刷新按钮
        button.invalidate();
        updateClickability();
    }


    /**
     * 调整内容区域的布局参数
     * @param fillSpace 是否填满空间（隐藏按钮时）
     */
    private void adjustContentLayout(boolean fillSpace) {
        LayoutParams contentParams = (LayoutParams) tvContent.getLayoutParams();
        int targetWidth = fillSpace ? LayoutParams.MATCH_PARENT : 0;
        float targetWeight = fillSpace ? 0f : 1f;
        if (contentParams.width == targetWidth && contentParams.weight == targetWeight) return;

        if (fillSpace) {
            // 按钮隐藏时，内容区域占据全部空间
            contentParams.width = LayoutParams.MATCH_PARENT;
            contentParams.weight = 0;
        } else {
            // 按钮显示时，内容区域占据剩余空间
            contentParams.width = 0;
            contentParams.weight = 1;
        }

        tvContent.setLayoutParams(contentParams);
    }

    private void setButtonIcon(TextView button, Drawable icon) {
        if (icon == null) {
            button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            return;
        }

        // 确保图标可突变（避免共享状态）
        Drawable mutableIcon = icon.mutate();

        // 根据图标位置设置drawable
        Drawable leftDrawable = null;
        Drawable topDrawable = null;
        Drawable rightDrawable = null;
        Drawable bottomDrawable = null;

        switch (iconPosition) {
            case ICON_POSITION_LEFT:
                rightDrawable = mutableIcon;
                break;
            case ICON_POSITION_TOP:
                bottomDrawable = mutableIcon;
                break;
            case ICON_POSITION_RIGHT:
                leftDrawable = mutableIcon;
                break;
            case ICON_POSITION_BOTTOM:
                topDrawable = mutableIcon;
                break;
        }

        // 如果设置了图标大小，直接调用 setIconSize，否则使用 intrinsic bounds
        if (iconSize > 0) {
            setIconSize(button, mutableIcon);
        } else {
            // 使用 setCompoundDrawablesWithIntrinsicBounds 确保图标有正确的大小
            button.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, topDrawable, rightDrawable, bottomDrawable);
        }
    }
    // 强制刷新控件状态
    public void refresh() {
        updateButtonAppearance();
        updateClickability();
        requestLayout();
        scheduleContentCheck();
    }

    /**
     * 重置控件状态，用于在复用场景下清理状态
     */
    public void reset() {
        cancelHeightAnimation(true);
        isExpanded = false;
        isExpandable = false;
        originalHeight = 0;
        collapsedHeight = 0;
        currentHeight = 0;
        setCollapsedState();
        updateButtonAppearance();
        scheduleContentCheck();
    }

    private void setIconSize(TextView button, Drawable icon) {
        if (icon == null) return;

        // 设置图标大小
        int size = iconSize;
        icon.setBounds(0, 0, size, size);

        // 重新设置带大小的图标
        Drawable leftDrawable = null;
        Drawable topDrawable = null;
        Drawable rightDrawable = null;
        Drawable bottomDrawable = null;

        switch (iconPosition) {
            case ICON_POSITION_LEFT:
                leftDrawable = icon;
                break;
            case ICON_POSITION_TOP:
                topDrawable = icon;
                break;
            case ICON_POSITION_RIGHT:
                rightDrawable = icon;
                break;
            case ICON_POSITION_BOTTOM:
                bottomDrawable = icon;
                break;
        }

        button.setCompoundDrawables(leftDrawable, topDrawable, rightDrawable, bottomDrawable);
    }

    private void checkButtonVisibility(TextView button, String text, Drawable icon) {
        boolean hasText = text != null && !text.isEmpty();
        boolean hasIcon = icon != null;

        if (!hasText && !hasIcon) {
            // 既没有文字也没有图标，隐藏按钮
            button.setVisibility(GONE);
            // 注意：这里不需要调用 adjustContentLayout(true)，因为 updateButtonAppearance 已经调用了
        } else {
            // 这里不需要再次检查 isExpandable，因为 updateButtonAppearance 已经处理过了
            // 直接显示按钮即可
            button.setVisibility(VISIBLE);
            // 注意：这里不需要调用 adjustContentLayout(false)，因为 updateButtonAppearance 已经调用了

            // 如果只有图标，调整按钮的宽高
            if (hasIcon && !hasText) {
                int iconSizeValue = iconSize > 0 ? iconSize : dpToPx(24);
                ViewGroup.LayoutParams params = button.getLayoutParams();
                params.width = iconSizeValue + dpToPx(8);
                params.height = iconSizeValue + dpToPx(8);
                button.setLayoutParams(params);
            } else {
                // 恢复默认的wrap_content
                ViewGroup.LayoutParams params = button.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                button.setLayoutParams(params);
            }
        }
    }

    // 在 setText 方法中确保布局正确
    public void setText(CharSequence text) {
        cancelHeightAnimation(true);
        tvContent.setText(text);
        originalHeight = 0;
        collapsedHeight = 0;
        isExpanded = false;
        setCollapsedState();
        updateButtonAppearance();
        updateClickability();
        scheduleContentCheck();
    }
    // 添加重载版本，支持资源ID
    public void setText(int resId) {
        setText(getContext().getString(resId));
    }
    private void setupClickListeners() {
        btnExpandLeft.setOnClickListener(v -> toggleExpand());
        btnExpandRight.setOnClickListener(v -> toggleExpand());

        // 如果没有设置任何按钮内容，让整个内容区域可点击
        if ((expandText == null || expandText.isEmpty()) &&
                (collapseText == null || collapseText.isEmpty()) &&
                expandIcon == null && collapseIcon == null) {
            tvContent.setOnClickListener(v -> toggleExpand());
        } else {
            tvContent.setOnClickListener(null);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 确保在测量后检查可展开性
        scheduleContentCheck();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        currentHeight = h;
    }


    private void checkIfExpandable() {
        Layout layout = tvContent.getLayout();
        if (layout != null) {
            int lineCount = layout.getLineCount();
            boolean isEllipsized = false;

            // 检查是否有省略号
            if (lineCount >= collapseMaxLines) {
                int lastLineIndex = collapseMaxLines - 1;
                if (lastLineIndex >= 0 && lastLineIndex < lineCount) {
                    int ellipsisCount = layout.getEllipsisCount(lastLineIndex);
                    isEllipsized = ellipsisCount > 0;
                }
            }

            // 保存之前的状态用于比较
            boolean previousExpandable = isExpandable;
            isExpandable = lineCount > collapseMaxLines || isEllipsized;

            // 只有在状态发生变化时才更新UI，避免不必要的重绘
            if (previousExpandable != isExpandable) {
                updateButtonAppearance();
                updateClickability();
                requestLayout();
            }
        }
    }
    private void updateClickability() {
        TextView button = getCurrentButton();
        boolean hasButtonContent = button != null && button.getVisibility() == VISIBLE;

        if (!hasButtonContent && isExpandable) {
            // 没有按钮但需要展开，让内容可点击
            tvContent.setClickable(true);
            tvContent.setOnClickListener(v -> toggleExpand());
        } else {
            tvContent.setClickable(false);
            tvContent.setOnClickListener(null);
        }
    }

    private TextView getCurrentButton() {
        return buttonPosition == POSITION_LEFT ? btnExpandLeft : btnExpandRight;
    }

    private void toggleExpand() {
        if (!isExpandable) return;

        if (isExpanded) {
            collapse();
        } else {
            expand();
        }
    }

    private void expand() {
        cancelHeightAnimation(false);
        if (originalHeight == 0) {
            // 第一次展开，保存原始高度
            tvContent.setMaxLines(Integer.MAX_VALUE);
            measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            originalHeight = getMeasuredHeight();

            // 设置回收起状态
            setCollapsedState();
        }

        // 执行展开动画
        performExpandAnimation();
        isExpanded = true;
        updateButtonAppearance();

        // 关键修改：展开后重新检查布局
        scheduleContentCheck();
    }

    private void collapse() {
        cancelHeightAnimation(false);
        performCollapseAnimation();
        isExpanded = false;
        updateButtonAppearance();

        // 关键修改：收起后重新检查布局
        scheduleContentCheck();
    }

    private void performExpandAnimation() {
        tvContent.setMaxLines(Integer.MAX_VALUE);

        // 使用当前高度和原始高度进行动画
        int startHeight = currentHeight;
        int endHeight = originalHeight;

        animateHeight(startHeight, endHeight, true);
    }

    private void performCollapseAnimation() {
        if (collapsedHeight == 0) {
            // 计算收起时的高度
            tvContent.setMaxLines(collapseMaxLines);
            measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            collapsedHeight = getMeasuredHeight();
            tvContent.setMaxLines(Integer.MAX_VALUE);
        }

        // 使用当前高度和收起高度进行动画
        int startHeight = currentHeight;
        int endHeight = collapsedHeight;

        animateHeight(startHeight, endHeight, false);

    }
    private void setCollapsedState() {
        tvContent.setMaxLines(collapseMaxLines);
        tvContent.setEllipsize(TextUtils.TruncateAt.END);
    }

    // 设置视图高度的辅助方法
    private void setViewHeight(int height) {
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        } else {
            params.height = height;
        }
        setLayoutParams(params);
        currentHeight = height;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public CharSequence getText() {
        return tvContent.getText();
    }


    // 在 setCollapseMaxLines 方法中也确保正确更新状态
    public void setCollapseMaxLines(int lines) {
        cancelHeightAnimation(true);
        this.collapseMaxLines = Math.max(1, lines);
        originalHeight = 0;
        collapsedHeight = 0;
        if (!isExpanded) {
            setCollapsedState();
        }
        scheduleContentCheck();
    }

    public int getCollapseMaxLines() {
        return collapseMaxLines;
    }

    public void setExpandText(String text) {
        this.expandText = text;
        updateButtonAppearance();
    }

    public String getExpandText() {
        return expandText;
    }

    public void setCollapseText(String text) {
        this.collapseText = text;
        updateButtonAppearance();
    }

    public String getCollapseText() {
        return collapseText;
    }

    public void setExpandIcon(Drawable icon) {
        this.expandIcon = icon;
        updateButtonAppearance();
    }

    public void setExpandIconRes(int resId) {
        this.expandIcon = ContextCompat.getDrawable(getContext(), resId);
        updateButtonAppearance();
    }

    public Drawable getExpandIcon() {
        return expandIcon;
    }

    public void setCollapseIcon(Drawable icon) {
        this.collapseIcon = icon;
        updateButtonAppearance();
    }

    public void setCollapseIconRes(int resId) {
        this.collapseIcon = ContextCompat.getDrawable(getContext(), resId);
        updateButtonAppearance();
    }

    public Drawable getCollapseIcon() {
        return collapseIcon;
    }

    public void setExpandDirection(int direction) {
        this.expandDirection = direction;
    }

    public int getExpandDirection() {
        return expandDirection;
    }

    public void setButtonPosition(int position) {
        this.buttonPosition = position;
        setupButton();
    }

    public int getButtonPosition() {
        return buttonPosition;
    }

    public void setIconPosition(int position) {
        this.iconPosition = position;
        updateButtonAppearance();
    }

    public int getIconPosition() {
        return iconPosition;
    }

    public void setIconSize(int size) {
        this.iconSize = size;
        updateButtonAppearance();
    }

    public int getIconSize() {
        return iconSize;
    }

    public void setIconTextSpacing(int spacing) {
        this.iconTextSpacing = spacing;
        btnExpandLeft.setCompoundDrawablePadding(spacing);
        btnExpandRight.setCompoundDrawablePadding(spacing);
        updateButtonAppearance();
    }

    public int getIconTextSpacing() {
        return iconTextSpacing;
    }

    public void setAnimationDuration(int duration) {
        this.animationDuration = Math.max(0, duration);
    }

    public int getAnimationDuration() {
        return animationDuration;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public boolean isExpandable() {
        return isExpandable;
    }
}
