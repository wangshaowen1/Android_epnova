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
package com.nhsd.mattise.internal.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.loader.content.CursorLoader;

import com.nhsd.mattise.MimeType;
import com.nhsd.mattise.internal.entity.Album;
import com.nhsd.mattise.internal.entity.SelectionSpec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Load all albums (grouped by bucket_id) into a single cursor.
 */
public class AlbumLoader extends CursorLoader {

    private static final String COLUMN_BUCKET_ID = "bucket_id";
    private static final String COLUMN_BUCKET_DISPLAY_NAME = "bucket_display_name";
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_COUNT = "count";
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");

    private static final String[] COLUMNS = {
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            COLUMN_URI,
            COLUMN_COUNT};

    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            "COUNT(*) AS " + COLUMN_COUNT};

    private static final String[] PROJECTION_29 = {
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE};

    // === params for showSingleMediaType: false ===
    private static final String SELECTION =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + ") GROUP BY (bucket_id";
    private static final String SELECTION_29 =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String[] SELECTION_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };
    // =============================================

    // === params for showSingleMediaType: true ===
    private static final String SELECTION_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + ") GROUP BY (bucket_id";
    private static final String SELECTION_FOR_SINGLE_MEDIA_TYPE_29 =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }
    // =============================================

    // === params for showSingleMediaType: true ===
    private static final String SELECTION_FOR_SINGLE_MEDIA_GIF_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + MediaStore.MediaColumns.MIME_TYPE + "=?"
                    + ") GROUP BY (bucket_id";
    private static final String SELECTION_FOR_SINGLE_MEDIA_GIF_TYPE_29 =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + MediaStore.MediaColumns.MIME_TYPE + "=?";

    private static String[] getSelectionArgsForSingleMediaGifType(int mediaType) {
        return new String[]{String.valueOf(mediaType), "image/gif"};
    }
    // =============================================

    private static final String BUCKET_ORDER_BY = "datetaken DESC";

    private AlbumLoader(Context context, String selection, String[] selectionArgs) {
        super(
                context,
                QUERY_URI,
                beforeAndroidTen() ? PROJECTION : PROJECTION_29,
                selection,
                selectionArgs,
                BUCKET_ORDER_BY
        );
    }

    public static CursorLoader newInstance(Context context) {
        String selection;
        String[] selectionArgs;
        if (SelectionSpec.getInstance().onlyShowGif()) {
            selection = beforeAndroidTen()
                    ? SELECTION_FOR_SINGLE_MEDIA_GIF_TYPE : SELECTION_FOR_SINGLE_MEDIA_GIF_TYPE_29;
            selectionArgs = getSelectionArgsForSingleMediaGifType(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
        } else if (SelectionSpec.getInstance().onlyShowImages()) {
            selection = beforeAndroidTen()
                    ? SELECTION_FOR_SINGLE_MEDIA_TYPE : SELECTION_FOR_SINGLE_MEDIA_TYPE_29;
            selectionArgs = getSelectionArgsForSingleMediaType(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
        } else if (SelectionSpec.getInstance().onlyShowVideos()) {
            selection = beforeAndroidTen()
                    ? SELECTION_FOR_SINGLE_MEDIA_TYPE : SELECTION_FOR_SINGLE_MEDIA_TYPE_29;
            selectionArgs = getSelectionArgsForSingleMediaType(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
        } else {
            selection = beforeAndroidTen() ? SELECTION : SELECTION_29;
            selectionArgs = SELECTION_ARGS;
        }
        return new AlbumLoader(context, selection, selectionArgs);
    }

    public Cursor loadInBackground() {
        Cursor albums = null;
        try {
            albums = super.loadInBackground();
            MatrixCursor allAlbum = new MatrixCursor(COLUMNS);
            MatrixCursor otherAlbums = new MatrixCursor(COLUMNS);

            int totalCount = 0;
            Uri allAlbumCoverUri = null;

            if (beforeAndroidTen()) {
                if (albums != null && albums.moveToFirst()) {
                    int idIndex = albums.getColumnIndex(MediaStore.Files.FileColumns._ID);
                    int bucketIdIndex = albums.getColumnIndex(COLUMN_BUCKET_ID);
                    int displayNameIndex = albums.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME);
                    int mimeTypeIndex = albums.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
                    int countIndex = albums.getColumnIndex(COLUMN_COUNT);

                    if (idIndex == -1 || bucketIdIndex == -1 || displayNameIndex == -1 || mimeTypeIndex == -1 || countIndex == -1) {
                        throw new IllegalArgumentException("Missing required column index.");
                    }

                    do {
                        long fileId = albums.getLong(idIndex);
                        long bucketId = albums.getLong(bucketIdIndex);
                        String bucketDisplayName = albums.getString(displayNameIndex);
                        String mimeType = albums.getString(mimeTypeIndex);
                        Uri uri = getUri(albums);
                        int count = albums.getInt(countIndex);

                        otherAlbums.addRow(new String[]{
                                Long.toString(fileId),
                                Long.toString(bucketId),
                                bucketDisplayName,
                                mimeType,
                                uri.toString(),
                                String.valueOf(count)
                        });
                        totalCount += count;
                    } while (albums.moveToNext());

                    albums.moveToFirst();
                    allAlbumCoverUri = getUri(albums);
                }
            } else {
                Map<Long, Long> countMap = new HashMap<>();
                Set<Long> done = new HashSet<>();

                if (albums != null && albums.moveToFirst()) {
                    int bucketIdIndex = albums.getColumnIndex(COLUMN_BUCKET_ID);
                    int idIndex = albums.getColumnIndex(MediaStore.Files.FileColumns._ID);
                    int displayNameIndex = albums.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME);
                    int mimeTypeIndex = albums.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);

                    if (bucketIdIndex == -1 || idIndex == -1 || displayNameIndex == -1 || mimeTypeIndex == -1) {
                        throw new IllegalArgumentException("Missing required column index.");
                    }

                    allAlbumCoverUri = getUri(albums);

                    do {
                        long bucketId = albums.getLong(bucketIdIndex);

                        // Update count map
                        countMap.compute(bucketId, (k, v) -> (v == null) ? 1L : v + 1);

                        if (done.contains(bucketId)) {
                            continue;
                        }

                        long fileId = albums.getLong(idIndex);
                        String bucketDisplayName = albums.getString(displayNameIndex);
                        String mimeType = albums.getString(mimeTypeIndex);
                        Uri uri = getUri(albums);
                        long count = countMap.get(bucketId);

                        otherAlbums.addRow(new String[]{
                                Long.toString(fileId),
                                Long.toString(bucketId),
                                bucketDisplayName,
                                mimeType,
                                uri.toString(),
                                String.valueOf(count)
                        });
                        done.add(bucketId);
                        totalCount += count;
                    } while (albums.moveToNext());
                }
            }

            allAlbum.addRow(new String[]{
                    Album.ALBUM_ID_ALL,
                    Album.ALBUM_ID_ALL,
                    Album.ALBUM_NAME_ALL,
                    null,
                    allAlbumCoverUri == null ? null : allAlbumCoverUri.toString(),
                    String.valueOf(totalCount)
            });

            return new MergeCursor(new Cursor[]{allAlbum, otherAlbums});
        } finally {
            if (albums != null) {
                albums.close();
            }
        }
    }


    private static Uri getUri(Cursor cursor) {
        int idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
        int mimeTypeColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);

        long id = cursor.getLong(idColumnIndex);
        String mimeType = cursor.getString(mimeTypeColumnIndex);
        Uri contentUri;

        if (mimeType != null && MimeType.isImage(mimeType)) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType != null && MimeType.isVideo(mimeType)) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            contentUri = MediaStore.Files.getContentUri("external");
        }

        return ContentUris.withAppendedId(contentUri, id);
    }


    @Override
    public void onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }

    /**
     * @return 是否是 Android 10 （Q） 之前的版本
     */
    private static boolean beforeAndroidTen() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }
}