package com.kuaigeglobal.epnova.common.utils;

import static com.kuaigeglobal.epnova.common.ext.ContextUtils.getTopActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.blankj.utilcode.util.PhoneUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.kuaigeglobal.epnova.App;
import com.kuaigeglobal.epnova.R;
import com.kuaigeglobal.epnova.common.Constants;
import com.kuaigeglobal.epnova.common.ext.UriExtKt;
import com.kuaigeglobal.epnova.common.permissions.PermissionsExt;
import com.kuaigeglobal.epnova.common.permissions.PermissionsUtils;
import com.kuaigeglobal.epnova.widget.LottieHeader;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kuaige.imageloader.GlideImageLoader;
import com.nhsd.basic.BaseActivity;
import com.nhsd.basic.BaseApp;
import com.nhsd.basic.ext.LifecycleUtils;
import com.nhsd.basic.ext.ToastExtKt;
import com.nhsd.basic.manager.FileManager;
import com.shizhefei.view.coolrefreshview.PullHeader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Created by ludaiqian on 16/7/7.
 */
public class Utils {
    private static final String TEMP_IMAGE_NAME = "_temp.jpg";

    public static File createCameraImagePath() {
        return FileManager.getInstance().createImageFile(System.currentTimeMillis() + TEMP_IMAGE_NAME);
    }


    public static <T> boolean equals(List<T> list1, List<T> list2, int length) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() < length || list1.size() > list2.size()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the Application object.
     * <p>Main process get app by UtilsFileProvider,
     * and other process get app by reflect.</p>
     *
     * @return the Application object
     */
    public static Application getApp() {
        return App.getInstance();
    }

