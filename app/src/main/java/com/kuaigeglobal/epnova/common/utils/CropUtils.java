package com.kuaigeglobal.epnova.common.utils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.nhsd.crop.Crop;
import com.kuaigeglobal.epnova.R;

import kotlin.Pair;

/**
 * UCropUtils
 * Created by Yancy on 2016/11/2.
 */
public class CropUtils {
    private static final int MAX_SIZE = 2400;

    public static void start(Fragment fragment, Uri source, @NonNull Uri destination, int statusBarColor, int widgetColor, @NonNull Pair<Float, Float> cropWHRatio) {
        Crop crop = Crop.of(source, destination)
                .withAspectRatio(cropWHRatio.getFirst(), cropWHRatio.getSecond())
                .withMaxResultSize(MAX_SIZE, MAX_SIZE);

        Crop.Options options = new Crop.Options();
        options.setToolbarColor(statusBarColor);
        options.setStatusBarColor(statusBarColor);
        options.setToolbarWidgetColor(widgetColor);
//        options.setActiveControlsWidgetColor(widgetColor);
        options.setActiveWidgetColor(widgetColor);
        crop.withOptions(options);
        crop.start(fragment.getActivity(), fragment);
    }

    public static void start(Activity activity, Uri source, @NonNull Uri destination, int statusBarColor, int widgetColor, @NonNull Pair<Float, Float> cropWHRatio) {
        Crop crop = Crop.of(source, destination)
                .withAspectRatio(cropWHRatio.getFirst(), cropWHRatio.getSecond())
                .withMaxResultSize(MAX_SIZE, MAX_SIZE);

        Crop.Options options = new Crop.Options();
        options.setToolbarColor(statusBarColor);
        options.setStatusBarColor(statusBarColor);
        options.setToolbarWidgetColor(widgetColor);
//        options.setActiveControlsWidgetColor(widgetColor);
        options.setActiveWidgetColor(widgetColor);
        crop.withOptions(options);
        crop.start(activity);
    }

    public static void crop(Activity context, Uri source) {
        int color = context.getResources().getColor(R.color.textColorDark);
        CropUtils.start(context, source, Uri.fromFile(FileUtils.newTempImageFile()), getColorPrimary(context), color, new Pair<>(0f, 0f));
    }

    public static void crop(Activity context, Uri source, @NonNull Pair<Float, Float> cropWHRatio) {
        int color = context.getResources().getColor(R.color.textColorDark);
        CropUtils.start(context, source, Uri.fromFile(FileUtils.newTempImageFile()), getColorPrimary(context), color, cropWHRatio);
    }

    public static void crop(Fragment context, Uri source) {
        int color = context.getResources().getColor(R.color.textColorDark);
        CropUtils.start(context, source, Uri.fromFile(FileUtils.newTempImageFile()), getColorPrimary(context.getActivity()), color, new Pair<>(0f, 0f));
    }

    public static int getColorPrimary(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    public static void crop(Fragment context, Uri source, @NonNull Pair<Float, Float> cropWHRatio) {
        int color = context.getResources().getColor(R.color.textColorDark);
        CropUtils.start(context, source, Uri.fromFile(FileUtils.newTempImageFile()), getColorPrimary(context.getActivity()), color, cropWHRatio);
    }

}
/*
 *   ┏┓　　　┏┓
 * ┏┛┻━━━┛┻┓
 * ┃　　　　　　　┃
 * ┃　　　━　　　┃
 * ┃　┳┛　┗┳　┃
 * ┃　　　　　　　┃
 * ┃　　　┻　　　┃
 * ┃　　　　　　　┃
 * ┗━┓　　　┏━┛
 *     ┃　　　┃
 *     ┃　　　┃
 *     ┃　　　┗━━━┓
 *     ┃　　　　　　　┣┓
 *     ┃　　　　　　　┏┛
 *     ┗┓┓┏━┳┓┏┛
 *       ┃┫┫　┃┫┫
 *       ┗┻┛　┗┻┛
 *        神兽保佑
 *        代码无BUG!
 */