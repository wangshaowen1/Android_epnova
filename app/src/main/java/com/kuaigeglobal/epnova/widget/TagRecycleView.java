package com.kuaigeglobal.epnova.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration;
import com.kuaigeglobal.epnova.widget.decoration.SpaceDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;


@SuppressWarnings("RestrictedApi")
public class TagRecycleView extends FrameLayout {
    private int tagTextId;
    private int tagDeleteId;
    private int tagResId = R.layout.item_selected_tag;
    private ArrayList<String> tags = new ArrayList<>();

    private ArrayList<String> selectedTags = new ArrayList<>();
    private int maxChildCount = 8;
    private OnTagChangedListener onTagChangedListener;
    private boolean deleteButtonVisible;

    private RecyclerView tagsView;
    private RecyclerView.Adapter adapter;
    private int horizontalSpace;
    private int verticalSpace;
    private int columnCount;
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;
    private int orientation = HORIZONTAL;
    private int maximumSelectionCount = Integer.MAX_VALUE;
    private static final int SELECT_MODE_NONE = 0;
    private static final int SELECT_MODE_SINGLE = 1;
    private static final int SELECT_MODE_MULTIPLE = 2;
    private int tagSelectMode = SELECT_MODE_NONE;
    private View currentSelected;
    private boolean spaceIncludeEdge;

    public TagRecycleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public TagRecycleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TagRecycleView(Context context) {
        super(context);
        init(null);
    }


    public void clear() {
        tags.clear();
        adapter.notifyDataSetChanged();
    }

    private OnSelectOutRangeListener onSelectOutRangeListener;
    private OnSelectChangeListener onSelectChangeListener;

    private OnTagRemoveListener onTagRemoveListener;
    private OnItemClickListener onItemClickListener;

    public interface OnTagChangedListener {
        void onTagAdded(String tag);

        void onTagRemoved(String tag);
    }

    public interface OnSelectOutRangeListener {
        void onSelectOutRange(View child, int index);
    }

    public interface OnSelectChangeListener {
        void onSelectChange(View child, int index, boolean isSelected);
    }
    public interface OnTagRemoveListener {
        void onTagRemoved(View child, int index,String tag);
    }
    public interface OnItemClickListener {
        boolean onItemClick(View child, int index);
    }

    public void setOnSelectOutRangeListener(OnSelectOutRangeListener onSelectOutRangeListener) {
        this.onSelectOutRangeListener = onSelectOutRangeListener;
    }

