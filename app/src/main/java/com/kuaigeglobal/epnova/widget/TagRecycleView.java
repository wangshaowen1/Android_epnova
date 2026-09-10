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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;


@SuppressWarnings("RestrictedApi")
public class TagRecycleView extends FrameLayout {
    private int tagTextId;
    private int tagDeleteId;
    private int tagResId = R.layout.item_selected_tag;
    private final ArrayList<String> tags = new ArrayList<>();

    private final Set<Integer> selectedPositions = new HashSet<>();
    private long dataVersion;
    private int maxChildCount = 8;
    private OnTagChangedListener onTagChangedListener;

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
    private boolean spaceIncludeEdge;
    private final Runnable scrollToEndAction = () -> {
        if (!tags.isEmpty()) tagsView.scrollToPosition(tags.size() - 1);
    };

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
        int count = tags.size();
        if (count == 0) return;
        dataVersion++;
        tags.clear();
        selectedPositions.clear();
        removeCallbacks(scrollToEndAction);
        adapter.notifyItemRangeRemoved(0, count);
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
        try {
            maxChildCount = Math.max(0, a.getInt(R.styleable.TagRecycleView_max_child_count, 8));
            tagResId = a.getResourceId(R.styleable.TagRecycleView_tagResId, R.layout.item_selected_tag);
            tagTextId = a.getResourceId(R.styleable.TagRecycleView_tagTextId, R.id.text);
            tagDeleteId = a.getResourceId(R.styleable.TagRecycleView_tagDeleteId, R.id.delete);
            horizontalSpace = a.getDimensionPixelOffset(R.styleable.TagRecycleView_horizontalSpace, 0);
            verticalSpace = a.getDimensionPixelOffset(R.styleable.TagRecycleView_verticalSpace, 0);
            columnCount = Math.max(1, a.getInt(R.styleable.TagRecycleView_columnCount, 1));
            orientation = a.getInt(R.styleable.TagRecycleView_orientation, HORIZONTAL);
            maximumSelectionCount = Math.max(0, a.getInteger(R.styleable.TagRecycleView_maximumSelectionCount, Integer.MAX_VALUE));
            spaceIncludeEdge = a.getBoolean(R.styleable.TagRecycleView_spaceIncludeEdge, false);
            tagSelectMode = a.getInt(R.styleable.TagRecycleView_tagSelectMode, SELECT_MODE_NONE);
        } finally {
            a.recycle();
        }
        if (tagsView != null) removeView(tagsView);
        dataVersion++;
        selectedPositions.clear();
        if (tags.size() > maxChildCount) tags.subList(maxChildCount, tags.size()).clear();
        tagsView = new RecyclerView(getContext());
        addView(tagsView);
        if (orientation == HORIZONTAL) {
            tagsView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            tagsView.addItemDecoration(new SpaceDecoration(horizontalSpace, RecyclerView.HORIZONTAL,
                    spaceIncludeEdge ? SpaceDecoration.Direction.BOTH : SpaceDecoration.Direction.MIDDLE));
        } else {
            tagsView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
            tagsView.addItemDecoration(new GridSpacingItemDecoration(columnCount, horizontalSpace, verticalSpace,
                    spaceIncludeEdge ? GridSpacingItemDecoration.Direction.All : GridSpacingItemDecoration.Direction.Middle));
        }
        tagsView.setItemAnimator(new NoAlphaItemAnimator());
        tagsView.setAdapter(adapter = new SlideInLeftAnimationAdapter(new TagsAdapter(getContext())));
    }


    public void scrollToPosition(int position) {
        post(() -> {
            if (position >= 0 && position < tags.size()) tagsView.scrollToPosition(position);
        });
    }

    public void addTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        int start = this.tags.size();
        int count = Math.min(tags.size(), Math.max(0, maxChildCount - start));
        if (count == 0) return;
        List<String> added = new ArrayList<>(tags.subList(0, count));
        dataVersion++;
        this.tags.addAll(added);
        adapter.notifyItemRangeInserted(start, count);
        // 末行留白依赖总数量，局部通知不会使位置未变的旧行间距缓存失效。
        tagsView.invalidateItemDecorations();
        scrollToEnd();
        for (String tag : added) notifyTagAdded(tag);
    }
    public void setTags(String... tags) {
        setTags(tags == null ? null : Arrays.asList(tags));

    }
    public void setTags(@Nullable List<String> tags) {
        List<String> nextTags = tags == null ? new ArrayList<>()
                : new ArrayList<>(tags.subList(0, Math.min(tags.size(), maxChildCount)));
        if (!this.tags.equals(nextTags)) {
            dataVersion++;
            this.tags.clear();
            this.tags.addAll(nextTags);
            selectedPositions.clear();
            adapter.notifyDataSetChanged();
        }
        scrollToEnd();
        // 保留原有 setTags 的逐项添加通知，通知前先完成数据与 Adapter 同步。
        for (String tag : nextTags) notifyTagAdded(tag);
    }

    public void add(String tag) {
        addTags(Arrays.asList(tag));
    }

    private int tagSize() {
        return tags.size();
    }

    private void scrollToEnd() {
        removeCallbacks(scrollToEndAction);
        if (!tags.isEmpty()) post(scrollToEndAction);
    }

    public ArrayList<String> getTags() {
        return new ArrayList<>(tags);
    }


    public void setOnTagChangedListener(OnTagChangedListener onTagChangedListener) {
        this.onTagChangedListener = onTagChangedListener;
    }

    private void notifyTagAdded(String tag) {
        if (onTagChangedListener != null)
            onTagChangedListener.onTagAdded(tag);
    }

    public void remove(String tag) {
        removeAt(tags.indexOf(tag));
    }


    private String removeAt(int position) {
        if (position < 0 || position >= tags.size()) return null;
        dataVersion++;
        String removed = tags.remove(position);
        Set<Integer> shifted = new HashSet<>();
        for (int selected : selectedPositions) {
            if (selected != position) shifted.add(selected > position ? selected - 1 : selected);
        }
        selectedPositions.clear();
        selectedPositions.addAll(shifted);
        adapter.notifyItemRemoved(position);
        tagsView.invalidateItemDecorations();
        notifyTagRemoved(removed);
        return removed;
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(scrollToEndAction);
        super.onDetachedFromWindow();
    }

    private static void setSelectedState(View view, boolean selected) {
        view.setSelected(selected);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setSelectedState(group.getChildAt(i), selected);
        }
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
            private final TextView tagTextView;

            public ViewHolder() {
                // 保持旧实现由 LayoutManager 决定根尺寸，避免横向标签突然按 match_parent 铺满。
                super(LayoutInflater.from(context).inflate(tagResId, null));
                tagTextView = itemView.findViewById(tagTextId);
                if (tagTextView == null) {
                    throw new IllegalArgumentException("Tag layout must contain the configured TextView");
                }
                View delete = itemView.findViewById(tagDeleteId);
                if (delete != null) {
                    delete.setOnClickListener(v -> {
                        int position = getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION || position >= tags.size()) return;
                        String removed = removeAt(position);
                        if (onTagRemoveListener != null) {
                            onTagRemoveListener.onTagRemoved(itemView, position, removed);
                        }
                    });
                }
                itemView.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position == RecyclerView.NO_POSITION || position >= tags.size()) return;
                    long version = dataVersion;
                    if (onItemClickListener != null && onItemClickListener.onItemClick(tagTextView, position)) return;
                    if (version != dataVersion || getBindingAdapterPosition() != position) return;
                    if (tagSelectMode == SELECT_MODE_NONE) return;
                    boolean selected = selectedPositions.contains(position);
                    if (!selected && (maximumSelectionCount == 0
                            || (tagSelectMode == SELECT_MODE_MULTIPLE && selectedPositions.size() >= maximumSelectionCount))) {
                        if (onSelectOutRangeListener != null) onSelectOutRangeListener.onSelectOutRange(tagTextView, position);
                        return;
                    }
                    if (tagSelectMode == SELECT_MODE_SINGLE) {
                        Set<Integer> previous = new HashSet<>(selectedPositions);
                        selectedPositions.clear();
                        selectedPositions.add(position);
                        for (int oldPosition : previous) {
                            if (oldPosition != position) {
                                RecyclerView.ViewHolder oldHolder = tagsView.findViewHolderForAdapterPosition(oldPosition);
                                if (oldHolder != null) setSelectedState(oldHolder.itemView, false);
                                adapter.notifyItemChanged(oldPosition);
                            }
                        }
                    } else if (tagSelectMode == SELECT_MODE_MULTIPLE) {
                        if (selected) selectedPositions.remove(position);
                        else selectedPositions.add(position);
                    } else {
                        return;
                    }
                    setSelectedState(itemView, selectedPositions.contains(position));
                    if (onSelectChangeListener != null) {
                        onSelectChangeListener.onSelectChange(itemView, position, itemView.isSelected());
                    }
                });
            }

            public void bind(int position) {
                tagTextView.setText(tags.get(position));
                setSelectedState(itemView, tagSelectMode != SELECT_MODE_NONE && selectedPositions.contains(position));
            }
        }
    }
}
