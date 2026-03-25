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


import com.nhsd.basic.utils.Logger;
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
    private List<String> mTags;

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
    }

    public void setTagSelectMode(int tagSelectMode) {
        this.mTagSelectMode = tagSelectMode;
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
        setTags(Arrays.asList(tagsList),null);
    }
    public  void setTags(List<String> tagsList){
        setTags(tagsList,null);
    }
    public <T> void setTags(List<String> tagsList, OnTagViewAttachedListener<T> onTagViewAttachedListener) {
        removeAllViews();
        if (tagsList != null && tagsList.size() > 0) {
            for (int i = 0; i < tagsList.size(); i++) {
                String tag=tagsList.get(i);
                TagView tagView = addTag(tag);
                if (onTagViewAttachedListener != null) {
                    onTagViewAttachedListener.onTagViewAttached(tagView.getTagHost(), i);
                }
            }
        }

        if (tagsList != null && tagsList.size() % getColumnCount() != 0) {
            int spaceCount = getColumnCount() - tagsList.size() % getColumnCount();
            for(int i=0;i<spaceCount;i++) {
                addSpaceTag();
            }
        }
        this.mTags = tagsList;
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
                    if (view instanceof TextView) {
                        return (TextView) view;
                    }
                    if (view instanceof ViewGroup) {
                        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                            View child = ((ViewGroup) view).getChildAt(i);
                            if (child instanceof TextView) {
                                return (TextView) child;
                            }
                        }
                    }
                    throw new NullPointerException("can not find TextView in " + view);
                }

                @Override
                public void setTagSelected(boolean selected) {
                    if (view != null) {
                        view.setSelected(selected);
                        if (view instanceof ViewGroup) {
                            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                                View child = ((ViewGroup) view).getChildAt(i);
                                child.setSelected(selected);
                            }
                        }
                    }
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
                if (tagView.getTagHost().getVisibility() != View.VISIBLE)
                    return;
                if (onItemClickListener != null) {
                    if (onItemClickListener.onItemClick(tagView, position)) {
                        return;
                    }
                }
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
        selectTagPositions(Arrays.asList(selectedPos));
    }
    public void selectTags(List<String> tags) {
        if(this.mTags==null){
            Logger.i("call filter tags is null");
            return;
        }
        Logger.i("call filter= selected tags="+tags+", this tags="+mTags);
        ArrayList<Integer> selectedPos=new ArrayList<>();
        if(tags!=null) {
            for (int i = 0; i < mTags.size(); i++) {
                if (tags.contains(mTags.get(i))) {
                    selectedPos.add(i);
                }
            }
        }
        selectTagPositions(selectedPos);
    }




    public void selectTagPositions(List<Integer> selectedPos) {
        if (selectedPos.size()>1&&mTagSelectMode == SELECT_MODE_SINGLE) {
//            throw new UnsupportedOperationException("select at most 1 tag in SELECT_MODE_SINGLE!");
            return;
        }
        int childCount = getChildCount();
        int selectedCount = 0;
        for (int i = 0; i < childCount; i++) {
            TagView tagView = toTagView(getChildAt(i));
            if (selectedPos.contains(i)) {
                tagView.setTagSelected(true);
                if(mTagSelectMode==SELECT_MODE_SINGLE){
                    mCurrentSelected=tagView;
                }
                selectedCount++;
            } else {
                tagView.setTagSelected(false);
            }
        }
        mSelectedChildCount = selectedCount;
    }
    public void unSelectAllTags() {
        selectTagPosition(-1);
    }
    public void selectTagPosition(int index) {
        if (index == -1) {
            if (mTagSelectMode == SELECT_MODE_SINGLE) {
                if (mCurrentSelected != null) {
                    mCurrentSelected.setTagSelected(false);
                }
                mCurrentSelected = null;
                mSelectedChildCount = 0;
            }else {
                if (mSelectedChildCount > 0) {
                    for (int i = 0; i < getChildCount(); i++) {
                        TagView tagView = toTagView(getChildAt(i));
                        tagView.setTagSelected(false);
                    }
                    mSelectedChildCount = 0;
                }
            }
        } else {
            TagView tagView = toTagView(getChildAt(index));
            if (mTagSelectMode == SELECT_MODE_SINGLE) {
                if (mCurrentSelected != tagView) {
                    if (mCurrentSelected != null) {
                        mCurrentSelected.setTagSelected(false);
                    }
                    mCurrentSelected = tagView;
                    mCurrentSelected.setTagSelected(true);
                    mSelectedChildCount = 1;
                }
            }else if (mTagSelectMode == SELECT_MODE_MULTIPLE) {
                if (mSelectedChildCount < mMaximumSelectionCount) {
                    tagView.setTagSelected(true);
                    mSelectedChildCount++;
                }
            }
        }
    }

    public void setMaximumSelectionCount(int maximumSelectionCount) {
        this.mMaximumSelectionCount = maximumSelectionCount;
    }

    public TagView getCurrentSelected() {
        return mCurrentSelected;
    }

    public List<Integer> getSelectedTagPositions() {
        List<Integer> selectedList = new ArrayList<>();
        if (mTagSelectMode == SELECT_MODE_SINGLE && mCurrentSelected != null) {
            selectedList.add(indexOfChild(mCurrentSelected.getTagHost()));
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                if (getChildAt(i).getVisibility() == View.VISIBLE && getChildAt(i).isSelected()) {
                    selectedList.add(i);
                }
            }
        }
        return selectedList;
    }

    private boolean hasValue(Object value) {
        return value != null;
    }


    public List<String> getTags() {
        return mTags;
    }
}
