@file:JvmName("ContextUtils")

package com.kuaigeglobal.epnova.common.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.common.utils.Utils
import com.kuaigeglobal.epnova.service.DownloadService
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.toArrayList
import com.nhsd.basic.manager.FileManager
import com.nhsd.mattise.Matisse
import com.nhsd.mattise.MimeType
import com.nhsd.mattise.engine.impl.GlideEngine
import com.nhsd.mattise.internal.entity.CaptureStrategy

// Fragment参数扩展
fun Fragment.putArgument(key: String, value: String?) {
    if (arguments == null) arguments = Bundle()
    arguments!!.putString(key, value)
}

fun Fragment.putArgument(key: String, value: Int) {
    if (arguments == null) arguments = Bundle()
    arguments!!.putInt(key, value)
}

fun Fragment.putArgument(key: String, value: Boolean) {
    if (arguments == null) arguments = Bundle()
    arguments!!.putBoolean(key, value)
}

fun Fragment.putArgument(key: String, value: Parcelable) {
    if (arguments == null) arguments = Bundle()
    arguments!!.putParcelable(key, value)
}

fun Fragment.putStringArrayList(key: String, value: List<String>) {
    if (arguments == null) arguments = Bundle()
    arguments!!.putStringArrayList(key, value.toArrayList())
}

fun Fragment.putArrayList(key: String, value: List<Parcelable>) {
    if (arguments == null) arguments = Bundle()
    arguments!!.putParcelableArrayList(key, value.toArrayList())
}

// Activity扩展
fun Context.openBrowser(url: String?) {
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(url)
    }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(Intent.createChooser(intent, getString(R.string.select_browser)))
    }
}

fun FragmentActivity.downloadFile(url: String?) {
    requestRead2WritePermissions(R.string.sdcard_permission_description) { allGranted ->
        if (!allGranted) return@requestRead2WritePermissions
        val fileName = Utils.parseName(url)
        Intent(this, DownloadService::class.java).apply {
            putExtra(DownloadService.DOWNLOAD_FILE_NAME, fileName)
            putExtra(DownloadService.DOWNLOADURL, url)
            startService(this)
        }
        getString(R.string.downloading_please_wait).showToast()
    }
}

fun List<Fragment>.clear(host: Fragment) {
    forEach { host.childFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss() }
    (this as? ArrayList)?.clear()
}

fun List<Fragment>.clear(host: FragmentActivity) {
    forEach { host.supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss() }
    (this as? ArrayList)?.clear()
}


fun Activity.selectMultPhotoFromAlbums(
    selectedUris: List<Uri?>?,
    requestCode: Int,
    countable: Boolean = false,
    capture: Boolean = true
) {
    Matisse.from(this)
        .choose(MimeType.ofImage(), false)
        .countable(countable)
        .capture(capture)
        .spanCount(4)
        .captureStrategy(
            CaptureStrategy(true, FileManager.getInstance().fileProvider, "album")
        )
        .maxSelectable(2) //                .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
        .setSelectedUris(selectedUris)

        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        .thumbnailScale(0.85f)
        .imageEngine(GlideEngine())
        .showSingleMediaType(true)
        .originalEnable(Constants.ORIGINAL_ENABLE)
        .autoHideToolbarOnSingleTap(true)
        .setOnCheckedListener { isChecked: Boolean ->
            Log.e(
                "isChecked",
                "onCheck: isChecked=$isChecked"
            )
        }
        .forResult(requestCode)
}

fun Activity.selectSinglePhotoFromAlbums(
    selectedUri: Uri?,
    requestCode: Int,
    capture: Boolean = true,
    useEmbeddedCamera: Boolean = false
) {
    selectSinglePhotoFromAlbums(
        activity = this,
        selectedUri = selectedUri,
        requestCode = requestCode,
        countable = false,
        capture = capture,
        useEmbeddedCamera = useEmbeddedCamera
    )
}

fun selectSinglePhotoFromAlbums(
    activity: Activity? = null,
    fragment: Fragment? = null,
    selectedUri: Uri?,
    requestCode: Int,
    countable: Boolean = false,
    capture: Boolean = true,
    useEmbeddedCamera: Boolean = false,
    spanCount: Int = 4,
    crop: Boolean = false,
    cropWHRatio: Pair<Float, Float> = 1f to 1f,
    cropWHMax: Pair<Int, Int>? = null,//裁剪图片的宽高限制
    limitSize: Boolean = false,//打开资源文件大小限制
    limitSizeValue: Int = 10,//单位MB
) {
    val matisse = when {
        activity != null -> Matisse.from(activity)
        fragment != null -> Matisse.from(fragment)
        else -> null
    } ?: return
    matisse
        .choose(MimeType.ofImage(), false)
        .countable(countable)
        .capture(capture)
        .useEmbeddedCamera(useEmbeddedCamera)
        .spanCount(spanCount)
        .captureStrategy(CaptureStrategy(true, FileManager.getInstance().fileProvider, "album"))
        .maxSelectable(1)
        .originalEnable(limitSize)
        .maxOriginalSize(limitSizeValue)
        .setSelectedUris(listOf(selectedUri))
        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        .thumbnailScale(0.85f)
        .imageEngine(GlideEngine())
        .showSingleMediaType(true)
        .originalEnable(Constants.ORIGINAL_ENABLE)
        .autoHideToolbarOnSingleTap(true)
        .also {
            if (crop) {
                it.crop(cropWHRatio)
                if (cropWHMax != null) {
                    it.cropMax(cropWHMax)
                }
            }
        }
        .forResult(requestCode)
}
//选择指定数量的图片
fun Activity.selectMultPhotoFromAlbums(
    requestCode: Int,
    maxSelectable: Int = 4
) {
    Matisse.from(this)
        .choose(MimeType.ofImage(), false)
        .countable(false)
        .capture(true)
        .useEmbeddedCamera(false)
        .spanCount(4)
        .captureStrategy(CaptureStrategy(true, FileManager.getInstance().fileProvider, "album"))
        .maxSelectable(maxSelectable)
        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        .thumbnailScale(0.85f)
        .imageEngine(GlideEngine())
        .showSingleMediaType(true)
        .originalEnable(Constants.ORIGINAL_ENABLE)
        .autoHideToolbarOnSingleTap(true)
        .also { if (false) it.crop(1f to 1f) }
        .forResult(requestCode)
}

val topActivity: Activity? get() = ActivityUtils.getTopActivity()
fun Activity.finishSameActivityExceptSelf() {
    ActivityUtils.getActivityList().forEach { activity ->
        if (activity.javaClass == javaClass && activity != this) {
            activity.finish()
        }
    }
}


fun getScreenWidth(): Int {
    return ScreenUtils.getScreenWidth()
}

fun getScreenHeight(): Int {
    return ScreenUtils.getScreenHeight()
}


