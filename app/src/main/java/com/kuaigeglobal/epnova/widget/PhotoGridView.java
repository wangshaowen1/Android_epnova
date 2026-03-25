package com.kuaigeglobal.epnova.widget;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.blankj.utilcode.util.SizeUtils;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.common.utils.Utils;
import com.kuaigeglobal.epnova.feature.PhotoPagerActivity;
import com.kuaigeglobal.epnova.widget.roundedimageview.RoundedImageView;
import com.kuaige.imageloader.GlideImageLoader;
import com.nhsd.basic.ext.ViewUtils;
import com.nhsd.basic.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;



@SuppressWarnings("RestrictedApi")
public class PhotoGridView extends GridLayout {
    private static final int NULL = 0;
    private LayoutTransition mTransition;
    private int itemLayoutResId = R.layout.item_photo;
    private Activity mContext;
    private ArrayList<Uri> photoUris = new ArrayList<>();
    private int maxChildCount = 8;
    private OnPhotoChangedListener onPhotoChangedListener;
    private Button addButton;
    private int itemWidth;
    private int dividerSpace = SizeUtils.dp2px(10);
    private int addButtonSrcId = R.drawable.icon_feedback_addpicture;
    private int deleteButtonSrcId;
    private boolean deleteButtonVisible;
    private boolean addButtonVisible = true;
    private boolean autoHide = true;
    private int itemHeight;
    private boolean autoWith = false;

    public ArrayList<Uri> setAll(List<String> urls) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (String url : urls) {
            uris.add(Uri.parse(url));
        }
        setAll(uris, false);
        return uris;
    }

    public void addAll(ArrayList<Uri> uris) {
        int storage = (addButton == null || addButton.getParent() == null) ? maxChildCount : maxChildCount + 1;
        int max = Math.min(uris.size(), storage - getChildCount());
        for (int i = 0; i < max; i++) {
            Uri photo = uris.get(i);
            add(photo);
        }
    }

    public void setAll(ArrayList<Uri> uris, boolean append) {
        if (!Utils.equals(photoUris, uris, maxChildCount)) {
            if (append) {
                ListIterator<Uri> iterator = photoUris.listIterator();
                while (iterator.hasNext()) {
                    Uri photo = iterator.next();
                    if (!uris.contains(photo)) {
                        iterator.remove();
                        removeViewByTag(photo);
//                        notifyPhotoRemoved(photo);
                    } else {
                        uris.remove(photo);
                    }

                }
                int storage = (addButton == null || addButton.getParent() == null) ? maxChildCount : maxChildCount + 1;
                int max = Math.min(uris.size(), storage - getChildCount());
                for (int i = 0; i < max; i++) {
                    Uri photo = uris.get(i);
                    add(photo);
                }
            } else {

                photoUris.clear();
                removeAllViews();
//        int storage = addButton.getParent() == null ? maxChildCount : maxChildCount + 1;
                int max = Math.min(uris.size(), maxChildCount);
                for (int i = 0; i < max; i++) {
                    Uri photo = uris.get(i);
                    add(photo);
                }
                resetLayoutParams();
            }
        }
    }

    private void removeViewByTag(Uri tag) {
        for (int i = 0; i < getChildCount(); i++) {
            if (tag.equals(getChildAt(i).getTag())) {
                removeView(getChildAt(i));
            }
        }
    }

    public void removeViewByUri(Uri uri) {
        removeViewByTag(uri);
        photoUris.remove(uri);
    }


    public void removeAllPhotos() {
        removeAllViews();
        requestLayout();

    }

    public interface OnPhotoChangedListener {
        void onPhotoAdded(Uri photo);

        void onPhotoRemoved(Uri photo);
    }

    public PhotoGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = (Activity) context;
        init(attrs);
    }

    public PhotoGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = (Activity) context;
        init(attrs);
    }

    public PhotoGridView(Context context) {
        super(context);
        mContext = (Activity) context;
        init(null);
    }

    private OnClickListener onAddBtnClickListener;


    public void setOnAddButtonClickedListener(OnClickListener onClickListener) {
        this.onAddBtnClickListener = onClickListener;
    }

    public void init(AttributeSet attrs) {
        if (attrs == null) {
            setColumnCount(4);
        }

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PhotoGridView);
        if (a != null) {
            addButtonSrcId = a.getResourceId(R.styleable.PhotoGridView_add_button_src, R.drawable.icon_feedback_addpicture);
            deleteButtonSrcId = a.getResourceId(R.styleable.PhotoGridView_delete_button_src, 0);
            dividerSpace = a.getDimensionPixelOffset(R.styleable.PhotoGridView_divider_space, SizeUtils.dp2px(10));
            maxChildCount = a.getInt(R.styleable.PhotoGridView_max_child_count, 8);
            itemLayoutResId = a.getResourceId(R.styleable.PhotoGridView_item_layout_resource, R.layout.item_photo);
            autoHide = a.getBoolean(R.styleable.PhotoGridView_add_button_auto_hide, true);
            itemWidth = a.getDimensionPixelOffset(R.styleable.PhotoGridView_child_width, 0);
            itemHeight = a.getDimensionPixelOffset(R.styleable.PhotoGridView_child_height, 0);
            addButtonVisible = a.getBoolean(R.styleable.PhotoGridView_add_button_visible, true);
        }
        if (itemWidth == 0) {
            autoWith = true;
        }
        deleteButtonVisible = deleteButtonSrcId != 0;
        if (addButtonVisible) {
            addButton = new Button(getContext());
            addButton.setBackgroundResource(addButtonSrcId);
            addButton.setId(R.id.add_photo);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addButton.setForeground(getResources().getDrawable(R.drawable.selector_item_background));
            }
            ViewUtils.setOnSingleClickListener(addButton, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onAddBtnClickListener != null) {
                        onAddBtnClickListener.onClick(v);
                    }
                }

            });
        }

        mTransition = new LayoutTransition();
        mTransition.setAnimator(LayoutTransition.APPEARING, (mTransition.getAnimator(LayoutTransition.APPEARING)));
        mTransition.setAnimator(LayoutTransition.CHANGE_APPEARING,
                (mTransition.getAnimator(LayoutTransition.CHANGE_APPEARING)));
        mTransition.setAnimator(LayoutTransition.DISAPPEARING, (mTransition.getAnimator(LayoutTransition.DISAPPEARING)));
