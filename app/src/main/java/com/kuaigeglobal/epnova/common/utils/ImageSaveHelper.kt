package com.kuaigeglobal.epnova.common.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.permissions.PermissionsUtils
import com.kuaigeglobal.epnova.common.permissions.requestRxPermissions
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ImageSaveHelper {

    @JvmStatic
    fun saveImage(activity: FragmentActivity, url: String) {
        val android12WritePermission = PermissionsUtils.getAndroid12WritePermission()
        activity.requestRxPermissions(R.string.sdcard_permission_description, *android12WritePermission) { granted: Boolean ->
            if (granted) {
                //权限申请成功
                // 使用 Activity 的生命周期，避免内存泄漏
                activity.lifecycleScope.launch {
                    val success = saveImageToGallery(activity, url)
                    if (success) {
                        showToast(R.string.saved_to_album)
                    } else {
                        showToast(R.string.toast_save_fail)
                    }
                }
//                    new ImageCacheAsyncTask(context) {
//                        @Override
//                        protected void onPostExecute(File result) {
//                            super.onPostExecute(result);
//                            ToastUtils.showShort("已保存到手机相册");
//                        }
//                    }.execute(url);
            } else {
                //权限申请失败
            }
            null
        }
    }

    /**
     * 挂起函数：下载并保存图片到相册
     * @return 保存成功返回 true，失败返回 false
     */
    suspend fun saveImageToGallery(context: Context, url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 使用 Glide 下载文件 (不加载到内存 Bitmap，保持原图质量)
                val sourceFile = Glide.with(context)
                    .asFile()
                    .load(url)
                    .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get()

                // 2. 根据 Android 版本选择保存方式
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(context, sourceFile)
                } else {
                    saveToLegacyStorage(context, sourceFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Android 10 (Q) 及以上：使用 MediaStore 插入 (无需写权限)
    private fun saveToMediaStore(context: Context, sourceFile: File): Boolean {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1) // 标记为处理中，避免相册此时扫描到半成品
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        return if (uri != null) {
            try {
                // 复制文件流
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // 处理完成，解除 PENDING 状态
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                // 失败了删除这个空记录
                resolver.delete(uri, null, null)
                false
            }
        } else {
            false
        }
    }

    // Android 9 及以下：使用传统文件操作 (需要写权限)
    private fun saveToLegacyStorage(context: Context, sourceFile: File): Boolean {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }
        val destFile = File(picturesDir, "IMG_${System.currentTimeMillis()}.jpg")

        return try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            // 通知系统扫描文件
            notifySystemScanner(context, destFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun notifySystemScanner(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(file)
        context.sendBroadcast(intent)
    }
}