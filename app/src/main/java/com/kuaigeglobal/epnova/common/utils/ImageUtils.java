package com.kuaigeglobal.epnova.common.utils;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.UriUtils;

// 假设你使用 Luban 压缩库
import com.nhsd.luban.CompressionPredicate;
import com.nhsd.luban.Luban;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <pre>
 * desc  : 综合图片工具类 (整合版)
 * 包含了 Bitmap/Drawable/Bytes 转换、压缩、缩放、裁剪、圆角、模糊、保存、
 * Luban 压缩、Tint 着色等功能。
 * </pre>
 */
public final class ImageUtils {

    private static final String TAG = "ImageUtils";
    public static final int QUALITY_NOT_WIFI = 70;
    public static final int QUALITY_WIFI = 80;
    private static final int BITMAP_TEMP_STORAGE = 32 * 1024;

    private ImageUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /* =================================================================================== */
    /* 转换相关 (Convert)                                */
    /* =================================================================================== */

    /**
     * Bitmap to bytes.
     */
    public static byte[] bitmap2Bytes(final Bitmap bitmap) {
        return bitmap2Bytes(bitmap, CompressFormat.PNG, 100);
    }

    /**
     * Bitmap to bytes.
     */
    public static byte[] bitmap2Bytes(@Nullable final Bitmap bitmap, @NonNull final CompressFormat format, int quality) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, quality, baos);
        return baos.toByteArray();
    }

    /**
     * Bytes to bitmap.
     */
    public static Bitmap bytes2Bitmap(@Nullable final byte[] bytes) {
        return (bytes == null || bytes.length == 0)
                ? null
                : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Drawable to bitmap.
     */
    public static Bitmap drawable2Bitmap(@Nullable final Drawable drawable) {
        if (drawable == null) return null;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1,
                    drawable.getOpacity() != PixelFormat.OPAQUE
                            ? Bitmap.Config.ARGB_8888
                            : Bitmap.Config.RGB_565);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE
                            ? Bitmap.Config.ARGB_8888
                            : Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Bitmap to drawable.
     */
    public static Drawable bitmap2Drawable(@Nullable final Bitmap bitmap) {
        return bitmap == null ? null : new BitmapDrawable(Utils.getApp().getResources(), bitmap);
    }

    /**
     * Drawable to bytes.
     */
    public static byte[] drawable2Bytes(@Nullable final Drawable drawable) {
        return drawable == null ? null : bitmap2Bytes(drawable2Bitmap(drawable));
    }

    /**
     * Drawable to bytes.
     */
    public static byte[] drawable2Bytes(final Drawable drawable, final CompressFormat format, int quality) {
        return drawable == null ? null : bitmap2Bytes(drawable2Bitmap(drawable), format, quality);
    }

    /**
     * Bytes to drawable.
     */
    public static Drawable bytes2Drawable(final byte[] bytes) {
        return bitmap2Drawable(bytes2Bitmap(bytes));
    }

    /**
     * View to bitmap.
     */
    public static Bitmap view2Bitmap(final View view) {
        return view2Bitmap(view, Bitmap.Config.ARGB_8888);
    }

    /**
     * View to bitmap.
     */
    public static Bitmap view2Bitmap(final View view, Bitmap.Config config) {
        if (view == null) return null;
        // 处理 DrawingCache 的旧逻辑已移除，改用标准的 Canvas 绘制
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), config);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE); // 默认白色背景，防止黑底
        }
        view.draw(canvas);
        return bitmap;
    }

    /* =================================================================================== */
    /* 获取 Bitmap (Get/Decode)                          */
    /* =================================================================================== */

    /**
     * Return bitmap from file.
     */
    public static Bitmap getBitmap(final File file) {
        if (file == null) return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    /**
     * Return bitmap from file with max width/height limits.
     */
    public static Bitmap getBitmap(final File file, final int maxWidth, final int maxHeight) {
        if (file == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    /**
     * Return bitmap from path.
     */
    public static Bitmap getBitmap(final String filePath) {
        if (StringUtils.isSpace(filePath)) return null;
        return BitmapFactory.decodeFile(filePath);
    }

    /**
     * Return bitmap from path with limits.
     */
    public static Bitmap getBitmap(final String filePath, final int maxWidth, final int maxHeight) {
        if (StringUtils.isSpace(filePath)) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static Bitmap getBitmap(final InputStream is) {
        if (is == null) return null;
        return BitmapFactory.decodeStream(is);
    }

    public static Bitmap getBitmap(final InputStream is, final int maxWidth, final int maxHeight) {
        if (is == null) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(is, null, options);
    }

    public static Bitmap getBitmap(@DrawableRes final int resId) {
        Drawable drawable = ContextCompat.getDrawable(Utils.getApp(), resId);
        if (drawable == null) return null;
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap getBitmap(@DrawableRes final int resId, final int maxWidth, final int maxHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        final Resources resources = Utils.getApp().getResources();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resId, options);
    }

    /**
     * Load bitmap from Asset
     */
    public static Bitmap getBitmapFromAsset(AssetManager am, String path) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inPreferredConfig = Bitmap.Config.ARGB_8888;
        InputStream in = null;
        try {
            in = am.open(path);
            return BitmapFactory.decodeStream(in, null, option);
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * 生成缩略图
     */
    public static Bitmap getThumbnailImage(String imagePath, int width, int height) {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false;
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    /* =================================================================================== */
    /* 处理与编辑 (Process/Edit)                         */
    /* =================================================================================== */

    /**
     * Return the bitmap with the specified color.
     */
    public static Bitmap drawColor(@NonNull final Bitmap src, @ColorInt final int color) {
        return drawColor(src, color, false);
    }

    public static Bitmap drawColor(@NonNull final Bitmap src, @ColorInt final int color, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        Bitmap ret = recycle ? src : src.copy(src.getConfig(), true);
        Canvas canvas = new Canvas(ret);
        canvas.drawColor(color, PorterDuff.Mode.DARKEN);
        return ret;
    }

    /**
     * Scale the bitmap.
     */
    public static Bitmap scale(final Bitmap src, final int newWidth, final int newHeight) {
        return scale(src, newWidth, newHeight, false);
    }

    public static Bitmap scale(final Bitmap src, final int newWidth, final int newHeight, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        Bitmap ret = Bitmap.createScaledBitmap(src, newWidth, newHeight, true);
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    public static Bitmap scale(final Bitmap src, final float scaleWidth, final float scaleHeight) {
        return scale(src, scaleWidth, scaleHeight, false);
    }

    public static Bitmap scale(final Bitmap src, final float scaleWidth, final float scaleHeight, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        Matrix matrix = new Matrix();
        matrix.setScale(scaleWidth, scaleHeight);
        Bitmap ret = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /**
     * Clip the bitmap.
     */
    public static Bitmap clip(final Bitmap src, final int x, final int y, final int width, final int height) {
        return clip(src, x, y, width, height, false);
    }

    public static Bitmap clip(final Bitmap src, final int x, final int y, final int width, final int height, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        int realX = Math.max(0, x);
        int realY = Math.max(0, y);
        int realWidth = Math.min(width, src.getWidth() - realX);
        int realHeight = Math.min(height, src.getHeight() - realY);
        if (realWidth <= 0 || realHeight <= 0) return null;

        Bitmap ret = Bitmap.createBitmap(src, realX, realY, realWidth, realHeight);
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /**
     * Rotate the bitmap.
     */
    public static Bitmap rotate(final Bitmap src, final int degrees, final float px, final float py) {
        return rotate(src, degrees, px, py, false);
    }

    public static Bitmap rotate(final Bitmap src, final int degrees, final float px, final float py, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        if (degrees == 0) return src;
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, px, py);
        Bitmap ret = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /**
     * Read picture degree (Rotation).
     */
    public static int getRotateDegree(final String filePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Convert to Round Bitmap.
     */
    public static Bitmap toRound(final Bitmap src) {
        return toRound(src, 0, 0, false);
    }

    public static Bitmap toRound(final Bitmap src, @IntRange(from = 0) int borderSize, @ColorInt int borderColor, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        int width = src.getWidth();
        int height = src.getHeight();
        int size = Math.min(width, height);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bitmap ret = Bitmap.createBitmap(width, height, src.getConfig());
        float center = size / 2f;
        RectF rectF = new RectF(0, 0, width, height);
        rectF.inset((width - size) / 2f, (height - size) / 2f);
        Matrix matrix = new Matrix();
        matrix.setTranslate(rectF.left, rectF.top);
        if (width != height) {
            matrix.preScale((float) size / width, (float) size / height);
        }
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        shader.setLocalMatrix(matrix);
        paint.setShader(shader);
        Canvas canvas = new Canvas(ret);
        canvas.drawRoundRect(rectF, center, center, paint);
        if (borderSize > 0) {
            paint.setShader(null);
            paint.setColor(borderColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(borderSize);
            float radius = center - borderSize / 2f;
            canvas.drawCircle(width / 2f, height / 2f, radius, paint);
        }
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /**
     * Convert to Round Corner Bitmap.
     */
    public static Bitmap toRoundCorner(final Bitmap src, final float radius) {
        return toRoundCorner(src, radius, 0, 0, false);
    }

    public static Bitmap toRoundCorner(final Bitmap src, final float radius, @FloatRange(from = 0) float borderSize, @ColorInt int borderColor, final boolean recycle) {
        float[] radii = {radius, radius, radius, radius, radius, radius, radius, radius};
        return toRoundCorner(src, radii, borderSize, borderColor, recycle);
    }

    public static Bitmap toRoundCorner(final Bitmap src, final float[] radii, @FloatRange(from = 0) float borderSize, @ColorInt int borderColor, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        int width = src.getWidth();
        int height = src.getHeight();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bitmap ret = Bitmap.createBitmap(width, height, src.getConfig());
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        Canvas canvas = new Canvas(ret);
        RectF rectF = new RectF(0, 0, width, height);
        float halfBorderSize = borderSize / 2f;
        rectF.inset(halfBorderSize, halfBorderSize);
        Path path = new Path();
        path.addRoundRect(rectF, radii, Path.Direction.CW);
        canvas.drawPath(path, paint);
        if (borderSize > 0) {
            paint.setShader(null);
            paint.setColor(borderColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(borderSize);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawPath(path, paint);
        }
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /**
     * Add reflection to bitmap.
     */
    public static Bitmap addReflection(final Bitmap src, final int reflectionHeight, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        final int REFLECTION_GAP = 0;
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);
        Bitmap reflectionBitmap = Bitmap.createBitmap(src, 0, srcHeight - reflectionHeight,
                srcWidth, reflectionHeight, matrix, false);
        Bitmap ret = Bitmap.createBitmap(srcWidth, srcHeight + reflectionHeight, src.getConfig());
        Canvas canvas = new Canvas(ret);
        canvas.drawBitmap(src, 0, 0, null);
        canvas.drawBitmap(reflectionBitmap, 0, srcHeight + REFLECTION_GAP, null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient shader = new LinearGradient(
                0, srcHeight,
                0, ret.getHeight() + REFLECTION_GAP,
                0x70FFFFFF,
                0x00FFFFFF,
                Shader.TileMode.MIRROR);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, srcHeight + REFLECTION_GAP, srcWidth, ret.getHeight(), paint);
        if (!reflectionBitmap.isRecycled()) reflectionBitmap.recycle();
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /**
     * Add text watermark.
     */
    public static Bitmap addTextWatermark(final Bitmap src, final String content, final float textSize, @ColorInt final int color, final float x, final float y, final boolean recycle) {
        if (isEmptyBitmap(src) || content == null) return null;
        Bitmap ret = src.copy(src.getConfig(), true);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Canvas canvas = new Canvas(ret);
        paint.setColor(color);
        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(content, 0, content.length(), bounds);
        canvas.drawText(content, x, y + textSize, paint);
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /**
     * Convert to Gray/Grayscale.
     */
    public static Bitmap toGray(final Bitmap src, final boolean recycle) {
        if (isEmptyBitmap(src)) return null;
        Bitmap ret = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(ret);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixColorFilter);
        canvas.drawBitmap(src, 0, 0, paint);
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    /* =================================================================================== */
    /* 模糊处理 (Blur)                                   */
    /* =================================================================================== */
    /**
     * Fast Blur.
     */
    public static Bitmap fastBlur(final Bitmap src, final float scale) {
        return fastBlur(src, scale, 25, false, false);
    }

    /**
     * Fast Blur.
     */
    public static Bitmap fastBlur(final Bitmap src, @FloatRange(from = 0, to = 1, fromInclusive = false) final float scale, @FloatRange(from = 0, to = 25, fromInclusive = false) final float radius) {
        return fastBlur(src, scale, radius, false, false);
    }

    public static Bitmap fastBlur(final Bitmap src, @FloatRange(from = 0, to = 1, fromInclusive = false) final float scale, @FloatRange(from = 0, to = 25, fromInclusive = false) final float radius, final boolean recycle, final boolean isReturnScale) {
        if (isEmptyBitmap(src)) return null;
        int width = src.getWidth();
        int height = src.getHeight();
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        Bitmap scaleBitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        Canvas canvas = new Canvas();
        PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_ATOP);
        paint.setColorFilter(filter);
        canvas.scale(scale, scale);
        canvas.drawBitmap(scaleBitmap, 0, 0, paint);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            scaleBitmap = renderScriptBlur(scaleBitmap, radius, recycle);
        } else {
            scaleBitmap = stackBlur(scaleBitmap, (int) radius, recycle);
        }
        if (scale == 1 || isReturnScale) {
            if (recycle && !src.isRecycled() && scaleBitmap != src) src.recycle();
            return scaleBitmap;
        }
        Bitmap ret = Bitmap.createScaledBitmap(scaleBitmap, width, height, true);
        if (!scaleBitmap.isRecycled()) scaleBitmap.recycle();
        if (recycle && !src.isRecycled() && ret != src) src.recycle();
        return ret;
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap renderScriptBlur(final Bitmap src, @FloatRange(from = 0, to = 25, fromInclusive = false) final float radius, final boolean recycle) {
        // 1. 变量声明提到外面，初始化为 null
        RenderScript rs = null;
        Allocation input = null;
        Allocation output = null;
        ScriptIntrinsicBlur blurScript = null;

        Bitmap ret = recycle ? src : src.copy(src.getConfig(), true);

        try {
            rs = RenderScript.create(Utils.getApp());
            rs.setMessageHandler(new RenderScript.RSMessageHandler());

            // 2. 赋值
            input = Allocation.createFromBitmap(rs, ret, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            output = Allocation.createTyped(rs, input.getType());
            blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

            blurScript.setInput(input);
            blurScript.setRadius(radius);
            blurScript.forEach(output);
            output.copyTo(ret);
        } catch (Exception e) {
            // Fallback to stack blur if RS fails
            return stackBlur(src, (int)radius, recycle);
        } finally {
            // 3. 显式销毁所有对象（建议按创建的反向顺序销毁）
            if (blurScript != null) {
                blurScript.destroy(); // 必须调用，否则报 StrictMode 错误
            }
            if (input != null) {
                input.destroy();      // 必须调用，释放 Native 内存
            }
            if (output != null) {
                output.destroy();     // 必须调用，释放 Native 内存
            }
            if (rs != null) {
                rs.destroy();         //最后销毁上下文
            }
        }
        return ret;
    }

    /**
     * Stack Blur (Java implementation, slower but compatible).
     * 代码较长，直接复用原文件中的实现。
     */
    public static Bitmap stackBlur(final Bitmap src, int radius, final boolean recycle) {
        Bitmap ret = recycle ? src : src.copy(src.getConfig(), true);
        if (radius < 1) {
            radius = 1;
        }
        int w = ret.getWidth();
        int h = ret.getHeight();

        int[] pix = new int[w * h];
        ret.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
        ret.setPixels(pix, 0, w, 0, 0, w, h);
        return ret;
    }

    /* =================================================================================== */
    /* 保存相关 (Save)                                   */
    /* =================================================================================== */

    /**
     * Save the bitmap to file.
     */
    public static boolean save(final Bitmap src, final File file, final CompressFormat format, final int quality, final boolean recycle) {
        if (isEmptyBitmap(src)) {
            Log.e("ImageUtils", "bitmap is empty.");
            return false;
        }
        if (src.isRecycled()) {
            Log.e("ImageUtils", "bitmap is recycled.");
            return false;
        }
        if (!FileUtils.createFileByDeleteOldFile(file)) {
            Log.e("ImageUtils", "create or delete file <" + file + "> failed.");
            return false;
        }
        OutputStream os = null;
        boolean ret = false;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            ret = src.compress(format, quality, os);
            if (recycle && !src.isRecycled()) src.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
        return ret;
    }

    /**
     * Save to Album (适配 Android Q).
     */
    @Nullable
    public static File save2Album(final Bitmap src, final String dirName, final String fileName, final CompressFormat format, final int quality, final boolean recycle) {
        String safeDirName = StringUtils.isSpace(dirName) ? Utils.getApp().getPackageName() : dirName;
        String suffix = CompressFormat.JPEG.equals(format) ? "JPG" : format.name();
        String safeFileName = StringUtils.isSpace(fileName) ? System.currentTimeMillis() + "_" + quality + "." + suffix : fileName;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!Utils.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.e("ImageUtils", "save to album need storage permission");
                return null;
            }
            File picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File destFile = new File(picDir, safeDirName + "/" + safeFileName);
            if (!save(src, destFile, format, quality, recycle)) {
                return null;
            }
            FileUtils.notifySystemToScan(destFile);
            return destFile;
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, safeFileName);
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
            Uri contentUri;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else {
                contentUri = MediaStore.Images.Media.INTERNAL_CONTENT_URI;
            }
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/" + safeDirName);
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = Utils.getApp().getContentResolver().insert(contentUri, contentValues);
            if (uri == null) {
                return null;
            }
            OutputStream os = null;
            try {
                os = Utils.getApp().getContentResolver().openOutputStream(uri);
                src.compress(format, quality, os);

                contentValues.clear();
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                Utils.getApp().getContentResolver().update(uri, contentValues, null, null);

                return UriUtils.uri2File(uri);
            } catch (Exception e) {
                Utils.getApp().getContentResolver().delete(uri, null, null);
                e.printStackTrace();
                return null;
            } finally {
                if (os != null) {
                    try { os.close(); } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }
    }

    /**
     * 异步保存图片 (来自原 com.nhsd.basic.utils)
     */
    public interface OnViewBitmapFileGetListener {
        void onViewBitmapGet(File imageFile);
    }

    public static void saveBitmapToLocalAsync(final Bitmap bitmap, final File targetFile, final OnViewBitmapFileGetListener listener) {
        AsyncTask<Void, Void, File> asyncTask = new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... voids) {
                if (save(bitmap, targetFile, CompressFormat.JPEG, 100, false)) {
                    return targetFile;
                }
                return null;
            }

            @Override
            protected void onPostExecute(File result) {
                super.onPostExecute(result);
                if (listener != null) {
                    listener.onViewBitmapGet(result);
                }
            }
        };
        asyncTask.execute();
    }

    /* =================================================================================== */
    /* 辅助与工具 (Utils/Helpers)                        */
    /* =================================================================================== */

    /**
     * 调用系统裁剪 Intent
     */
    public static void startPicCut(Activity activity, Uri uri, int outputX, int outputY, int requestCode) {
        Intent intentCamera = new Intent("com.android.camera.action.CROP");
        intentCamera.setDataAndType(uri, "image/*");
        intentCamera.putExtra("crop", true);
        intentCamera.putExtra("aspectX", 1);
        intentCamera.putExtra("aspectY", 1);
        intentCamera.putExtra("outputX", outputX);
        intentCamera.putExtra("outputY", outputY);
        intentCamera.putExtra("return-data", true);
        activity.startActivityForResult(intentCamera, requestCode);
    }

    /**
     * 图片着色 (Tint)
     * @param fraction 0-1 之间，用于计算颜色过渡
     */
    public static void setTint(ImageView imageView, int drawableRes, int colorStartValue, int colorEndValue, float fraction) {
        ArgbEvaluator argbEvaluator = new ArgbEvaluator();
        int color = (int) argbEvaluator.evaluate(fraction, colorStartValue, colorEndValue);
        Drawable backDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(imageView.getContext(), drawableRes));
        DrawableCompat.setTint(backDrawable, color);
        imageView.setImageDrawable(backDrawable);
    }

    public static void setWhiteBlackTint(ImageView imageView, int drawableRes, float fraction) {
        setTint(imageView, drawableRes, Color.BLACK, Color.WHITE, fraction);
    }

    /* =================================================================================== */
    /* 压缩 (Compress/Luban)                             */
    /* =================================================================================== */

    /**
     * 使用 Luban 压缩 (需要 Luban 依赖)
     * 替换了原 BaseApp 和 FileManager 的依赖，改为传入 Context 和 目标目录
     */
    public static File compressWithLuban(Context context, final String srcFilePath, final String targetDir) {
        try {
            return Luban.with(context)
                    .ignoreBy(100)
                    .setTargetDir(targetDir)
                    .filter(new CompressionPredicate() {
                        @Override
                        public boolean apply(String path) {
                            return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                        }
                    })
                    .get(srcFilePath);
        } catch (Throwable e) {
            e.printStackTrace();
            return new File(srcFilePath);
        }
    }

    /**
     * 手动质量压缩 (Smart Quality Compress)
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean compressSmart(Bitmap bitmapOrg, File destFile, int maxLimit) {
        int quality = NetworkUtils.isWifiConnected() ? QUALITY_WIFI : QUALITY_NOT_WIFI;
        return compressSmart(bitmapOrg, destFile, quality, maxLimit);
    }

    public static boolean compressSmart(Bitmap bitmapOrg, File destFile, int quality, int maxLimit) {
        if (bitmapOrg == null) return false;

        // 尺寸压缩
        int orgWidth = bitmapOrg.getWidth();
        int orgHeight = bitmapOrg.getHeight();
        if (orgWidth > maxLimit || orgHeight > maxLimit) {
            float scale = maxLimit / (Math.max(orgWidth, orgHeight) * 1.0f);
            bitmapOrg = scale(bitmapOrg, scale, scale, false);
        }

        // 质量压缩并保存
        return save(bitmapOrg, destFile, CompressFormat.JPEG, quality, true);
    }

    /**
     * 计算采样率
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static boolean isEmptyBitmap(final Bitmap src) {
        return src == null || src.getWidth() == 0 || src.getHeight() == 0;
    }

    /**
     * 释放 Bitmap 集合
     */
    public static void release(HashMap<String, Bitmap> bitmaps) {
        Iterator<Map.Entry<String, Bitmap>> iter = bitmaps.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bitmap> entry = iter.next();
            Bitmap bmp = entry.getValue();
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
    }

    /* =================================================================================== */
    /* 内部类与枚举                                      */
    /* =================================================================================== */

    public enum ImageType {
        TYPE_JPG("jpg"),
        TYPE_PNG("png"),
        TYPE_GIF("gif"),
        TYPE_TIFF("tiff"),
        TYPE_BMP("bmp"),
        TYPE_WEBP("webp"),
        TYPE_ICO("ico"),
        TYPE_UNKNOWN("unknown");

        String value;
        ImageType(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    /**
     * Return the type of image.
     */
    public static ImageType getImageType(final File file) {
        if (file == null) return null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return getImageType(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    public static ImageType getImageType(final InputStream is) {
        if (is == null) return null;
        try {
            byte[] bytes = new byte[12];
            return is.read(bytes) != -1 ? getImageType(bytes) : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ImageType getImageType(final byte[] bytes) {
        String type = ConvertUtils.bytes2HexString(bytes).toUpperCase();
        if (type.contains("FFD8FF")) {
            return ImageType.TYPE_JPG;
        } else if (type.contains("89504E47")) {
            return ImageType.TYPE_PNG;
        } else if (type.contains("47494638")) {
            return ImageType.TYPE_GIF;
        } else if (type.contains("49492A00") || type.contains("4D4D002A")) {
            return ImageType.TYPE_TIFF;
        } else if (type.contains("424D")) {
            return ImageType.TYPE_BMP;
        } else if (type.startsWith("52494646") && type.endsWith("57454250")) {
            return ImageType.TYPE_WEBP;
        } else if (type.contains("00000100") || type.contains("00000200")) {
            return ImageType.TYPE_ICO;
        } else {
            return ImageType.TYPE_UNKNOWN;
        }
    }
}