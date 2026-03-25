package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kuaigeglobal.epnova.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lu on 2017/4/5.
 */
public class TagLayout extends ViewGroup {
    private static final String INSTANCE_STATE = "saved_instance";
    private static final String TAGS = "tags";
    private static final String SELECTED_TAG_POSITIONS = "selected_tag_positions";
    private static final int SELECT_MODE_NONE = 0;
    private static final int SELECT_MODE_SINGLE = 1;
    private static final int SELECT_MODE_MULTIPLE = 2;
    private static final int CACHE_MODE_AUTO = 0;
    private static final int CACHE_MODE_LAZY = 1;
    private static final int CACHE_MODE_NONE = 2;
    private static final int LINES_CACHE_COUNT = 10;
    public static final int MAX_PRE_CACHE_COUNT = 30;
    private static final String DEFAULT_TEXT = "";

    // 配置类
    private static class TagConfig {
        int horizontalSpace = 0;
        int verticalSpace = 0;
        int maxLines = Integer.MAX_VALUE;
        int maximumSelectionCount = Integer.MAX_VALUE;
        int cacheMode = CACHE_MODE_AUTO;
        int maxTags = Integer.MAX_VALUE;
        int textMaxLine = Integer.MAX_VALUE;
        int tagSelectMode = SELECT_MODE_NONE;

        boolean usePreCache = false;
        boolean bold = false;

        Integer tagResId;
        Integer tagTextId;
        Integer tagTextSize;
        Integer tagBackground;
        Integer tagMinWidth;
        Integer tagTextHorizontalPadding;
        Integer tagTextVerticalPadding;
        ColorStateList tagTextColor;

