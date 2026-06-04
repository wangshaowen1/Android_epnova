package com.kuaigeglobal.epnova.common.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * APP运行时权限相关
 */
object PermissionsUtils {
    /**
     * 检查权限是否授予
     */
    @JvmStatic
    fun checkPermissions(context: Context, vararg permissions: String): Boolean {
        var granted = false
        for (item in permissions) {
            granted = checkPermission(context, item)
            if (!granted) break
        }
        return granted
    }

    /**
     * 检查权限是否授予
     */
    @JvmStatic
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查部分图片、视频权限是否授予
     */
    @JvmStatic
    fun checkUserSelectedPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

    /**
     * 检查拍照，图片、视频权限是否授予
     */
    @JvmStatic
    fun checkMediaAndCameraPermission(context: Context): Boolean {
        return checkMediaPermission(context) && checkPermission(context, Manifest.permission.CAMERA)
    }

    /**
     * 检查拍照，图片权限是否授予
     */
    @JvmStatic
    fun checkImagesAndCameraPermission(context: Context): Boolean {
        return checkImagesPermission(context) && checkPermission(context, Manifest.permission.CAMERA)
    }

    /**
     * 检查媒体权限是否授予。
     * 当前 Manifest 移除了 READ_MEDIA_VIDEO，运行时保持只申请图片/部分照片权限。
     */
    @JvmStatic
    fun checkMediaPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            checkPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                    checkPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            checkPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * 检查图片权限是否授予
     */
    @JvmStatic
    fun checkImagesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            checkPermission(context, Manifest.permission.READ_MEDIA_IMAGES) || checkPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            checkPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * 获取拍照和媒体相关权限。
     * 当前 Manifest 移除了 READ_MEDIA_VIDEO，运行时保持只申请图片/部分照片权限。
     */
    @JvmStatic
    fun getCamera2MediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }


    /**
     * 获取拍照，图片的相关权限
     */
    @JvmStatic
    fun getCamera2ImagesPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * 获取媒体相关权限。
     * 当前 Manifest 移除了 READ_MEDIA_VIDEO，运行时保持只申请图片/部分照片权限。
     */
    @JvmStatic
    fun getImagesAndVideosPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * 获取图片相关权限
     */
    @JvmStatic
    fun getImagesPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * 获取拍照相关权限
     */
    @JvmStatic
    fun getCameraPermission(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * 获取录音相关权限
     */
    @JvmStatic
    fun getRecordAudioPermission(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * 获取android12以下的文件读写权限
     */
    @JvmStatic
    fun getAndroid12WritePermission(): Array<String> {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else emptyArray()
    }


}