    public void setOnSelectChangeListener(OnSelectChangeListener onSelectChangeListener) {
        this.onSelectChangeListener = onSelectChangeListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnTagRemoveListener(OnTagRemoveListener onTagRemoveListener) {
        this.onTagRemoveListener = onTagRemoveListener;
    }

    public void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TagRecycleView);
        maxChildCount = a.getInt(R.styleable.TagRecycleView_max_child_count, 8);
        tagResId = a.getResourceId(R.styleable.TagRecycleView_tagResId, R.layout.item_selected_tag);
        tagTextId = a.getResourceId(R.styleable.TagRecycleView_tagTextId, R.id.text);
        tagDeleteId = a.getResourceId(R.styleable.TagRecycleView_tagDeleteId, R.id.delete);
        horizontalSpace = a.getDimensionPixelOffset(R.styleable.TagRecycleView_horizontalSpace, 0);
        verticalSpace = a.getDimensionPixelOffset(R.styleable.TagRecycleView_verticalSpace, 0);
        columnCount = a.getInt(R.styleable.TagRecycleView_columnCount, 0);
        orientation = a.getInt(R.styleable.TagRecycleView_orientation, HORIZONTAL);
        maximumSelectionCount = a.getInteger(R.styleable.TagLayout_maximumSelectionCount, Integer.MAX_VALUE);
        spaceIncludeEdge=a.getBoolean(R.styleable.TagRecycleView_spaceIncludeEdge,false);
        tagSelectMode = a.getInt(R.styleable.TagLayout_tagSelectMode, SELECT_MODE_NONE);
        tagsView = new RecyclerView(getContext());
        addView(tagsView);
        if (orientation == HORIZONTAL) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            tagsView.setLayoutManager(linearLayoutManager);
            tagsView.addItemDecoration(new SpaceDecoration(horizontalSpace, RecyclerView.HORIZONTAL,
                    spaceIncludeEdge ? SpaceDecoration.Direction.BOTH : SpaceDecoration.Direction.MIDDLE));
        } else {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), columnCount);
            tagsView.setLayoutManager(gridLayoutManager);
            tagsView.addItemDecoration(new GridSpacingItemDecoration(columnCount, horizontalSpace, verticalSpace,spaceIncludeEdge? GridSpacingItemDecoration.Direction.All:GridSpacingItemDecoration.Direction.Middle));
        }
        tagsView.setItemAnimator(new NoAlphaItemAnimator());
        tagsView.setAdapter(adapter = new SlideInLeftAnimationAdapter(new TagsAdapter(getContext())));
    }


    public void scrollToPosition(int position){
        post(() -> tagsView.scrollToPosition(position));
    }

    public void addTags(List<String> tags) {
        int storage = maxChildCount;
        int max = Math.min(tags.size(), storage - tagsView.getChildCount());
        for (int i = 0; i < max; i++) {
            String tag = tags.get(i);
            addToList(tag);
        }
        adapter.notifyDataSetChanged();
        scrollToEnd();
    }
    public void setTags(String... tags) {
        setTags(Arrays.asList(tags));

    }
    public void setTags(@Nullable List<String> tags) {
        this.tags.clear();
        if (tags != null) {
            int max = Math.min(tags.size(), maxChildCount);
            for (int i = 0; i < max; i++) {
                String tag = tags.get(i);
                addToList(tag);
            }
        }
        adapter.notifyDataSetChanged();
        scrollToEnd();
    }

    public void add(String tag) {
        int index = tags.size();
        addToList(tag);
        adapter.notifyItemInserted(index);
        adapter.notifyDataSetChanged();
        scrollToEnd();
    }

    private void addToList(String tag) {
        tags.add(tag);
        notifyTagAdded(tag);
    }

    private int tagSize() {
        return tags.size();
    }

    private void scrollToEnd() {
        post(() -> tagsView.scrollToPosition(tagSize() - 1));
    }

    public ArrayList<String> getTags() {
        return tags;
    }


    public void setOnTagChangedListener(OnTagChangedListener onTagChangedListener) {
        this.onTagChangedListener = onTagChangedListener;
    }

    private void notifyTagAdded(String tag) {
        if (onTagChangedListener != null)
            onTagChangedListener.onTagAdded(tag);
    }

    public void remove(String tag) {
        tags.remove(tag);
        notifyTagRemoved(tag);
        adapter.notifyDataSetChanged();
    }


    private void notifyTagRemoved(String tag) {
        if (onTagChangedListener != null)
            onTagChangedListener.onTagRemoved(tag);
    }

    public class TagsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context context;

        public TagsAdapter(Context context) {
            this.context = context;
        }


        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder();
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.bind(position);
        }

        @Override
        public int getItemCount() {
            return tagSize();
        }


        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tagTextView;
            View delete;

            public ViewHolder() {
                super(LayoutInflater.from(context).inflate(tagResId, null));
                tagTextView = itemView.findViewById(tagTextId);
                delete = itemView.findViewById(tagDeleteId);
                delete.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (tags.size() > position) {
                        String tag=tags.get(position);
                        remove(tag);
                        if(onTagRemoveListener!=null){
                            onTagRemoveListener.onTagRemoved(itemView,position,tag);
                        }
                    }

                });
            }

            public void bind(int position) {
                String tag = tags.get(position);
                tagTextView.setText(tag);
                if (tagSelectMode == SELECT_MODE_MULTIPLE) {
                    setItemViewSelected(selectedTags.contains(tag));
                }
                itemView.setOnClickListener(v -> {
                    if (onItemClickListener != null) {
                        if(onItemClickListener.onItemClick(tagTextView, position)){
                            return;
                        }
                    }
                    if (tagSelectMode == SELECT_MODE_MULTIPLE) {
                        if (selectedTags.size() >= maximumSelectionCount && !itemView.isSelected()) {
                            if (onSelectOutRangeListener != null) {
                                onSelectOutRangeListener.onSelectOutRange(tagTextView, position);
                            }
                            return;
                        }
                        setItemViewSelected(!itemView.isSelected());
                        if (itemView.isSelected()) {
                            selectedTags.add(tag);
                        } else {
                            selectedTags.remove(tag);
                        }
                        if (onSelectChangeListener != null) {
                            onSelectChangeListener.onSelectChange(itemView, position, itemView.isSelected());
                        }
                    } else if (tagSelectMode == SELECT_MODE_SINGLE) {
                        if (currentSelected != null) {
                            currentSelected.setSelected(false);
                        }
                        setItemViewSelected(true);
                        currentSelected = itemView;
                        if (onSelectChangeListener != null) {
                            onSelectChangeListener.onSelectChange(itemView, position, itemView.isSelected());
                        }

                    }
                });

            }

            public void setItemViewSelected(boolean selected) {
                itemView.setSelected(selected);
                if (itemView instanceof ViewGroup) {
                    for (int i = 0; i < ((ViewGroup) itemView).getChildCount(); i++) {
                        ((ViewGroup) itemView).getChildAt(i).setSelected(selected);
                    }
                }
            }
        }
    }


}