        static TagConfig fromAttributes(Context context, AttributeSet attrs) {
            TagConfig config = new TagConfig();
            if (attrs == null) return config;

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TagLayout);
            try {
                config.horizontalSpace = a.getDimensionPixelOffset(R.styleable.TagLayout_horizontalSpace, 0);
                config.verticalSpace = a.getDimensionPixelOffset(R.styleable.TagLayout_verticalSpace, 0);
                config.maxLines = a.getInteger(R.styleable.TagLayout_maxLines, Integer.MAX_VALUE);
                config.usePreCache = a.getBoolean(R.styleable.TagLayout_preCache, false);
                config.maximumSelectionCount = a.getInteger(R.styleable.TagLayout_maximumSelectionCount, Integer.MAX_VALUE);
                config.cacheMode = a.getInt(R.styleable.TagLayout_cacheMode, CACHE_MODE_AUTO);
                config.maxTags = a.getInteger(R.styleable.TagLayout_maxTags, Integer.MAX_VALUE);
                config.textMaxLine = a.getInteger(R.styleable.TagLayout_tagTextMaxLine, Integer.MAX_VALUE);
                config.bold = a.getBoolean(R.styleable.TagLayout_tagTextBold, false);
                config.tagSelectMode = a.getInt(R.styleable.TagLayout_tagSelectMode, SELECT_MODE_NONE);

                if (a.hasValue(R.styleable.TagLayout_tagResId)) {
                    config.tagResId = a.getResourceId(R.styleable.TagLayout_tagResId, -1);
                }
                if (a.hasValue(R.styleable.TagLayout_tagTextId)) {
                    config.tagTextId = a.getResourceId(R.styleable.TagLayout_tagTextId, R.id.text);
                }
                if (a.hasValue(R.styleable.TagLayout_tagTextSize)) {
                    config.tagTextSize = a.getDimensionPixelSize(R.styleable.TagLayout_tagTextSize, 0);
                }
                if (a.hasValue(R.styleable.TagLayout_tagBackground)) {
                    config.tagBackground = a.getResourceId(R.styleable.TagLayout_tagBackground, -1);
                }
                if (a.hasValue(R.styleable.TagLayout_tagTextColor)) {
                    config.tagTextColor = a.getColorStateList(R.styleable.TagLayout_tagTextColor);
                }
                if (a.hasValue(R.styleable.TagLayout_tagTextHorizontalPadding)) {
                    config.tagTextHorizontalPadding = a.getDimensionPixelOffset(R.styleable.TagLayout_tagTextHorizontalPadding, 0);
                }
                if (a.hasValue(R.styleable.TagLayout_tagTextVerticalPadding)) {
                    config.tagTextVerticalPadding = a.getDimensionPixelOffset(R.styleable.TagLayout_tagTextVerticalPadding, 0);
                }
                if (a.hasValue(R.styleable.TagLayout_tagMinWidth)) {
                    config.tagMinWidth = a.getDimensionPixelOffset(R.styleable.TagLayout_tagMinWidth, 0);
                }
            } finally {
                a.recycle();
            }
            return config;
        }
    }

    // 事件分发器
    private static class TagEventDispatcher {
        private OnSelectOutRangeListener onSelectOutRangeListener;
        private OnSelectChangeListener onSelectChangeListener;
        private OnItemClickListener onItemClickListener;
        private OnTagsLayoutListener onTagsLayoutListener;

        boolean dispatchItemClick(TagView tagView, int position) {
            return onItemClickListener != null &&
                    onItemClickListener.onItemClick(tagView, position);
        }

        void dispatchSelectChange(TagLayout parent, TagView child, int index, boolean selected) {
            if (onSelectChangeListener != null) {
                onSelectChangeListener.onSelectChange(parent, child, index, selected);
            }
        }

        void dispatchSelectOutRange(TagLayout parent, TagView child, int index) {
            if (onSelectOutRangeListener != null) {
                onSelectOutRangeListener.onSelectOutRange(parent, child, index);
            }
        }

        void dispatchTagsLayout(int lineCount, int showCount, boolean outRange) {
            if (onTagsLayoutListener != null) {
                onTagsLayoutListener.onTagsLayout(lineCount, showCount, outRange);
            }
        }

        void setOnSelectOutRangeListener(OnSelectOutRangeListener listener) {
            this.onSelectOutRangeListener = listener;
        }

        void setOnSelectChangeListener(OnSelectChangeListener listener) {
            this.onSelectChangeListener = listener;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            this.onItemClickListener = listener;
        }

        void setOnTagsLayoutListener(OnTagsLayoutListener listener) {
            this.onTagsLayoutListener = listener;
        }
    }

    // 测量状态管理
    private static class MeasureState {
        boolean hasMeasured = false;
        int lastWidthMeasureSpec;
        int lastHeightMeasureSpec;
        int lastMeasureWidth;
        int lastMeasureHeight;
        int lastMeasuredLine;

        boolean shouldSkipMeasure(int widthMeasureSpec, int heightMeasureSpec, int currentMaxLines) {
            return hasMeasured &&
                    lastWidthMeasureSpec == widthMeasureSpec &&
                    lastHeightMeasureSpec == heightMeasureSpec &&
                    currentMaxLines == lastMeasuredLine;
        }

        void saveMeasureState(int widthMeasureSpec, int heightMeasureSpec, int width, int height, int maxLines) {
            this.lastWidthMeasureSpec = widthMeasureSpec;
            this.lastHeightMeasureSpec = heightMeasureSpec;
            this.lastMeasureWidth = width;
            this.lastMeasureHeight = height;
            this.lastMeasuredLine = maxLines;
            this.hasMeasured = true;
        }

        void reset() {
            hasMeasured = false;
        }
    }

    // 默认TagView实现
    private static class DefaultTagView implements TagView {
        private final View mHostView;
        private TextView mCachedTextView;

        DefaultTagView(View hostView) {
            mHostView = hostView;
        }

        @Override
        public View getTagHost() {
            return mHostView;
        }

        @Override
        public TextView getTagText() {
            if (mCachedTextView == null) {
                mCachedTextView = findTextViewInView(mHostView);
            }
            return mCachedTextView;
        }

        private TextView findTextViewInView(View view) {
            if (view instanceof TextView) {
                return (TextView) view;
            } else if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof TextView) {
                        return (TextView) child;
                    }
                }
            }
            throw new NullPointerException("Cannot find TextView in " + view);
        }

        @Override
        public void setTagSelected(boolean selected) {
            mHostView.setSelected(selected);
            if (mHostView instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) mHostView;
                for (int i = 0; i < group.getChildCount(); i++) {
                    group.getChildAt(i).setSelected(selected);
                }
            }
        }

        @Override
        public void setMinWidth(int minWidth) {
            getTagHost().setMinimumWidth(minWidth);
        }

        @Override
        public boolean isTagSelected() {
            return mHostView.isSelected();
        }
    }

    // TagLine类
    public class TagLine {
        private final List<View> mTagViews = new ArrayList<>();
        private final int mMaxWidth;
        private final int mHorizontalSpace;
        private int mUsedWidth;
        private int mHeight;

        public TagLine(int maxWidth, int horizontalSpace) {
            mMaxWidth = maxWidth;
            mHorizontalSpace = horizontalSpace;
        }

        public void addView(View child) {
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (mTagViews.isEmpty()) {
                mUsedWidth = Math.min(childWidth, mMaxWidth);
                mHeight = childHeight;
            } else {
                mUsedWidth += mHorizontalSpace + childWidth;
                mHeight = Math.max(childHeight, mHeight);
            }
            mTagViews.add(child);
        }

        public boolean canAccept(View child) {
            int width = child.getMeasuredWidth();
            if (mTagViews.isEmpty()) {
                return true;
            }
            return mUsedWidth + width + mHorizontalSpace <= mMaxWidth;
        }

        public void layout(int marginLeft, int marginTop) {
            for (View child : mTagViews) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                int extraTop = (mHeight - childHeight) / 2;

                child.layout(
                        marginLeft,
                        marginTop + extraTop,
                        marginLeft + childWidth,
                        marginTop + extraTop + childHeight
                );
                marginLeft += childWidth + mHorizontalSpace;
            }
        }
    }

    public interface OnSelectOutRangeListener {
        void onSelectOutRange(TagLayout parent, TagView child, int index);
    }

    public interface OnSelectChangeListener {
        void onSelectChange(TagLayout parent, TagView child, int index, boolean isSelected);
    }

    public interface OnItemClickListener {
        boolean onItemClick(TagView child, int index);
    }

    public interface OnTagsLayoutListener {
        void onTagsLayout(int lineCount, int showCount, boolean outRange);
    }

    public interface OnTagViewAttachedListener {
        void onTagViewAttached(TagView tagView, int position);
    }

    public interface TagView {
        View getTagHost();
        TextView getTagText();
        void setTagSelected(boolean selected);
        void setMinWidth(int minWidth);
        boolean isTagSelected();
    }

    private final TagConfig mConfig;
    private final TagEventDispatcher mEventDispatcher = new TagEventDispatcher();
    private final MeasureState mMeasureState = new MeasureState();
    private final LayoutInflater mInflater;

    private List<TagLine> mTagLines = new ArrayList<>();
    private List<String> mTags;
    private TagView mCurrentSelected;
    private int mSelectedChildCount;
    private boolean mClickAvailable = true;

    public TagLayout(Context context) {
        this(context, null);
    }

    public TagLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = LayoutInflater.from(context);
        mConfig = TagConfig.fromAttributes(context, attrs);

        initializePreCache();
    }

    private void initializePreCache() {
        if (mConfig.usePreCache) {
            int count = Math.min(MAX_PRE_CACHE_COUNT,
                    Math.min(mConfig.maxTags, mConfig.maxLines * LINES_CACHE_COUNT));
            for (int i = 0; i < count; i++) {
                TagView tagView = createTagView(DEFAULT_TEXT);
                tagView.getTagHost().setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMeasureState.shouldSkipMeasure(widthMeasureSpec, heightMeasureSpec, mConfig.maxLines) && !mTagLines.isEmpty()) {
            setMeasuredDimension(mMeasureState.lastMeasureWidth, mMeasureState.lastMeasureHeight);
            return;
        }

        mTagLines.clear();
        int parentSuggestWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int maxWidth = parentSuggestWidth - getPaddingLeft() - getPaddingRight();

        TagLine currentLine = null;
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);

            if (currentLine == null) {
                currentLine = createTagLine(maxWidth);
            } else if (!currentLine.canAccept(child)) {
                if (mTagLines.size() == mConfig.maxLines) {
                    break;
                }
                currentLine = createTagLine(maxWidth);
            }

            currentLine.addView(child);
        }

        int measuredHeight = calculateTotalHeight();
        setMeasuredDimension(parentSuggestWidth, resolveSize(measuredHeight, heightMeasureSpec));

        mMeasureState.saveMeasureState(widthMeasureSpec, heightMeasureSpec,
                parentSuggestWidth, measuredHeight, mConfig.maxLines);

        notifyTagsLayoutIfNeeded();
    }

    private int calculateTotalHeight() {
        int totalHeight = getPaddingTop() + getPaddingBottom();
        for (int i = 0; i < mTagLines.size(); i++) {
            totalHeight += mTagLines.get(i).mHeight;
            if (i != mTagLines.size() - 1) {
                totalHeight += mConfig.verticalSpace;
            }
        }
        return totalHeight;
    }

    private void notifyTagsLayoutIfNeeded() {
        int totalViewSize = 0;
        for (TagLine line : mTagLines) {
            totalViewSize += line.mTagViews.size();
        }

        int tagsSize = mTags != null ? mTags.size() : 0;
        mEventDispatcher.dispatchTagsLayout(mTagLines.size(), totalViewSize, totalViewSize < tagsSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int currentTop = getPaddingTop();
        int currentLeft = getPaddingLeft();

        for (TagLine line : mTagLines) {
            line.layout(currentLeft, currentTop);
            currentTop += mConfig.verticalSpace + line.mHeight;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());

        if (mTags != null) {
            bundle.putStringArrayList(TAGS, new ArrayList<>(mTags));
        }

        List<Integer> selectedPositions = getSelectedTagPositions();
        if (selectedPositions != null && !selectedPositions.isEmpty()) {
            bundle.putIntegerArrayList(SELECTED_TAG_POSITIONS, new ArrayList<>(selectedPositions));
        }

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mTags = bundle.getStringArrayList(TAGS);
            List<Integer> selectedTagPositions = bundle.getIntegerArrayList(SELECTED_TAG_POSITIONS);

            if (selectedTagPositions != null && !selectedTagPositions.isEmpty()) {
                if (getChildCount() >= selectedTagPositions.size()) {
                    if (mConfig.tagSelectMode == SELECT_MODE_MULTIPLE) {
                        selectTagPositions(selectedTagPositions);
                    } else if (!selectedTagPositions.isEmpty()) {
                        selectTagPosition(selectedTagPositions.get(0));
                    }
                }
            }
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    // 公共方法保持不变
    public List<String> getTags() {
        return mTags;
    }

    public void removeTag(String tag) {
        if (mTags == null) return;

        int pos = mTags.indexOf(tag);
        if (pos != -1) {
            mTagLines.clear();
            mTags.remove(pos);
            removeViewAt(pos);
            mMeasureState.reset();
            requestLayout();
        }
    }

    public void setMaxLines(int maxLines) {
        mConfig.maxLines = maxLines;
        mMeasureState.reset();
        requestLayout();
    }

    public int tagLineCount() {
        return mTagLines.size();
    }

    public void setClickAvailable(boolean clickAvailable) {
        this.mClickAvailable = clickAvailable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mClickAvailable) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void setTags(String... tagsList) {
        if (tagsList != null && tagsList.length > 0) {
            setTags(Arrays.asList(tagsList));
        }
    }

    public void setTags(List<String> tagsList, boolean forceRefresh) {
        setTags(tagsList, forceRefresh, null);
    }


    public void setTags(List<String> tagsList) {
        setTags(tagsList, false, null);
    }

    public void setTags(List<String> tagsList, boolean forceRefresh, OnTagViewAttachedListener onTagViewAttachedListener) {
        mMeasureState.reset();

        if (mConfig.cacheMode != CACHE_MODE_NONE) {
            applyCacheMode(tagsList, onTagViewAttachedListener);
        } else {
            applyNoCacheMode(tagsList, onTagViewAttachedListener);
        }
        if (forceRefresh) {
            forceRefreshLayout();
        }
        this.mTags = tagsList;
    }

    private void applyCacheMode(List<String> tagsList, OnTagViewAttachedListener listener) {
        int maxDisplayTags = Math.min(tagsList == null ? 0 : tagsList.size(), mConfig.maxTags);
        int childCount = getChildCount();
        int availableCount = Math.min(maxDisplayTags, childCount);

        // 更新可见的tag
        for (int i = 0; i < availableCount; i++) {
            TagView tagView = toTagView(getChildAt(i));
            tagView.getTagHost().setVisibility(View.VISIBLE);
            String tagText = tagsList.get(i);
            tagView.getTagText().setText(tagText);
            if (listener != null) {
                listener.onTagViewAttached(tagView, i);
            }
        }

        // 处理多余的view
        if (childCount > maxDisplayTags) {
            handleExtraViews(availableCount, childCount);
        } else if (childCount < maxDisplayTags) {
            handleMissingViews(tagsList, childCount, maxDisplayTags, listener);
        }
    }

    private void forceRefreshLayout() {
        //强制重新测量布局
        if (getWidth() > 0) {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST)
            );
            layout(getLeft(), getTop(), getRight(), getBottom());
        } else {
            requestLayout();
        }
    }

    private void handleExtraViews(int availableCount, int childCount) {
        if (mConfig.cacheMode == CACHE_MODE_LAZY) {
            for (int i = availableCount; i < childCount; i++) {
                toTagView(getChildAt(i)).getTagHost().setVisibility(View.GONE);
            }
        } else {
            for (int i = childCount - 1; i >= availableCount; i--) {
                removeViewAt(i);
            }
        }
    }

    private void handleMissingViews(List<String> tagsList, int childCount, int maxDisplayTags,
                                    OnTagViewAttachedListener listener) {
        int addedCount = maxDisplayTags - childCount;
        for (int i = 0; i < addedCount; i++) {
            String tagText = tagsList.get(i + childCount);
            TagView tag = createTagView(tagText);
            if (listener != null) {
                listener.onTagViewAttached(tag, i + childCount);
            }
        }
    }

    private void applyNoCacheMode(List<String> tagsList, OnTagViewAttachedListener listener) {
        removeAllViews();
        if (tagsList != null && !tagsList.isEmpty()) {
            for (int i = 0; i < tagsList.size(); i++) {
                TagView tv = createTagView(tagsList.get(i));
                if (listener != null) {
                    listener.onTagViewAttached(tv, i);
                }
            }
        }
    }

    public void unSelectTagPosition(int position) {
        if (position >= 0 && position < getChildCount()) {
            getChildAt(position).setSelected(false);
            mSelectedChildCount--;
        }
    }

    public void unSelectTag(String tag) {
        if (mTags == null) return;

        int index = mTags.indexOf(tag);
        if (index != -1) {
            getChildAt(index).setSelected(false);
            mSelectedChildCount--;
        }
    }

    public void selectTagPositions(Integer... selectedPos) {
        selectTagPositions(Arrays.asList(selectedPos));
    }

    public void selectTags(String... tags) {
        selectTags(Arrays.asList(tags));
    }

    public void selectTags(List<String> tags) {
        List<Integer> positions = new ArrayList<>();
        if (mTags != null) {
            for (String tag : tags) {
                int index = mTags.indexOf(tag);
                if (index != -1) {
                    positions.add(index);
                }
            }
        }
        selectTagPositions(positions);
    }

    public void selectTagPositions(List<Integer> selectedPos) {
        if (selectedPos.size() > 1 && mConfig.tagSelectMode == SELECT_MODE_SINGLE) {
            throw new UnsupportedOperationException("select at most 1 tag in SELECT_MODE_SINGLE!");
        }

        int childCount = getChildCount();
        int selectedCount = 0;

        for (int i = 0; i < childCount; i++) {
            TagView tagView = toTagView(getChildAt(i));
            boolean shouldSelect = selectedPos.contains(i);
            tagView.setTagSelected(shouldSelect);

            if (shouldSelect) {
                selectedCount++;
                if (mConfig.tagSelectMode == SELECT_MODE_SINGLE) {
                    mCurrentSelected = tagView;
                }
            }
        }

        mSelectedChildCount = selectedCount;
    }

    public void unSelectAllTags() {
        selectTagPosition(-1);
    }

    public void selectTagPosition(int index) {
        if (index == -1) {
            clearAllSelections();
        } else {
            handleTagSelection(index);
        }
    }

    private void clearAllSelections() {
        if (mConfig.tagSelectMode == SELECT_MODE_SINGLE) {
            if (mCurrentSelected != null) {
                mCurrentSelected.setTagSelected(false);
            }
            mCurrentSelected = null;
            mSelectedChildCount = 0;
        } else {
            if (mSelectedChildCount > 0) {
                for (int i = 0; i < getChildCount(); i++) {
                    toTagView(getChildAt(i)).setTagSelected(false);
                }
                mSelectedChildCount = 0;
            }
        }
    }

    private void handleTagSelection(int index) {
        TagView tagView = toTagView(getChildAt(index));

        if (mConfig.tagSelectMode == SELECT_MODE_SINGLE) {
            if (mCurrentSelected != tagView) {
                if (mCurrentSelected != null) {
                    mCurrentSelected.setTagSelected(false);
                }
                mCurrentSelected = tagView;
                mCurrentSelected.setTagSelected(true);
                mSelectedChildCount = 1;
            }
        } else if (mConfig.tagSelectMode == SELECT_MODE_MULTIPLE) {
            if (mSelectedChildCount < mConfig.maximumSelectionCount) {
                tagView.setTagSelected(true);
                mSelectedChildCount++;
            }
        }
    }

    public void setMaximumSelectionCount(int maximumSelectionCount) {
        mConfig.maximumSelectionCount = maximumSelectionCount;
    }

    public int getSelectedTagPosition() {
        if (mConfig.tagSelectMode == SELECT_MODE_SINGLE && mCurrentSelected != null) {
            return indexOfChild(mCurrentSelected.getTagHost());
        }
        return -1;
    }

    public String getSelectedTag() {
        int pos = getSelectedTagPosition();
        if (pos != -1 && mTags != null && pos < mTags.size()) {
            return mTags.get(pos);
        }
        return null;
    }

    public List<Integer> getSelectedTagPositions() {
        List<Integer> selectedList = new ArrayList<>();

        if (mConfig.tagSelectMode == SELECT_MODE_SINGLE && mCurrentSelected != null) {
            selectedList.add(indexOfChild(mCurrentSelected.getTagHost()));
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == View.VISIBLE && child.isSelected()) {
                    selectedList.add(i);
                }
            }
        }
        return selectedList;
    }

    // 事件监听器设置方法保持不变
    public void setOnSelectOutRangeListener(OnSelectOutRangeListener listener) {
        mEventDispatcher.setOnSelectOutRangeListener(listener);
    }

    public void setOnSelectChangeListener(OnSelectChangeListener listener) {
        mEventDispatcher.setOnSelectChangeListener(listener);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mEventDispatcher.setOnItemClickListener(listener);
    }

    public void setOnTagsLayoutListener(OnTagsLayoutListener listener) {
        mEventDispatcher.setOnTagsLayoutListener(listener);
    }

    // 内部辅助方法
    private TagView createTagView(final String tagText) {
        View child = mConfig.tagResId != null ?
                mInflater.inflate(mConfig.tagResId, null) :
                (TextView) mInflater.inflate(R.layout.default_tag_text, null);

        final TagView tagView = toTagView(child);
        configureTagView(tagView);
        tagView.getTagText().setText(tagText);

        setupTagClickListener(tagView);
        addView(tagView.getTagHost());
        return tagView;
    }

    private void configureTagView(TagView tagView) {
        View host = tagView.getTagHost();
        TextView textView = tagView.getTagText();

        if (mConfig.tagBackground != null) {
            host.setBackgroundResource(mConfig.tagBackground);
        }

        if (mConfig.textMaxLine < Integer.MAX_VALUE) {
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setMaxLines(mConfig.textMaxLine);
        }

        if (mConfig.tagTextSize != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mConfig.tagTextSize);
        }

        if (mConfig.tagTextColor != null) {
            textView.setTextColor(mConfig.tagTextColor);
        }

        if (mConfig.tagMinWidth != null) {
            tagView.setMinWidth(mConfig.tagMinWidth);
        }

        if (mConfig.bold) {
            textView.getPaint().setFakeBoldText(true);
        }

        applyTagPadding(tagView);
    }

    private void applyTagPadding(TagView tagView) {
        View host = tagView.getTagHost();
        int paddingLeft = host.getPaddingLeft();
        int paddingRight = host.getPaddingRight();
        int paddingTop = host.getPaddingTop();
        int paddingBottom = host.getPaddingBottom();

        if (mConfig.tagTextHorizontalPadding != null) {
            paddingLeft = mConfig.tagTextHorizontalPadding;
            paddingRight = mConfig.tagTextHorizontalPadding;
        }
        if (mConfig.tagTextVerticalPadding != null) {
            paddingTop = mConfig.tagTextVerticalPadding;
            paddingBottom = mConfig.tagTextVerticalPadding;
        }

        host.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    private void setupTagClickListener(final TagView tagView) {
        final int position = getChildCount();
        tagView.getTagHost().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tagView.getTagHost().getVisibility() != View.VISIBLE) return;

                if (mEventDispatcher.dispatchItemClick(tagView, position)) {
                    return;
                }

                handleTagSelectionLogic(tagView, position);
            }
        });
    }

    private void handleTagSelectionLogic(TagView tagView, int position) {
        if (mConfig.tagSelectMode == SELECT_MODE_MULTIPLE) {
            handleMultipleSelection(tagView, position);
        } else if (mConfig.tagSelectMode == SELECT_MODE_SINGLE) {
            handleSingleSelection(tagView, position);
        }
    }

    private void handleMultipleSelection(TagView tagView, int position) {
        if (mSelectedChildCount >= mConfig.maximumSelectionCount && !tagView.isTagSelected()) {
            mEventDispatcher.dispatchSelectOutRange(TagLayout.this, tagView, position);
            return;
        }

        boolean newSelectedState = !tagView.isTagSelected();
        tagView.setTagSelected(newSelectedState);

        if (newSelectedState) {
            mSelectedChildCount++;
        } else {
            mSelectedChildCount--;
        }

        mEventDispatcher.dispatchSelectChange(TagLayout.this, tagView, position, newSelectedState);
    }

    private void handleSingleSelection(TagView tagView, int position) {
        if (mCurrentSelected != null) {
            mCurrentSelected.setTagSelected(false);
        }
        tagView.setTagSelected(true);
        mSelectedChildCount = 1;
        mCurrentSelected = tagView;
        mEventDispatcher.dispatchSelectChange(TagLayout.this, tagView, position, true);
    }

    public TagView toTagView(View view) {
        if (view instanceof TagView) {
            return (TagView) view;
        } else {
            return new DefaultTagView(view);
        }
    }

    private TagLine createTagLine(int maxWidth) {
        TagLine tagLine = new TagLine(maxWidth, mConfig.horizontalSpace);
        mTagLines.add(tagLine);
        return tagLine;
    }
}