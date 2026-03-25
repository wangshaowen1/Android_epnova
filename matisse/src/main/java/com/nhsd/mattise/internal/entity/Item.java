/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nhsd.mattise.internal.entity;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.blankj.utilcode.util.Utils;
import com.bumptech.glide.Glide;
import com.nhsd.mattise.MimeType;
import com.nhsd.mattise.internal.utils.PathUtils;

public class Item implements Parcelable {
    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        @Nullable
        public Item createFromParcel(Parcel source) {
            return new Item(source);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
    public static final long ITEM_ID_CAPTURE = -1;
    public static final long ITEM_ID_MEDIA_ADD = -2;
    public static final String ITEM_DISPLAY_NAME_CAPTURE = "Capture";
    public static final String ITEM_DISPLAY_NAME_MEDIA_ADD = "MEDIA_ADD";
    public final long id;
    public final String mimeType;
    public final Uri uri;
    public final long size;
    public final long duration; // only for video, in ms

    private Item(long id, String mimeType, long size, long duration) {
        this.id = id;
        this.mimeType = mimeType;
        Uri contentUri;
        if (isImage()) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (isVideo()) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            // ?
            contentUri = MediaStore.Files.getContentUri("external");
        }
        this.uri = ContentUris.withAppendedId(contentUri, id);
        this.size = size;
        this.duration = duration;
    }

    private Item(Parcel source) {
        id = source.readLong();
        mimeType = source.readString();
        uri = source.readParcelable(Uri.class.getClassLoader());
        size = source.readLong();
        duration = source.readLong();
    }

    public static Item valueOf(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        // 定义常量避免硬编码
        final String DURATION_COLUMN = "duration";

        // 缓存列索引避免重复调用
        int idColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
        int mimeTypeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
        int sizeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
        int durationColumnIndex = cursor.getColumnIndex(DURATION_COLUMN);

        // 检查列索引有效性
        Long id = (idColumnIndex != -1) ? cursor.getLong(idColumnIndex) : null;
        String mimeType = (mimeTypeColumnIndex != -1) ? cursor.getString(mimeTypeColumnIndex) : null;
        Long size = (sizeColumnIndex != -1) ? cursor.getLong(sizeColumnIndex) : null;
        Long duration = (durationColumnIndex != -1) ? cursor.getLong(durationColumnIndex) : null;

        return new Item(id, mimeType, size, duration);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(mimeType);
        dest.writeParcelable(uri, 0);
        dest.writeLong(size);
        dest.writeLong(duration);
    }

    public Uri getContentUri() {
        return uri;
    }

    public boolean isCapture() {
        return id == ITEM_ID_CAPTURE;
    }

    public boolean isMediaAdd() {
        return id == ITEM_ID_MEDIA_ADD;
    }

    public boolean isImage() {
        return MimeType.isImage(mimeType);
    }

    public boolean isGif() {
        return MimeType.isGif(mimeType);
    }

    public boolean isVideo() {
        return MimeType.isVideo(mimeType);
    }

    public String getPath() {
        return PathUtils.getPath(Utils.getApp(), getContentUri());
    }

    @WorkerThread
    public Size getPixels() {
        try {
            if (isImage()) {
                Bitmap bitmap = Glide.with(Utils.getApp())
                        .asBitmap()
                        .load(this)
                        .submit()
                        .get();
                return new Size(bitmap.getWidth(), bitmap.getHeight());
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getPath());
            int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            if (rotation % 180 == 0) {
                return new Size(width, height);
            } else {
                return new Size(height, width);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Size(1, 1);
    }

    public int getVideoRotation() {
        try {
            if (!isVideo()) {
                return 0;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getPath());
            return Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Item)) {
            return false;
        }

        Item other = (Item) obj;
        return id == other.id
                && (mimeType != null && mimeType.equals(other.mimeType)
                || (mimeType == null && other.mimeType == null))
                && (uri != null && uri.equals(other.uri)
                || (uri == null && other.uri == null))
                && size == other.size
                && duration == other.duration;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Long.valueOf(id).hashCode();
        if (mimeType != null) {
            result = 31 * result + mimeType.hashCode();
        }
        result = 31 * result + uri.hashCode();
        result = 31 * result + Long.valueOf(size).hashCode();
        result = 31 * result + Long.valueOf(duration).hashCode();
        return result;
    }
}
