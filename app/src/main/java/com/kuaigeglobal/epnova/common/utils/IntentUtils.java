package com.kuaigeglobal.epnova.common.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import com.kuaigeglobal.epnova.App;
import com.kuaigeglobal.epnova.service.DownloadService;
import com.kuaigeglobal.epnova.widget.CommonDialog;
import com.nhsd.basic.ext.ToastExtKt;

public class IntentUtils {

    public static void startDownload(Context context, String schemeUri) {
        String fileName = Utils.parseName(schemeUri);
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(DownloadService.DOWNLOAD_FILE_NAME, fileName);
        intent.putExtra(DownloadService.DOWNLOADURL, schemeUri);
        context.startService(intent);
        ToastExtKt.showToast("正在为你下载，请稍候");
    }

    /**
     * 手机是否开启位置服务，如果没有开启那么所有app将不能使用定位功能
     */
    public static boolean isLocationServiceEnable(Context context) {
        if (context == null) {
            return false;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }

    public static void startOpenGPSSetting(Context context) {
        startOpenGPSSetting(context, null);
    }

    public static void startOpenGPSSetting(Context context, Callback callback) {
        CommonDialog commonDialog = new CommonDialog(context, "检测到系统定位服务未开启，是否立即开启？");
        commonDialog.setOnPositiveButtonClickListener(dialog -> {
            if (callback != null) {
                callback.callBack(true);
            }
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                startActivityForResult(intent,887);
            context.startActivity(intent);
            return null;
        });
        commonDialog.show();
    }

    public static boolean isNotificationEnabled(Context context) {
        boolean isOpened = false;
        try {
            isOpened = NotificationManagerCompat.from(context).areNotificationsEnabled();
        } catch (Exception e) {
            e.printStackTrace();
            isOpened = false;
        }
        return isOpened;
    }

    public static void startOpenNotificationSetting(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 26) {
            // android 8.0引导
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
        } else if (Build.VERSION.SDK_INT >= 21) {
            // android 5.0-7.0
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else {
            // 其他
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }

    public static void openSystemLocSetting() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getInstance().startActivity(intent);
    }


    public static void openAppSetting() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", App.getInstance().getPackageName(), null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//这句要加  不加很多机型打不开设置界面
        intent.setData(uri);
        try {
            App.getInstance().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void jumpToWx(Context context) {
        // jump to wechat
        ComponentName name = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
        try {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(name);
            context.startActivity(intent);
        } catch (Exception e) {
            ToastExtKt.showToast("微信未安装");
            e.printStackTrace();
        }
    }
}
