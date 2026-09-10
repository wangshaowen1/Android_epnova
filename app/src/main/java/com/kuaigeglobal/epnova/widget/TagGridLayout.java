package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;


import com.kuaigeglobal.epnova.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagGridLayout extends GridLayout {
    public static final int SELECT_MODE_NONE = 0;
    public static final int SELECT_MODE_SINGLE = 1;
    public static final int SELECT_MODE_MULTIPLE = 2;
    private LayoutInflater mInflater;
    private int mHorizontalSpace;
    private int mVerticalSpace;
    private Integer mTagTextSize;
    private Integer mTagResId;
    private Integer mTagBackground;
    private ColorStateList mTagTextColor;
    private Integer mTagTextHorizontalPadding;
    private Integer mTagTextVerticalPadding;
    private int mTagSelectMode = SELECT_MODE_NONE;
    private int mMaximumSelectionCount = Integer.MAX_VALUE;
    private int mSelectedChildCount;
    private TagView mCurrentSelected;
    private List<String> mTags = new ArrayList<>();
    private int mLaidOutColumnCount;
    private long mDataVersion;

    public TagGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initWithAttr(context, attrs);
    }


    public TagGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWithAttr(context, attrs);
    }

    public TagGridLayout(Context context) {
        super(context);
        initWithAttr(context, null);
    }


    public interface OnSelectOutRangeListener {
        void onSelectOutRange(TagGridLayout parent, View child, int index);
    }

    public interface OnSelectChangeListener {
        void onSelectChange(TagGridLayout parent, View child, int index, boolean isSelected);
    }

    public interface OnItemClickListener {
        boolean onItemClick(TagView child, int index);
    }

    private OnSelectOutRangeListener onSelectOutRangeListener;

    private OnItemClickListener onItemClickListener;

    private OnSelectChangeListener onSelectChangeListener;


    public void setOnSelectOutRangeListener(OnSelectOutRangeListener onSelectOutRangeListener) {
        this.onSelectOutRangeListener = onSelectOutRangeListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnSelectChangeListener(OnSelectChangeListener onSelectChangeListener) {
        this.onSelectChangeListener = onSelectChangeListener;
    }

    private void initWithAttr(Context context, AttributeSet attrs) {
        mInflater = LayoutInflater.from(context);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TagGridView);
        mHorizontalSpace = a.getDimensionPixelOffset(R.styleable.TagGridView_horizontalSpace, 0);
        mVerticalSpace = a.getDimensionPixelOffset(R.styleable.TagGridView_verticalSpace, 0);
        mMaximumSelectionCount = a.getInteger(R.styleable.TagGridView_maximumSelectionCount, Integer.MAX_VALUE);
        if (a.hasValue(R.styleable.TagGridView_tagResId)) {
            mTagResId = a.getResourceId(R.styleable.TagGridView_tagResId, -1);
        }
        if (a.hasValue(R.styleable.TagGridView_tagTextSize)) {
            mTagTextSize = a.getDimensionPixelSize(R.styleable.TagGridView_tagTextSize, 0);
        }
        if (a.hasValue(R.styleable.TagGridView_tagBackground)) {
            mTagBackground = a.getResourceId(R.styleable.TagGridView_tagBackground, -1);
        }
        if (a.hasValue(R.styleable.TagGridView_tagTextColor)) {
            mTagTextColor = a.getColorStateList(R.styleable.TagGridView_tagTextColor);
        }
        if (a.hasValue(R.styleable.TagGridView_tagTextHorizontalPadding)) {
            mTagTextHorizontalPadding = a.getDimensionPixelOffset(R.styleable.TagGridView_tagTextHorizontalPadding, 0);
        }
        if (a.hasValue(R.styleable.TagGridView_tagSelectMode)) {
            mTagSelectMode = a.getInt(R.styleable.TagGridView_tagSelectMode, SELECT_MODE_NONE);
        }
        if (a.hasValue(R.styleable.TagGridView_tagTextVerticalPadding)) {
            mTagTextVerticalPadding = a.getDimensionPixelOffset(R.styleable.TagGridView_tagTextVerticalPadding, 0);
        }
        a.recycle();
        mMaximumSelectionCount = Math.max(0, mMaximumSelectionCount);
        if (getColumnCount() <= 0) setColumnCount(1);
    }

    public void setTagSelectMode(int tagSelectMode) {
        mTagSelectMode = tagSelectMode;
        reconcileSelection();
    }

    public interface OnTagViewAttachedListener<T> {
        public void onTagViewAttached(View view, int position);
    }

    private void addSpaceTag() {
        TagView tag = addTag("");
        tag.getTagHost().setVisibility(View.INVISIBLE);
        tag.getTagHost().setClickable(false);
    }


    public void setTags(String... tagsList) {
        setTags(tagsList == null ? null : Arrays.asList(tagsList), null);
    }
    public  void setTags(List<String> tagsList){
        setTags(tagsList,null);
    }
    public <T> void setTags(List<String> tagsList, OnTagViewAttachedListener<T> onTagViewAttachedListener) {
        List<String> nextTags = tagsList == null ? new ArrayList<>() : new ArrayList<>(tagsList);
        if (getColumnCount() <= 0) setColumnCount(1);
        int columns = getColumnCount();
        int paddingCount = (columns - nextTags.size() % columns) % columns;
        mDataVersion++;
        // DataBinding 仅更新选中项时复用当前格子，避免整组重建和布局闪动。
        if (nextTags.equals(mTags) && mLaidOutColumnCount == columns
                && getChildCount() == nextTags.size() + paddingCount) {
            if (onTagViewAttachedListener != null) {
                for (int i = 0; i < nextTags.size(); i++) {
                    onTagViewAttachedListener.onTagViewAttached(getChildAt(i), i);
                }
            }
            reconcileSelection();
            return;
        }
        List<Integer> selected = nextTags.equals(mTags) ? getSelectedTagPositions() : new ArrayList<>();
        removeAllViews();
        mSelectedChildCount = 0;
        mCurrentSelected = null;
        mTags = nextTags;
        mLaidOutColumnCount = columns;
        for (int i = 0; i < mTags.size(); i++) {
            TagView tagView = addTag(mTags.get(i));
            if (onTagViewAttachedListener != null) {
                onTagViewAttachedListener.onTagViewAttached(tagView.getTagHost(), i);
            } else {
                tagView.setTagSelected(selected.contains(i));
            }
        }
        for (int i = 0; i < paddingCount; i++) addSpaceTag();
        reconcileSelection();
    }

    public static TagView toTagView(View view) {
        if (view instanceof TagView) {
            return (TagView) view;
        } else {
            return new TagView() {
                @Override
                public View getTagHost() {
                    return view;
                }

                @Override
                public TextView getTagText() {
                    TextView text = findFirstTextView(view);
                    if (text != null) return text;
                    throw new IllegalArgumentException("Tag layout must contain a TextView");
                }

                @Override
                public void setTagSelected(boolean selected) {
                    setSelectedState(view, selected);
                }

                @Override
                public boolean isTagSelected() {
                    return view.isSelected();
                }
            };
        }
    }

    public interface TagView {

        View getTagHost();

        TextView getTagText();

        void setTagSelected(boolean selected);

        boolean isTagSelected();
    }

    private TagView addTag(final String tagText) {
        View child;
        if (mTagResId != null) {
            child = mInflater.inflate(mTagResId, null);
        } else {
            TextView textView = new TextView(getContext());
            textView.setGravity(Gravity.CENTER);
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.END);
            child = textView;
        }
        final TagView tagView = toTagView(child);
        if (hasValue(mTagBackground)) {
            tagView.getTagHost().setBackgroundResource(mTagBackground);
        }
        if (hasValue(mTagTextSize)) {
            tagView.getTagText().setTextSize(TypedValue.COMPLEX_UNIT_PX, mTagTextSize);
        }
        if (hasValue(mTagTextColor)) {
            tagView.getTagText().setTextColor(mTagTextColor);
        }
        int paddingLeft = tagView.getTagHost().getPaddingLeft();
        int paddingRight = tagView.getTagHost().getPaddingRight();
        int paddingTop = tagView.getTagHost().getPaddingTop();
        int paddingBottom = tagView.getTagHost().getPaddingBottom();

        if (hasValue(mTagTextHorizontalPadding)) {
            paddingLeft = mTagTextHorizontalPadding;
            paddingRight = mTagTextHorizontalPadding;
        }
        if (hasValue(mTagTextVerticalPadding)) {
            paddingTop = mTagTextVerticalPadding;
            paddingBottom = mTagTextVerticalPadding;
        }
        tagView.getTagHost().setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        tagView.getTagText().setText(tagText);

        return addTagView(tagView);
    }

    private TagView addTagView(TagView tagView) {
        final int position = getChildCount();
        tagView.getTagHost().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = indexOfChild(tagView.getTagHost());
                if (!isTagPosition(position)) return;
                long version = mDataVersion;
                if (onItemClickListener != null) {
                    if (onItemClickListener.onItemClick(tagView, position)) {
                        return;
                    }
                }
                if (version != mDataVersion || indexOfChild(tagView.getTagHost()) != position) return;
                if (mTagSelectMode == SELECT_MODE_MULTIPLE) {
                    if (mSelectedChildCount >= mMaximumSelectionCount && !tagView.isTagSelected()) {
                        if (onSelectOutRangeListener != null) {
                            onSelectOutRangeListener.onSelectOutRange(TagGridLayout.this, tagView.getTagHost(), position);
                        }
                        return;
                    }
                    tagView.setTagSelected(!tagView.isTagSelected());
                    if (tagView.isTagSelected()) {
                        mSelectedChildCount++;
                    } else {
                        mSelectedChildCount--;
                    }
                    if (onSelectChangeListener != null) {
                        onSelectChangeListener.onSelectChange(TagGridLayout.this, tagView.getTagHost(), position, tagView.isTagSelected());
                    }
                } else if (mTagSelectMode == SELECT_MODE_SINGLE) {
                    if (mMaximumSelectionCount == 0) {
                        if (onSelectOutRangeListener != null) {
                            onSelectOutRangeListener.onSelectOutRange(TagGridLayout.this, tagView.getTagHost(), position);
                        }
                        return;
                    }
                    if (mCurrentSelected != null) {
                        mCurrentSelected.setTagSelected(false);
                    }
                    tagView.setTagSelected(true);
                    mSelectedChildCount = 1;
                    mCurrentSelected = tagView;
                    if (onSelectChangeListener != null) {
                        onSelectChangeListener.onSelectChange(TagGridLayout.this, tagView.getTagHost(), position, tagView.isTagSelected());
                    }

                }
            }
        });
        int column = position / getColumnCount();
        ViewGroup.LayoutParams vp = new ViewGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        LayoutParams params = new LayoutParams(vp);
        if (column > 0) {
            params.topMargin = mVerticalSpace;
        }
        int row = position % getColumnCount();
        if (row > 0) {
            params.leftMargin = mHorizontalSpace;
        }
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1.0f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1.0f);
        tagView.getTagHost().setLayoutParams(params);
        addView(tagView.getTagHost());
        return tagView;
    }

    public void selectTagPositions(Integer... selectedPos) {
        selectTagPositions(selectedPos == null ? null : Arrays.asList(selectedPos));
    }
    public void selectTags(List<String> tags) {
        List<Integer> positions = new ArrayList<>();
        if (tags != null) {
            for (int i = 0; i < mTags.size(); i++) {
                if (tags.contains(mTags.get(i))) positions.add(i);
            }
        }
        selectTagPositions(positions);
    }




    public void selectTagPositions(List<Integer> selectedPos) {
        for (int i = 0; i < getChildCount(); i++) {
            toTagView(getChildAt(i)).setTagSelected(isTagPosition(i)
                    && selectedPos != null && selectedPos.contains(i));
        }
        reconcileSelection();
    }
    public void unSelectAllTags() {
        selectTagPosition(-1);
    }
    public void selectTagPosition(int index) {
        if (index == -1) {
            selectTagPositions((List<Integer>) null);
            return;
        }
        if (!isTagPosition(index)) return;
        TagView tagView = toTagView(getChildAt(index));
        if (tagView.isTagSelected()) return;
        if (mTagSelectMode == SELECT_MODE_SINGLE && mMaximumSelectionCount > 0) {
            selectTagPositions((List<Integer>) null);
            tagView.setTagSelected(true);
        } else if (mTagSelectMode == SELECT_MODE_MULTIPLE && mSelectedChildCount < mMaximumSelectionCount) {
            tagView.setTagSelected(true);
        }
        reconcileSelection();
    }

    public void setMaximumSelectionCount(int maximumSelectionCount) {
        mMaximumSelectionCount = Math.max(0, maximumSelectionCount);
        reconcileSelection();
    }

    public TagView getCurrentSelected() {
        return mCurrentSelected;
    }

    public List<Integer> getSelectedTagPositions() {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            if (isTagPosition(i) && toTagView(getChildAt(i)).isTagSelected()) selected.add(i);
        }
        return selected;
    }

    private boolean isTagPosition(int position) {
        return position >= 0 && position < mTags.size() && position < getChildCount()
                && getChildAt(position).getVisibility() == View.VISIBLE;
    }

    private void reconcileSelection() {
        mSelectedChildCount = 0;
        mCurrentSelected = null;
        int limit = mTagSelectMode == SELECT_MODE_SINGLE ? Math.min(1, mMaximumSelectionCount) : mMaximumSelectionCount;
        for (int i = 0; i < getChildCount(); i++) {
            TagView tag = toTagView(getChildAt(i));
            if (!isTagPosition(i) || mSelectedChildCount >= limit) tag.setTagSelected(false);
            if (tag.isTagSelected()) {
                mSelectedChildCount++;
                if (mTagSelectMode == SELECT_MODE_SINGLE) mCurrentSelected = tag;
            }
        }
    }

    private static void setSelectedState(View view, boolean selected) {
        view.setSelected(selected);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setSelectedState(group.getChildAt(i), selected);
        }
    }

    private static TextView findFirstTextView(View view) {
        if (view instanceof TextView) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView text = findFirstTextView(group.getChildAt(i));
                if (text != null) return text;
            }
        }
        return null;
    }

    private boolean hasValue(Object value) {
        return value != null;
    }


    public List<String> getTags() {
        return new ArrayList<>(mTags);
    }
}