    /**
     * 方法描述：判断某一Service是否正在运行
     *
     * @param context     上下文
     * @param serviceName Service的全路径： 包名 + service的类名
     * @return true 表示正在运行，false 表示没有运行
     */
    public static boolean isServiceRunning(Context context, String serviceName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServiceInfos = am.getRunningServices(200);
        if (runningServiceInfos.isEmpty()) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo serviceInfo : runningServiceInfos) {
            if (serviceInfo.service.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean isEmpty(Collection<T> list) {
        return list == null || list.isEmpty();
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }


    public static List<String> toPathList(List<Uri> photoUris) {
        List<String> pathList = new ArrayList<>();
        for (Uri photoUri : photoUris) {
            String path = null;
            try {
                path = RealPathUtil.getRealPath(App.getInstance(), photoUri);
            } catch (Exception ignore) {
            }
            if (TextUtils.isEmpty(path)) {
                path = UriExtKt.getTempFilePathFromUri(photoUri, App.getInstance());
            }
            if (!TextUtils.isEmpty(path)) {
                pathList.add(path);
            }
        }
        return pathList;
    }

    public static PullHeader getPullHeader() {
        return getPullHeader(getTopActivity());
    }

    public static PullHeader getPullHeader(Context context) {
        if (context == null) return null;
        LottieHeader materialHeader = new LottieHeader(context);
//        int[] colors = new int[]{context.getResources().getColor(R.color.gplus_color_1),
//                context.getResources().getColor(R.color.gplus_color_2),
//                context.getResources().getColor(R.color.gplus_color_3),
//                context.getResources().getColor(R.color.gplus_color_4)};
//        materialHeader.setColorSchemeColors(colors);
        return materialHeader;
    }

    /*生成二维码*/
    public static Bitmap createQRCode(String content, int w, int h, Bitmap logo) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        /*偏移量*/
        int offsetX = w / 2;
        int offsetY = h / 2;

        /*生成logo*/
        Bitmap logoBitmap = null;

        if (logo != null) {
            Matrix matrix = new Matrix();
            float scaleFactor = Math.min(w * 1.0f / 5 / logo.getWidth(), h * 1.0f / 5 / logo.getHeight());
            matrix.postScale(scaleFactor, scaleFactor);
            logoBitmap = Bitmap.createBitmap(logo, 0, 0, logo.getWidth(), logo.getHeight(), matrix, true);
        }


        /*指定为UTF-8*/
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        //容错级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //设置空白边距的宽度
        hints.put(EncodeHintType.MARGIN, 0);
        // 生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
        BitMatrix matrix = null;
        try {
            matrix = new QRCodeWriter().encode(content,
                    BarcodeFormat.QR_CODE, w, h, hints);
            w = matrix.getWidth();
            h = matrix.getHeight();

            /*如果log不为null,重新计算偏移量*/
            int logoW = 0;
            int logoH = 0;
            if (logoBitmap != null) {
                logoW = logoBitmap.getWidth();
                logoH = logoBitmap.getHeight();
                offsetX = (w - logoW) / 2;
                offsetY = (h - logoH) / 2;
            }


            // 二维矩阵转为一维像素数组,也就是一直横着排了
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (x >= offsetX && x < offsetX + logoW && y >= offsetY && y < offsetY + logoH) {
                        int pixel = logoBitmap.getPixel(x - offsetX, y - offsetY);
                        if (pixel == 0) {
                            if (matrix.get(x, y)) {
                                pixel = 0xff000000;
                            } else {
                                pixel = 0xffffffff;
                            }
                        }
                        pixels[y * w + x] = pixel;
                    } else {
                        if (matrix.get(x, y)) {
                            pixels[y * w + x] = 0xff000000;
                        } else {
                            pixels[y * w + x] = 0xffffffff;
                        }
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(w, h,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
            return bitmap;


        } catch (WriterException e) {

            System.out.print(e);
            return null;
        }
    }

    public static void saveToSystemAlbumWithWatermark(FragmentActivity activity, Drawable drawable, Callback callBack) {

        if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            return;
        }
        int screenWidth = drawable.getIntrinsicWidth();
        int screenHeight = drawable.getIntrinsicHeight();
        Bitmap dst = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dst);
        drawable.draw(canvas);
//        Bitmap bitmap = BitmapFactory.decodeResource(BaseApp.getInstance().getResources(), R.drawable.ic_shuiiyn);
        int shortSize = Math.min(screenWidth, screenHeight);

        float ratio = 70f / 375;
        float margin = Math.max(SizeUtils.dp2px(1f), screenWidth * 12f / 375);//12/12  70/12
        float width = Math.max(SizeUtils.dp2px(5.83333f), screenWidth * ratio);

        if (width > screenWidth) {
            width = screenWidth;
        }

//        float height = width * bitmap.getHeight() / (float) bitmap.getWidth();
//        float start = screenWidth - width - margin;
//        float top = screenHeight - height - margin;
//        RectF rectF = new RectF(start, top, start + width, top + height);
//        canvas.drawBitmap(bitmap, null, rectF, null);
        saveToSystemAlbum(activity, dst, callBack);
    }


    private static String getAreaName(String province, String city, String spacer) {
        if (province != null) {
            province = getSimplyProvinceName(province);
        }

        city = getSimplyCityName(city);
        if (TextUtils.equals(province, city) || Constants.KEY_UNLIMITED.equals(city)) {
            return province;
        }
        return province + spacer + city;
    }

    public static String getSimplyProvinceName(String province) {
        if (province.endsWith("省") || province.equals("市")) {
            province = province.substring(0, province.length() - 1);
        } else if (province.endsWith("新疆维吾尔自治区")) {
            province = "新疆";
        } else if (province.endsWith("内蒙古自治区")) {
            province = "内蒙古";
        } else if (province.endsWith("广西壮族自治区")) {
            province = "广西";
        } else if (province.endsWith("宁夏回族自治区")) {
            province = "宁夏";
        } else if (province.endsWith("西藏自治区")) {
            province = "西藏";
        } else if (province.equals("香港特别行政区")) {
            province = "香港";
        } else if (province.equals("澳门特别行政区")) {
            province = "澳门";
        }
        return province;
    }

    public static String getSimplyCityName(String city) {
        if (city != null) {
            if (city.endsWith("市") || city.equals("县")) {
                city = city.substring(0, city.length() - 1);
            } else if (city.equals("延边朝鲜族自治州")) {
                city = "延边";
            } else if (city.equals("恩施土家族苗族自治州")) {
                city = "恩施";
            } else if (city.equals("湘西土家族苗族自治州")) {
                city = "湘西";
            } else if (city.equals("阿坝藏族羌族自治州")) {
                city = "阿坝";
            } else if (city.equals("甘孜藏族自治州")) {
                city = "甘孜";
            } else if (city.equals("凉山彝族自治州")) {
                city = "凉山";
            } else if (city.equals("黔东南苗族侗族自治州")) {
                city = "黔东南";
            } else if (city.equals("黔南布依族苗族自治州")) {
                city = "黔南";
            } else if (city.equals("黔西南布依族苗族自治州")) {
                city = "黔西南";
            } else if (city.equals("楚雄彝族自治州")) {
                city = "楚雄";
            } else if (city.equals("红河哈尼族彝族自治州")) {
                city = "红河";
            } else if (city.equals("文山壮族苗族自治州")) {
                city = "文山";
            } else if (city.equals("西双版纳傣族自治州")) {
                city = "西双版纳";
            } else if (city.equals("大理白族自治州")) {
                city = "大理";
            } else if (city.equals("德宏傣族景颇族自治州")) {
                city = "德宏";
            } else if (city.equals("怒江傈僳族自治州")) {
                city = "怒江";
            } else if (city.equals("迪庆藏族自治州")) {
                city = "迪庆";
            } else if (city.equals("临夏回族自治州")) {
                city = "临夏";
            } else if (city.equals("甘南藏族自治州")) {
                city = "甘南";
            } else if (city.equals("海南藏族自治州")) {
                city = "海南";
            } else if (city.equals("海北藏族自治州")) {
                city = "海北";
            } else if (city.equals("海西蒙古族藏族自治州")) {
                city = "海西";
            } else if (city.equals("黄南藏族自治州")) {
                city = "黄南";
            } else if (city.equals("果洛藏族自治州")) {
                city = "果洛";
            } else if (city.equals("玉树藏族自治州")) {
                city = "玉树";
            } else if (city.equals("伊犁哈萨克自治州")) {
                city = "伊犁";
            } else if (city.equals("博尔塔拉蒙古自治州")) {
                city = "博尔塔拉";
            } else if (city.equals("昌吉回族自治州")) {
                city = "昌吉";
            } else if (city.equals("巴音郭楞蒙古自治州")) {
                city = "巴音郭楞";
            } else if (city.equals("克孜勒苏柯尔克孜自治州")) {
                city = "克孜勒苏";
            }

        }
        return city;
    }

    public static boolean isActivityFinishing(Context context) {
        if (context instanceof BaseActivity) {
            if (!LifecycleUtils.isActive((BaseActivity) context)) {
                return true;
            }
        }
        return ((Activity) context).isFinishing();
    }

    public static boolean hasPermission(String... permissions) {
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(App.getInstance(), permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static String parseName(String url) {
        int lastPos = url.lastIndexOf("/");
        if (lastPos != -1) {
            return url.substring(lastPos + 1);
        }
        return url;
    }

    public static boolean isAppInBackground() {
        // 需在主线程判断，或者通过 LifecycleObserver 监听状态变化
        return !ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    public static String mappingPdfId(String path) {
        return EncryptUtils.md5(path) + ".pdf";
    }

    public static void setBlurBackground(Context context, ImageView coverView, ImageView coverBgView, String coverImageUrl, int defaultResId) {
        setBlurBackground(context, coverView, coverBgView, coverImageUrl, defaultResId, true);
    }

    public static void setBlurBackground(Context context, ImageView coverView, ImageView coverBgView, String coverImageUrl, int defaultResId, boolean useThumbnail) {
        setBlurBackground(context, coverView, coverBgView, coverImageUrl, defaultResId, useThumbnail, null);
    }

    //
    public static void setBlurBackground(Context context, ImageView coverView, ImageView coverBgView, String coverImageUrl, int defaultResId, boolean useThumbnail, RequestListener<Drawable> blueBitmapRequestListener) {
        GlideImageLoader glide = useThumbnail ? GlideImageLoader.with(coverView).thumbnail() : GlideImageLoader.with(coverView);

        glide.load(coverImageUrl).placeholder(defaultResId).addListener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                if (blueBitmapRequestListener != null) {
                    blueBitmapRequestListener.onLoadFailed(e, model, target, isFirstResource);
                }
                return false;

            }

            @Override
            public boolean onResourceReady(Drawable drawable, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                Bitmap blurBitmap = ImageUtils.fastBlur(((BitmapDrawable) drawable).getBitmap(), 0.2f, 25f);
                coverBgView.setImageBitmap(blurBitmap);
                if (blueBitmapRequestListener != null) {
                    blueBitmapRequestListener.onResourceReady(new BitmapDrawable(blurBitmap), model, target, dataSource, isFirstResource);
                }
                return false;
            }
        }).into(coverView);
    }

    public static void saveToSystemAlbum(FragmentActivity context, Bitmap bitmap, Callback callBack) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifySystem(new File(FileUtils.savaImage(bitmap, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath())));
            callBack.callBack(true);
            return;
        }
        PermissionsExt.requestRxPermissions(
                context,
                context.getString(R.string.sdcard_permission_description),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                granted -> {
                    if (granted) {
                        callBack.callBack(true);
                        notifySystem(new File(FileUtils.savaImage(bitmap, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath())));
                    } else {
                        //权限申请失败
                        callBack.callBack(false);
                    }
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    public static void notifySystem(File file) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        BaseApp.getInstance().sendBroadcast(intent);
    }


    public static void takePhoto(final FragmentActivity activity, final int requestCode, final File imagePath) {
        PermissionsExt.requestRxPermissions(activity, R.string.camera_permission_description, PermissionsUtils.getCameraPermission(), new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean allGranted) {
                if (allGranted) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (imagePath.exists())
                        imagePath.delete();
                    Uri outputFileUri = getFileUri(activity, imagePath);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    if (intent.resolveActivity(activity.getPackageManager()) != null) {
                        activity.startActivityForResult(intent, requestCode);
                    }
                }
                return null;
            }
        });
    }

    public static Uri getFileUri(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, FileManager.getInstance().getFileProvider(), file);
        } else {
            return Uri.fromFile(file);
        }
    }

    @SuppressLint("MissingPermission")
    public static void callPhoneWithPermission(FragmentActivity context, String mobileNumber) {
        if (TextUtils.isEmpty(mobileNumber)) {
            ToastExtKt.showToast("号码错误！");
            return;
        }
        PermissionsExt.requestRxPermissions(
                context,
                context.getString(R.string.call_phone_permission_description),
                new String[]{Manifest.permission.CALL_PHONE},
                granted -> {
                    if (granted) {
                        PhoneUtils.call(mobileNumber);
                    } else {
                        //权限申请失败
                    }
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    public static Uri getUriFromResource(Context context, int resId) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + context.getResources().getResourcePackageName(resId) + "/"
                + context.getResources().getResourceTypeName(resId) + "/"
                + context.getResources().getResourceEntryName(resId));
    }

    public static void selectFileFromFolder(Activity context, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        Intent i = Intent.createChooser(intent, "File");
        context.startActivityForResult(i, requestCode);
    }

    public static String getFileName(String path) {
        File file = new File(path);
        String fileName = file.getName();
        String[] split = fileName.split("\\.");
        String fullName = split[0];
        String[] nameAndPaths = fullName.split(File.pathSeparator);
        return nameAndPaths[nameAndPaths.length - 1];
    }

    public static long getAudioDuration(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        long duration = 0;
        try {
            retriever.setDataSource(filePath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.parseLong(durationStr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
            }
        }
        return duration;
    }

    /**
     * 生成视频封面截图
     */
    public static String getVideoCover(String videoPath) {
        String coverPath = null;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();//实例化MediaMetadataRetriever对象
        File file = new File(videoPath);//实例化File对象，文件路径为/storage/sdcard/Movies/music1.mp4
        if (file.exists()) {
            mmr.setDataSource(file.getAbsolutePath());//设置数据源为该文件对象指定的绝对路径
            Bitmap bitmap = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);//获得视频第一帧的Bitmap对象
            if (bitmap != null) {
                File cover = new File(FileManager.getInstance().getImageFolder(), System.currentTimeMillis() + ".jpg");
                coverPath = cover.getAbsolutePath();
                ImageUtils.save(bitmap, cover, Bitmap.CompressFormat.PNG, 100, false);

            }

        }
        return coverPath;
    }
}

