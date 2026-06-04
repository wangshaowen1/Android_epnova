@file:JvmName("PermissionsExt")
package com.kuaigeglobal.epnova.common.permissions

import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch

/**
 * 权限请求扩展函数集合
 * 统一管理 Activity 和 Fragment 的权限请求逻辑，消除重复代码
 */

// ============================================================================================
// 1. 基础权限请求 (Basic Request)
// ============================================================================================

fun FragmentActivity.requestRxPermissions(
    delayText: String,
    vararg permissions: String,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    handleRequest(this, this.lifecycleScope, delayText, *permissions, callback = callback)
}

fun FragmentActivity.requestRxPermissions(
    resId: Int,
    vararg permissions: String,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val tip = getString(resId)
    requestRxPermissions(tip, *permissions, callback = callback)
}

fun Fragment.requestRxPermissions(
    delayText: String,
    vararg permissions: String,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    handleRequest(this, this.lifecycleScope, delayText, *permissions, callback = callback)
}

fun Fragment.requestRxPermissions(
    resId: Int,
    vararg permissions: String,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val tip = getString(resId)
    requestRxPermissions(tip, *permissions, callback = callback)
}

// ============================================================================================
// 2. 读写权限 (Read/Write Storage)
// ============================================================================================

fun FragmentActivity.requestRead2WritePermissions(
    tipResId: Int = R.string.sdcard_permission_description,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    requestRead2WritePermissions(getString(tipResId), callback)
}

fun FragmentActivity.requestRead2WritePermissions(
    tip: String,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val permissions = PermissionsUtils.getAndroid12WritePermission()
    requestRxPermissions(tip, *permissions, callback = callback)
}

fun Fragment.requestRead2WritePermissions(
    tipResId: Int = R.string.sdcard_permission_description,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    requestRead2WritePermissions(getString(tipResId), callback)
}

fun Fragment.requestRead2WritePermissions(
    tip: String,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val permissions = PermissionsUtils.getAndroid12WritePermission()
    requestRxPermissions(tip, *permissions, callback = callback)
}

// ============================================================================================
// 3. 图片/视频权限 (Images & Videos) - Android 14 适配
// ============================================================================================

fun FragmentActivity.requestImages2VideosPermissions(
    tip: String = getString(R.string.sdcard_permission_description),
    tipResId: Int? = null, // 兼容传 ResId 的情况
    withVideo: Boolean = false,
    openSelect: Boolean = false,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val finalTip = tipResId?.let { getString(it) } ?: tip
    handleImagesVideoRequest(this, this, finalTip, withVideo, openSelect, callback)
}

fun Fragment.requestImages2VideosPermissions(
    tip: String = getString(R.string.sdcard_permission_description),
    tipResId: Int? = null,
    withVideo: Boolean = false,
    openSelect: Boolean = false,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val finalTip = tipResId?.let { getString(it) } ?: tip
    handleImagesVideoRequest(this, requireContext(), finalTip, withVideo, openSelect, callback)
}

// ============================================================================================
// 4. 相机 + 媒体权限 (Camera & Media) - Android 14 适配
// ============================================================================================

fun FragmentActivity.requestCamera2MediaPermissions(
    tip: String = getString(R.string.sdcard_permission_description),
    tipResId: Int? = null,
    withVideo: Boolean = false,
    openSelect: Boolean = false,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val finalTip = tipResId?.let { getString(it) } ?: tip
    handleCameraMediaRequest(this, this, finalTip, withVideo, openSelect, callback)
}

fun Fragment.requestCamera2MediaPermissions(
    tip: String = getString(R.string.sdcard_permission_description),
    tipResId: Int? = null,
    withVideo: Boolean = false,
    openSelect: Boolean = false,
    callback: ((granted: Boolean) -> Unit)? = null
) {
    val finalTip = tipResId?.let { getString(it) } ?: tip
    handleCameraMediaRequest(this, requireContext(), finalTip, withVideo, openSelect, callback)
}

// ============================================================================================
// 私有处理逻辑 (Private Implementation)
// ============================================================================================

/**
 * 基础请求处理
 */
private fun handleRequest(
    owner: Any,
    scope: kotlinx.coroutines.CoroutineScope,
    delayText: String,
    vararg permissions: String,
    callback: ((granted: Boolean) -> Unit)?
) {
    if (permissions.isEmpty()) {
        callback?.invoke(true)
        return
    }

    scope.launch {
        val handler = when (owner) {
            is FragmentActivity -> Permissions.of(owner)
            is Fragment -> Permissions.of(owner)
            else -> null
        }

        val granted = handler?.request(delayText, *permissions) ?: false
        callback?.invoke(granted)
        if (!granted) {
            showToast(R.string.permission_application_failed)
        }
    }
}

/**
 * 通用的权限请求调用器（内部使用）
 */
private fun executeRequest(
    owner: Any,
    tip: String,
    permissions: Array<String>,
    callback: ((granted: Boolean) -> Unit)?
) {
    if (owner is FragmentActivity) {
        owner.requestRxPermissions(tip, *permissions, callback = callback)
    } else if (owner is Fragment) {
        owner.requestRxPermissions(tip, *permissions, callback = callback)
    }
}

/**
 * 处理图片视频权限逻辑
 */
private fun handleImagesVideoRequest(
    owner: Any,
    context: Context,
    tip: String,
    withVideo: Boolean,
    openSelect: Boolean,
    callback: ((granted: Boolean) -> Unit)?
) {
    val permissions = if (withVideo) PermissionsUtils.getImagesAndVideosPermissions() else PermissionsUtils.getImagesPermissions()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
        if (PermissionsUtils.checkUserSelectedPermission(context)) {
            // 已有部分权限
            if (openSelect) {
                // 直接申请部分权限会打开图片视频选择
                executeRequest(owner, tip, permissions) {
                    callback?.invoke(true)
                }
            } else {
                // 已经有权限了可以直接返回成功
                callback?.invoke(true)
            }
        } else {
            // 没有部分图片视频权限的时候直接走申请
            executeRequest(owner, tip, permissions) {
                callback?.invoke(PermissionsUtils.checkUserSelectedPermission(context))
            }
        }
    } else {
        // Android 14以下直接走申请
        executeRequest(owner, tip, permissions, callback)
    }
}

/**
 * 处理相机+媒体权限逻辑
 */
private fun handleCameraMediaRequest(
    owner: Any,
    context: Context,
    tip: String,
    withVideo: Boolean,
    openSelect: Boolean,
    callback: ((granted: Boolean) -> Unit)?
) {
    val permissions = if (withVideo) PermissionsUtils.getCamera2MediaPermissions() else PermissionsUtils.getCamera2ImagesPermissions()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
        // 检查当前是否满足条件（媒体权限包含“部分权限”的检查）
        val hasPermission = if (withVideo) {
            PermissionsUtils.checkMediaAndCameraPermission(context)
        } else {
            PermissionsUtils.checkImagesAndCameraPermission(context)
        }

        if (hasPermission) {
            if (openSelect) {
                executeRequest(owner, tip, permissions) {
                    callback?.invoke(true)
                }
            } else {
                callback?.invoke(true)
            }
        } else {
            executeRequest(owner, tip, permissions) {
                // 申请后再次检查，如果拿到了部分权限也算成功
                val granted = if (withVideo) {
                    PermissionsUtils.checkMediaAndCameraPermission(context)
                } else {
                    PermissionsUtils.checkImagesAndCameraPermission(context)
                }
                callback?.invoke(granted)
            }
        }
    } else {
        // Android 14以下直接走申请
        executeRequest(owner, tip, permissions, callback)
    }
}