//        mTransition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING,
//                (mTransition.getAnimator(LayoutTransition.CHANGE_DISAPPEARING)));
        setLayoutTransition(mTransition);

    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (autoWith) {
            int maxWidth = MeasureSpec.getSize(widthSpec)
                    - getPaddingStart() - getPaddingEnd();
            int itemWidth = (maxWidth - dividerSpace * (getColumnCount() - 1)) / getColumnCount();
//        Logger.i("itemsiez", "screen widht:"+ScreenUtil.getScreenWidth()+"margin:"+ ((MarginLayoutParams) getLayoutParams()).leftMargin+","+ ((MarginLayoutParams) getLayoutParams()).rightMargin+
//                "padding  :"+getPaddingStart()+","+getPaddingEnd()
//                        +",maxWidth:"+maxWidth+",itemsize:" + itemSize+",dividerSpace:"+dividerSpace);
            if (this.itemWidth != itemWidth) {
                this.itemWidth = itemWidth;
                if (getChildCount() == 0 && addButtonVisible) {
                    addView(addButton, generateLayoutParams(0));
                } else {
                    resetLayoutParams();
                }
            }
        }

    }

    private void resetLayoutParams() {
        if ((hasStorage() || !autoHide) && addButton != null && addButton.getParent() == null) {
            addView(addButton);
        }
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setLayoutParams(generateLayoutParams(i));
        }
    }

    private boolean hasStorage() {
        return getChildCount() <= maxChildCount - 1;
    }

    private LayoutParams generateLayoutParams(int index) {
        MarginLayoutParams params = new MarginLayoutParams(itemWidth, itemHeight > 0 ? itemHeight : itemWidth);
        if (index % getColumnCount() != 0) {
            params.leftMargin = dividerSpace;
        }
        Logger.i("itemsize", "index:" + index + ",leftMargin:" + params.leftMargin);
        if (index / getColumnCount() > 0) {
            params.topMargin = dividerSpace;
        }
        return new LayoutParams(params);
    }

    public void add(String url) {
        add(Uri.parse(url));
    }

    public void add(Uri uri) {
        final View itemView = inflate(getContext(), itemLayoutResId, null);
        View delete = itemView.findViewById(R.id.grid_item_delete_button);
        ImageView deleteIcon = itemView.findViewById(R.id.grid_item_delete_icon);
        if (deleteButtonVisible) {
            delete.setVisibility(View.VISIBLE);
            deleteIcon.setImageResource(deleteButtonSrcId);
        } else {
            delete.setVisibility(View.GONE);
        }
        RoundedImageView image = itemView.findViewById(R.id.grid_item_picture);
        GlideImageLoader.with(image).display(uri, R.drawable.ic_gallery_v21);
        itemView.setTag(uri);
        final int position = addButton != null ? (getChildCount() - 1) : getChildCount();
        addView(itemView, position);
        photoUris.add(uri);
        resetLayoutParams();
        notifyPhotoAdded(uri);
        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                PhotoPagerActivity.launch(getContext(), PhotoGridView.this, getPhotosUris(), indexOfChild(view), new PhotoPagerActivity.ImageGetterFromItemView() {
                    @Override
                    public View getImage(int position, View itemView) {
                        if (itemView == addButton) {
                            return null;
                        }
                        return itemView.findViewById(R.id.grid_item_picture);
                    }
                });
            }
        });
        delete.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                removeView(itemView);
                //makeMainPicture();
                resetLayoutParams();
                photoUris.remove(uri);
                notifyPhotoRemoved(uri);
            }

        });
        if (getChildCount() > maxChildCount) {
            if (autoHide && addButton != null) {
                removeView(addButton);
            }
        }
    }

    public ArrayList<String> getPhotosUris() {
        ArrayList<String> uris = new ArrayList<>();
        for (Uri uri : photoUris) {
            uris.add(uri.toString());
        }
        return uris;
    }

    public ArrayList<Uri> getOriginalPhotos() {
        return photoUris;
    }

    public ArrayList<Uri> getCopiedPhotos() {
        if (!Utils.isEmpty(photoUris)) {
            ArrayList<Uri> uris = new ArrayList<>();
            for (Uri uri : photoUris) {
                uris.add(uri);
            }
            return uris;
        }
        return null;
    }

    public void setOnPhotoChangedListener(OnPhotoChangedListener onPhotoChangedListener) {
        this.onPhotoChangedListener = onPhotoChangedListener;
    }

    private void notifyPhotoAdded(Uri uri) {
        if (onPhotoChangedListener != null)
            onPhotoChangedListener.onPhotoAdded(uri);
    }


    private void notifyPhotoRemoved(Uri uri) {
        if (onPhotoChangedListener != null)
            onPhotoChangedListener.onPhotoRemoved(uri);
    }


}