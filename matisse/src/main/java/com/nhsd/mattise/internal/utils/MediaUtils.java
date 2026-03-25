package com.nhsd.mattise.internal.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.nhsd.basic.BaseApp;
import com.nhsd.mattise.internal.entity.Item;

public class MediaUtils {

    public static Item queryItem(Context context, Uri uri){
        if(uri!=null) {
            try {
                ContentResolver cr = context.getContentResolver();
                Uri source = MediaStore.Files.getContentUri("external");
                // every column, although that is huge waste, you probably need
                // BaseColumns.DATA (the path) only.
                String[] projection = null;
                long id = ContentUris.parseId(uri);
                // exclude media files, they would be here also.
                String selection = MediaStore.Files.FileColumns._ID + "="
                        + id;
                String[] selectionArgs = null; // there is no ? in selection so null here
                String sortOrder = null; // unordered
                Cursor mediaFiles = cr.query(source, projection, selection, selectionArgs, sortOrder);
                if (mediaFiles != null && mediaFiles.getCount() > 0) {
                    mediaFiles.moveToFirst();
                    return Item.valueOf(mediaFiles);
                }
            }catch (Exception e){
                //ignore all exceptions
            }
        }
        return null;
    }
}
