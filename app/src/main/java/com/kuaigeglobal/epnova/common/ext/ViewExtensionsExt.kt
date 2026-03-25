package com.kuaigeglobal.epnova.common.ext

import android.graphics.Bitmap
import android.view.View
import com.nhsd.basic.manager.FileManager
import com.kuaigeglobal.epnova.common.utils.ImageUtils
import java.io.File

/**
 * View扩展
 * @author xiaoowuu
 */

/**
 * View转图片
 * @param scale 缩放比例 默认不缩放
 */
fun View.toBitmap(scale: Float = 1f, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
    val bitmap = ImageUtils.view2Bitmap(this, config)
    if (scale.compareTo(1f) == 0) {
        return bitmap
    }
    return ImageUtils.scale(bitmap, scale, scale, true)
}

fun Bitmap.CompressFormat.extension() = when (this) {
    Bitmap.CompressFormat.PNG -> "png"
    Bitmap.CompressFormat.JPEG -> "jpg"
    else -> "webp"
}

/**
 * View转图片，保存到package/cache/cache-images目录
 * @param scale 缩放比例，默认不缩放
 * @param format 压缩格式，默认png
 * @param filename 文件名，默认时间戳
 */
fun View.toCache(
    scale: Float = 1f,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    filename: String? = null
): File? {
    return toBitmap(scale, config).toCache(format, filename)
}

/**
 * Bitmap保存到package/cache/cache-images目录
 * @param format 压缩格式，默认png
 * @param filename 文件名，默认时间戳
 */
fun Bitmap.toCache(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, filename: String? = null, recycle: Boolean = true): File? {
    val file = File(FileManager.getInstance().imageFolder, "${filename ?: System.nanoTime()}.${format.extension()}")
    return if (ImageUtils.save(this, file, format, 100, recycle)) file else null
}

/**
 * View转图片，保存到相册
 * @param scale 缩放比例，默认不缩放
 * @param format 压缩格式，默认png
 * @param filepath 保存目录
 * @param filename 文件名，默认时间戳
 */
fun View.toAlbum(
    scale: Float = 1f,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    filepath: String? = null,
    filename: String? = null
): File? {
    return toBitmap(scale, config).toAlbum(format, filepath, filename)
}

/**
 * Bitmap保存到相册
 * @param format 压缩格式，默认png
 * @param filepath 保存目录
 * @param filename 文件名，默认时间戳
 */
fun Bitmap.toAlbum(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, filepath: String? = null, filename: String? = null): File? {
    return ImageUtils.save2Album(this, filepath, "${filename ?: System.nanoTime()}.${format.extension()}", format, 100, true)
}
