package com.kuaigeglobal.epnova.widget;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.SizeUtils;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.feature.PhotoPagerActivity;
import com.kuaige.imageloader.GlideImageLoader;
import com.nhsd.basic.ext.ViewUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 少量图片的编辑网格。数据接口在主线程调用，数量上限不包含添加按钮。 */
public class PhotoGridView extends GridLayout {
    private final ArrayList<Uri> photos = new ArrayList<>();
    private final ArrayList<View> photoViews = new ArrayList<>();
    private int maxPhotoCount;
    private int itemLayoutResId;
    private int configuredWidth;
    private int configuredHeight;
    private int dividerSpace;
    private int deleteButtonSrcId;
    private boolean autoHideAddButton;
    private Button addButton;
    private OnClickListener onAddButtonClickedListener;
    private OnPhotosChangedListener onPhotosChangedListener;

    public PhotoGridView(Context context) {
        this(context, null);
    }

    public PhotoGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PhotoGridView,
                defStyleAttr, 0);
        boolean addButtonVisible;
        int addButtonSrcId;
        try {
            maxPhotoCount = Math.max(0, a.getInt(R.styleable.PhotoGridView_max_child_count, 8));
            itemLayoutResId = a.getResourceId(R.styleable.PhotoGridView_item_layout_resource, R.layout.item_photo);
            configuredWidth = Math.max(0, a.getDimensionPixelOffset(R.styleable.PhotoGridView_child_width, 0));
            configuredHeight = Math.max(0, a.getDimensionPixelOffset(R.styleable.PhotoGridView_child_height, 0));
            dividerSpace = Math.max(0, a.getDimensionPixelOffset(R.styleable.PhotoGridView_divider_space, SizeUtils.dp2px(10)));
            deleteButtonSrcId = a.getResourceId(R.styleable.PhotoGridView_delete_button_src, 0);
            autoHideAddButton = a.getBoolean(R.styleable.PhotoGridView_add_button_auto_hide, true);
            addButtonVisible = a.getBoolean(R.styleable.PhotoGridView_add_button_visible, true);
            addButtonSrcId = a.getResourceId(R.styleable.PhotoGridView_add_button_src, R.drawable.icon_feedback_addpicture);
        } finally {
            a.recycle();
        }
        if (addButtonVisible) {
            addButton = new Button(context);
            addButton.setId(R.id.add_photo);
            addButton.setBackgroundResource(addButtonSrcId);
            addButton.setForeground(context.getDrawable(R.drawable.selector_item_background));
            ViewUtils.setOnSingleClickListener(addButton, view -> {
                if (onAddButtonClickedListener != null) {
                    onAddButtonClickedListener.onClick(view);
                }
            });
        }
        updateAddButton();
        setLayoutTransition(new LayoutTransition());
    }

    /** 完整替换；忽略 null/空 URI，按输入顺序截取上限，保留重复 URI。 */
    public void setPhotos(@Nullable List<Uri> uris) {
        ArrayList<Uri> next = new ArrayList<>();
        if (uris != null) {
            for (Uri uri : uris) {
                if (next.size() >= maxPhotoCount) break;
                if (uri != null && !uri.toString().trim().isEmpty()) next.add(uri);
            }
        }
        if (photos.equals(next)) return;

        // 按出现次数匹配旧 View，删除、调序和重复 URI 都不会重新加载未变化的图片。
        ArrayList<View> unused = new ArrayList<>(photoViews);
        ArrayList<View> nextViews = new ArrayList<>();
        for (Uri uri : next) {
            View reused = null;
            for (int i = 0; i < unused.size(); i++) {
                if (uri.equals(unused.get(i).getTag())) {
                    reused = unused.remove(i);
                    break;
                }
            }
            nextViews.add(reused != null ? reused : createPhotoView(uri));
        }
        applyPhotos(next, nextViews);
    }

    private void applyPhotos(ArrayList<Uri> next, ArrayList<View> nextViews) {
        ArrayList<View> removed = new ArrayList<>(photoViews);
        removed.removeAll(nextViews);
        photos.clear();
        photos.addAll(next);
        photoViews.clear();
        photoViews.addAll(nextViews);
        for (View view : removed) removeView(view);
        for (int i = 0; i < nextViews.size(); i++) {
            View view = nextViews.get(i);
            if (indexOfChild(view) != i) {
                if (view.getParent() == this) removeView(view);
                addView(view, i, createLayoutParams(i, configuredWidth));
            }
        }
        updateAddButton();
        requestLayout();
        if (onPhotosChangedListener != null) {
            onPhotosChangedListener.onPhotosChanged(getPhotos());
        }
    }

    public void addPhoto(@Nullable Uri uri) {
        addPhotos(Collections.singletonList(uri));
    }

    /** 服务端图片地址的完整替换入口，与 setPhotos 使用相同的过滤和容量规则。 */
    public void setPhotoUrls(@Nullable List<String> urls) {
        ArrayList<Uri> next = new ArrayList<>();
        if (urls != null) {
            for (String url : urls) {
                if (next.size() >= maxPhotoCount) break;
                if (url != null && !url.trim().isEmpty()) next.add(Uri.parse(url));
            }
        }
        setPhotos(next);
    }

    /** 追加只占用剩余容量，不修改调用方列表。 */
    public void addPhotos(@Nullable List<Uri> uris) {
        if (uris == null || photos.size() >= maxPhotoCount) return;
        ArrayList<Uri> next = getPhotos();
        for (Uri uri : uris) {
            if (next.size() >= maxPhotoCount) break;
            if (uri != null && !uri.toString().trim().isEmpty()) next.add(uri);
        }
        setPhotos(next);
    }

    /** 按位置删除，重复 URI 也只移除指定的一项。 */
    public void removePhotoAt(int position) {
        if (position < 0 || position >= photos.size()) return;
        ArrayList<Uri> next = getPhotos();
        next.remove(position);
        ArrayList<View> nextViews = new ArrayList<>(photoViews);
        nextViews.remove(position);
        applyPhotos(next, nextViews);
    }

    /** 删除首个匹配的 URI；需要区分重复项时使用 removePhotoAt。 */
    public void removePhoto(@Nullable Uri uri) {
        removePhotoAt(photos.indexOf(uri));
    }

    public void clearPhotos() {
        setPhotos(Collections.emptyList());
    }

    /** 始终返回非 null 快照，外部修改不会影响控件。 */
    @NonNull
    public ArrayList<Uri> getPhotos() {
        return new ArrayList<>(photos);
    }

    public int getPhotoCount() {
        return photos.size();
    }

    public int getMaxPhotoCount() {
        return maxPhotoCount;
    }

    public void setOnAddButtonClickedListener(@Nullable OnClickListener listener) {
        onAddButtonClickedListener = listener;
    }

    /** 注册时同步当前快照；随后每次有效变更完成后回调一次，包括清空和调序。 */
    public void setOnPhotosChangedListener(@Nullable OnPhotosChangedListener listener) {
        onPhotosChangedListener = listener;
        if (listener != null) listener.onPhotosChanged(getPhotos());
    }

    public interface OnPhotosChangedListener {
        void onPhotosChanged(@NonNull List<Uri> photos);
    }

    private View createPhotoView(Uri uri) {
        View itemView = LayoutInflater.from(getContext()).inflate(itemLayoutResId, this, false);
        ImageView image = itemView.findViewById(R.id.grid_item_picture);
        View delete = itemView.findViewById(R.id.grid_item_delete_button);
        ImageView deleteIcon = itemView.findViewById(R.id.grid_item_delete_icon);
        if (image == null || (deleteButtonSrcId != 0 && (delete == null || deleteIcon == null))) {
            throw new IllegalArgumentException("PhotoGridView item layout requires grid_item_picture; "
                    + "deletion also requires grid_item_delete_button and grid_item_delete_icon");
        }
        itemView.setTag(uri);
        GlideImageLoader.with(image).display(uri, R.drawable.ic_gallery_v21);
        if (delete != null) {
            delete.setVisibility(deleteButtonSrcId != 0 ? VISIBLE : GONE);
            if (deleteButtonSrcId != 0) deleteIcon.setImageResource(deleteButtonSrcId);
            delete.setOnClickListener(view -> removePhotoAt(photoViews.indexOf(itemView)));
        }
        itemView.setOnClickListener(view -> {
            int position = photoViews.indexOf(itemView);
            if (position < 0 || position >= photos.size()) return;
            ArrayList<String> urls = new ArrayList<>();
            for (Uri photo : photos) urls.add(photo.toString());
            PhotoPagerActivity.launch(getContext(), this, urls, position,
                    (index, child) -> child == addButton ? null : child.findViewById(R.id.grid_item_picture));
        });
        return itemView;
    }

    private void updateAddButton() {
        if (addButton == null) return;
        boolean show = !autoHideAddButton || photos.size() < maxPhotoCount;
        // 退场动画期间 parent 仍可能是 this；以真实子项索引判断是否需要重新加入。
        // addView 会先取消退场动画，完成旧 parent 的释放。
        if (show && indexOfChild(addButton) < 0) {
            addView(addButton, createLayoutParams(photos.size(), configuredWidth));
        } else if (!show && indexOfChild(addButton) >= 0) {
            removeView(addButton);
        }
    }

    @Override
    public void setColumnCount(int columnCount) {
        // GridLayout 构造期间也会调用此方法；未配置列数时统一使用四列。
        int columns = columnCount == UNDEFINED ? 4 : Math.max(1, columnCount);
        if (getColumnCount() == columns) return;
        // 先清除旧位置，避免缩减列数时 GridLayout 拒绝越界的旧 columnSpec。
        for (int i = 0; i < getChildCount(); i++) {
            LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
            params.columnSpec = spec(UNDEFINED);
            params.rowSpec = spec(UNDEFINED);
            getChildAt(i).setLayoutParams(params);
        }
        super.setColumnCount(columns);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = configuredWidth;
        if (width == 0) {
            if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) {
                // 无宽度约束时先取得子项自然尺寸，再统一为网格尺寸。
                updateItemLayoutParams(LayoutParams.WRAP_CONTENT);
                super.onMeasure(widthSpec, heightSpec);
                width = configuredHeight;
                for (int i = 0; i < getChildCount(); i++) {
                    width = Math.max(width, getChildAt(i).getMeasuredWidth());
                }
            } else {
                int available = Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
                long gaps = (long) dividerSpace * (getColumnCount() - 1);
                width = (int) Math.max(0, (available - gaps) / getColumnCount());
            }
        }
        // 在父类测量前更新，首帧、宽度变化和删除后的行高都使用当前尺寸。
        updateItemLayoutParams(width);
        super.onMeasure(widthSpec, heightSpec);
    }

    private void updateItemLayoutParams(int width) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams next = createLayoutParams(i, width);
            LayoutParams old = (LayoutParams) child.getLayoutParams();
            if (old.width != next.width || old.height != next.height
                    || old.getMarginStart() != next.getMarginStart() || old.getMarginEnd() != 0
                    || old.topMargin != next.topMargin || old.bottomMargin != 0
                    || !old.rowSpec.equals(next.rowSpec) || !old.columnSpec.equals(next.columnSpec)) {
                child.setLayoutParams(next);
            }
        }
    }

    private LayoutParams createLayoutParams(int index, int width) {
        int columns = getColumnCount();
        LayoutParams params = new LayoutParams(spec(index / columns), spec(index % columns));
        params.width = width;
        params.height = configuredHeight > 0 ? configuredHeight : width;
        params.setMarginStart(index % columns == 0 ? 0 : dividerSpace);
        params.setMarginEnd(0);
        params.topMargin = index >= columns ? dividerSpace : 0;
        params.bottomMargin = 0;
        return params;
    }
}
