/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nhsd.mattise.internal.ui.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhsd.mattise.internal.entity.Album;
import com.nhsd.mattise.internal.entity.IncapableCause;
import com.nhsd.mattise.internal.entity.Item;
import com.nhsd.mattise.internal.entity.SelectionSpec;
import com.nhsd.mattise.internal.model.SelectedItemCollection;
import com.nhsd.mattise.internal.ui.widget.CheckView;
import com.nhsd.mattise.internal.ui.widget.MediaGrid;
import com.nhsd.mattise.internal.utils.MediaUtils;
import com.zhihu.matisse.R;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class AlbumMediaAdapter extends
        RecyclerViewCursorAdapter<RecyclerView.ViewHolder> implements
        MediaGrid.OnMediaGridClickListener {

    private static final int VIEW_TYPE_CAPTURE = 0x01;
    private static final int VIEW_TYPE_MEDIA = 0x02;
    private static final int VIEW_TYPE_MEDIA_ADD = 0x03;
    private final SelectedItemCollection mSelectedCollection;
    private final Drawable mPlaceholder;
    private SelectionSpec mSelectionSpec;
    private CheckStateListener mCheckStateListener;
    private OnMediaClickListener mOnMediaClickListener;
    private RecyclerView mRecyclerView;
    private int mImageResize;

    public AlbumMediaAdapter(Context context, SelectedItemCollection selectedCollection, RecyclerView recyclerView) {
        super(null);
        mSelectionSpec = SelectionSpec.getInstance();
        mSelectedCollection = selectedCollection;

        TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{R.attr.item_placeholder});
        mPlaceholder = ta.getDrawable(0);
        ta.recycle();

        mRecyclerView = recyclerView;

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CAPTURE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_capture_item, parent, false);
            CaptureViewHolder holder = new CaptureViewHolder(v);
            holder.itemView.setOnClickListener(v2 -> {
                if (v2.getContext() instanceof OnPhotoCapture) {
                    ((OnPhotoCapture) v2.getContext()).capture();
                }
            });
            return holder;
        } else if (viewType == VIEW_TYPE_MEDIA) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_grid_item, parent, false);
            return new MediaViewHolder(v);
        } else if (viewType == VIEW_TYPE_MEDIA_ADD) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_media_add_item, parent, false);
            MediaAddHolder holder = new MediaAddHolder(v);
            holder.itemView.setOnClickListener(v1 -> {
                if (v1.getContext() instanceof OnPhotoCapture) {
                    ((OnPhotoCapture) v1.getContext()).expandAuthorizedMedia();
                }
            });
            return holder;
        }
        return null;
    }

    @Override
    protected void onBindViewHolder(final RecyclerView.ViewHolder holder, Cursor cursor) {
        prepareForSection(holder.itemView.getContext(),mSelectedCollection);
        if (holder instanceof MediaViewHolder) {
            MediaViewHolder mediaViewHolder = (MediaViewHolder) holder;

            final Item item = Item.valueOf(cursor);
            mediaViewHolder.mMediaGrid.preBindMedia(new MediaGrid.PreBindInfo(
                    getImageResize(mediaViewHolder.mMediaGrid.getContext()),
                    mPlaceholder,
                    mSelectionSpec.countable,
                    holder
            ));
            mediaViewHolder.mMediaGrid.bindMedia(item);
            mediaViewHolder.mMediaGrid.setOnMediaGridClickListener(this);
            setCheckStatus(item, mediaViewHolder.mMediaGrid);
        }
    }


    private void prepareForSection(Context context, SelectedItemCollection mSelectedCollection) {
        if (mSelectionSpec.selectedUris != null && !mSelectionSpec.selectedUris.isEmpty()) {

            List<Uri> tmp = new ArrayList<>(mSelectionSpec.selectedUris);

            for (Uri uri : tmp) {
                if (uri != null) {
                    Item item = MediaUtils.queryItem(context, uri);
                    if (item != null) {
                        mSelectedCollection.add(item);
                    }
                }
            }

            // 保证不崩
            try {
                mSelectionSpec.selectedUris.clear();
            } catch (UnsupportedOperationException e) {
                // 强制替换成可变列表
                mSelectionSpec.selectedUris = new ArrayList<>();
            }

            if (mCheckStateListener != null) {
                mCheckStateListener.onUpdate();
            }
        }
    }


    private void setCheckStatus(Item item, MediaGrid mediaGrid) {
        if (mSelectionSpec.crop) {
            mediaGrid.setCheckVisible(false);
        } else {
            if (mSelectionSpec.countable) {
                int checkedNum = mSelectedCollection.checkedNumOf(item);
                if (checkedNum > 0) {
                    mediaGrid.setCheckEnabled(true);
                    mediaGrid.setCheckedNum(checkedNum);
                } else {
                    if (mSelectedCollection.maxSelectableReached()) {
                        mediaGrid.setCheckEnabled(false);
                        mediaGrid.setCheckedNum(CheckView.UNCHECKED);
                    } else {
                        mediaGrid.setCheckEnabled(true);
                        mediaGrid.setCheckedNum(checkedNum);
                    }
                }
            } else {
                boolean selected = mSelectedCollection.isSelected(item);
                if (selected) {
                    mediaGrid.setCheckEnabled(true);
                    mediaGrid.setChecked(true);
                } else {
                    if (mSelectedCollection.maxSelectableReached()) {
                        mediaGrid.setCheckEnabled(false);
                    } else {
                        mediaGrid.setCheckEnabled(true);
                    }
                    mediaGrid.setChecked(false);
                }
            }
        }
    }

    @Override
    public void onThumbnailClicked(ImageView thumbnail, Item item, RecyclerView.ViewHolder holder) {
        if (mSelectionSpec.showPreview) {
            if (mOnMediaClickListener != null) {
                mOnMediaClickListener.onMediaClick(null, item, holder.getAdapterPosition());
            }
        } else {
            updateSelectedItem(item, holder);
        }
    }

    @Override
    public void onCheckViewClicked(CheckView checkView, Item item, RecyclerView.ViewHolder holder) {
        updateSelectedItem(item, holder);
    }

    private void updateSelectedItem(Item item, RecyclerView.ViewHolder holder) {
        if (mSelectionSpec.countable) {
            int checkedNum = mSelectedCollection.checkedNumOf(item);
            if (checkedNum == CheckView.UNCHECKED) {
                if (assertAddSelection(holder.itemView.getContext(), item)) {
                    mSelectedCollection.add(item);
                    notifyCheckStateChanged();
                }
            } else {
                mSelectedCollection.remove(item);
                notifyCheckStateChanged();
            }
        } else {
            if (mSelectedCollection.isSelected(item)) {
                mSelectedCollection.remove(item);
                notifyCheckStateChanged();
            } else {
                if (assertAddSelection(holder.itemView.getContext(), item)) {
                    mSelectedCollection.add(item);
                    notifyCheckStateChanged();
                }
            }
        }
    }

    private void notifyCheckStateChanged() {
        notifyDataSetChanged();
        if (mCheckStateListener != null) {
            mCheckStateListener.onUpdate();
        }
    }

    @Override
    public int getItemViewType(int position, Cursor cursor) {
        if (Item.valueOf(cursor).isCapture()) {
            return VIEW_TYPE_CAPTURE;
        } else if (Item.valueOf(cursor).isMediaAdd()) {
            return VIEW_TYPE_MEDIA_ADD;
        } else {
            return VIEW_TYPE_MEDIA;
        }
    }

    private boolean assertAddSelection(Context context, Item item) {
        IncapableCause cause = mSelectedCollection.isAcceptable(item);
        IncapableCause.handleCause(context, cause);
        return cause == null;
    }


    public void registerCheckStateListener(CheckStateListener listener) {
        mCheckStateListener = listener;
    }

    public void unregisterCheckStateListener() {
        mCheckStateListener = null;
    }

    public void registerOnMediaClickListener(OnMediaClickListener listener) {
        mOnMediaClickListener = listener;
    }

    public void unregisterOnMediaClickListener() {
        mOnMediaClickListener = null;
    }

    public void refreshSelection() {
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first == -1 || last == -1) {
            return;
        }
        Cursor cursor = getCursor();
        for (int i = first; i <= last; i++) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(first);
            if (holder instanceof MediaViewHolder) {
                if (cursor.moveToPosition(i)) {
                    setCheckStatus(Item.valueOf(cursor), ((MediaViewHolder) holder).mMediaGrid);
                }
            }
        }
    }

    private int getImageResize(Context context) {
        if (mImageResize == 0) {
            RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
            int spanCount = ((GridLayoutManager) lm).getSpanCount();
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int availableWidth = screenWidth - context.getResources().getDimensionPixelSize(
                    R.dimen.media_grid_spacing) * (spanCount - 1);
            mImageResize = availableWidth / spanCount;
            mImageResize = (int) (mImageResize * mSelectionSpec.thumbnailScale);
        }
        return mImageResize;
    }

    public interface CheckStateListener {
        void onUpdate();
    }

    public interface OnMediaClickListener {
        void onMediaClick(Album album, Item item, int adapterPosition);
    }

    public interface OnPhotoCapture {
        void capture();

        void expandAuthorizedMedia();
    }

    private static class MediaViewHolder extends RecyclerView.ViewHolder {

        private MediaGrid mMediaGrid;

        MediaViewHolder(View itemView) {
            super(itemView);
            mMediaGrid = (MediaGrid) itemView;
        }
    }

    private static class CaptureViewHolder extends RecyclerView.ViewHolder {
        CaptureViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class MediaAddHolder extends RecyclerView.ViewHolder {
        MediaAddHolder(View itemView) {
            super(itemView);
        }
    }


}
