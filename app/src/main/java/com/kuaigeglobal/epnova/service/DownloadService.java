/*
 * Copyright (C) 2009 Teleca Poland Sp. z o.o. <android@teleca.com>
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

package com.kuaigeglobal.epnova.service;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.kuaigeglobal.epnova.common.utils.FileUtils;
import com.kuaigeglobal.epnova.common.utils.Utils;
import com.kuaigeglobal.epnova.core.Events;
import com.nhsd.basic.bus.AppEvent;
import com.nhsd.basic.bus.FlowEventBus;
import com.nhsd.basic.manager.FileManager;

import java.io.File;

/**
 * 下载apk service
 *
 * @author BSN
 */
@SuppressLint("NewApi")
public class DownloadService extends Service {


    public static final String UPDATE_FILE_NAME = "appUpgrade.apk";
    private DownloadManager manager;
    private DownloadCompleteReceiver receiver;
    private String url;
    public static final String DOWNLOADPATH = Environment.DIRECTORY_DOWNLOADS;
    public static final String DOWNLOADURL = "downloadurl";
    public static final String MD5_HASH = "md5Hash";
    public static final String DOWNLOAD_FILE_NAME = "fileName";
    public static final String DOWNLOAD_WITH_NOTIFY = "downloadWithNotify";
    public static final String DOWNLOAD_TYPE = "download_type";
    public static final String DOWNLOAD_TYPE_TASK = "download_type_task";
    public static final String DOWNLOAD_TYPE_CONTRACT = "download_type_contract";
    private String md5;
    private boolean downloadWithNotify = false;
    private String path;
    private String fileName;
    private String downloadType;
    private long downloadId;

    private void initDownManager() {
        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        receiver = new DownloadCompleteReceiver();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        //DownloadManager.Request down = new DownloadManager.Request(Uri.parse(url));
        //.replace("https://", "http://")//解决https证书问题
        DownloadManager.Request down = new DownloadManager.Request(Uri.parse(url));
        down.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                | DownloadManager.Request.NETWORK_WIFI);
        down.setAllowedOverRoaming(false);
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
        down.setMimeType(mimeString);
//		down.setDescription("")
        down.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        down.setVisibleInDownloadsUi(true);
        /*if (!DOWNLOAD_TYPE_CONTRACT.equals(downloadType) && downloadWithNotify) {
            down.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }*/
        //down.setDestinationInExternalPublicDir(DOWNLOADPATH, fileName);
        down.setDestinationInExternalFilesDir(this, FileManager.getInstance().getApkFolder().getName(), fileName);
        down.setTitle(Utils.parseName(url));
        downloadId = manager.enqueue(down);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int percent = getPercent(downloadId);
                if (percent <= 0) {
                    percent = 1;
                }
                FlowEventBus.postSync(new Events.DownloadProgressEvent(url, percent, null));
                handler.postDelayed(this, 200);
            }
        }, 100);
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        url = intent.getStringExtra(DOWNLOADURL);
        md5 = intent.getStringExtra(MD5_HASH);
        downloadType = intent.getStringExtra(DOWNLOAD_TYPE);
        downloadWithNotify = intent.getBooleanExtra(DOWNLOAD_WITH_NOTIFY, false);
        fileName = intent.getStringExtra(DOWNLOAD_FILE_NAME);
        if (TextUtils.isEmpty(fileName)) {
            fileName = UPDATE_FILE_NAME;
        }
        // path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + DOWNLOADPATH + File.separator + fileName;

        File file = new File(FileManager.getInstance().getApkFolder(), fileName);
        path = file.getPath();
        // File file = new File(path);
        if (file.exists() && file.isFile() && md5 != null) {
            if (md5.equalsIgnoreCase(FileUtils.getFileMD5(file))) {
                if (downloadWithNotify) {
                    FlowEventBus.postSync(new DownloadApkEvent(path));
                } else {
                    FileUtils.openFile(this, file);
                }
                stopSelf();
                return Service.START_NOT_STICKY;
            } else {
                deleteFileWithPath(path);
            }
        }
        try {
            initDownManager();
        } catch (Exception e) {
            e.printStackTrace();
//            try {
//                Uri uri = Uri.parse("market://details?id=" + getPackageName());
//                Intent intent0 = new Intent(Intent.ACTION_VIEW, uri);
//                intent0.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent0);
//            } catch (Exception ex) {
            if (!downloadWithNotify) {
                Toast.makeText(getApplicationContext(), "下载失败", Toast.LENGTH_SHORT).show();
            }
//            }
            stopSelf();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver);
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private Handler handler = new Handler(Looper.getMainLooper());


    private int getPercent(long downloadId) {

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor = manager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载文件大小
                int download = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //下载文件的总大小
                int total = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                //下载状态
//                bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (total == 0) {
                    return 0;
                }
                return (int) (download * 100f / total);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    class DownloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                handler.removeCallbacksAndMessages(null);
                long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                String path = null;
                if (manager.getUriForDownloadedFile(downId) != null) {
                    try {
                        path = getRealFilePath(context, manager.getUriForDownloadedFile(downId));

                    } catch (Throwable e) {
                        path = DownloadService.this.path;
                    }
                    if (TextUtils.equals(downloadType, DOWNLOAD_TYPE_CONTRACT)) {
                        FlowEventBus.postSync(new Events.DownloadContractEvent(fileName));
                    } else {
                        installAPK(context, path);
                    }
                } else {
                    if (!downloadWithNotify) {
                        Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                    }
                }
                FlowEventBus.postSync(new Events.DownloadProgressEvent(url, 100, path));
                DownloadService.this.stopSelf();
            }
        }

        private void installAPK(Context context, String path) {
            File file = new File(path);
            String fileMd5 = FileUtils.getFileMD5(file);
            if (file.exists() && (md5 == null || md5.equalsIgnoreCase(fileMd5))) {
                if (downloadWithNotify) {
                    FlowEventBus.postSync(new DownloadApkEvent(path));
                } else {
                    if (TextUtils.equals(downloadType, DOWNLOAD_TYPE_TASK)) {
                        FlowEventBus.postSync(new InstallApkEvent(url, path));
                    }
                    FileUtils.openFile(context, file);
                }
            } else {
                if (!downloadWithNotify) {
                    Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public String getRealFilePath(Context context, Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {

            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


    public static boolean deleteFileWithPath(String filePath) {
        SecurityManager checker = new SecurityManager();
        File f = new File(filePath);
        checker.checkDelete(filePath);
        if (f.isFile()) {
            f.delete();
            return true;
        }
        return false;
    }

    public static class DownloadApkEvent implements AppEvent {
        public final String path;

        public DownloadApkEvent(String path) {
            this.path = path;
        }
    }

    public static class InstallApkEvent implements AppEvent {
        public final String url;
        public final String path;

        public InstallApkEvent(String url, String path) {
            this.url = url;
            this.path = path;
        }
    }
}
