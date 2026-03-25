package com.kuaigeglobal.epnova.feature.download.material

import android.graphics.Bitmap
import com.blankj.utilcode.util.ImageUtils
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.toBitmap
import kotlinx.coroutines.launch

class ImageSourceMaterialItem(
    private val url: String,
    private val outputWidth: Int,
    private val outputHeight: Int
) : SourceMaterialItem() {

    override val filename by lazy { "${toString()}.jpg".urlToFilename() }

    override fun prepare(listener: Listener) {
        super.prepare(listener)
        ioScope.launch {
            if (file.exists()) {
                onProgress(100)
                onSuccess()
                return@launch
            }
            try {
                val bitmap = (runCatching { url.toBitmap() }.getOrNull())
                    ?.let {
                        val targetHeight: Int
                        val targetWidth: Int
                        val whRatio = outputWidth.toFloat() / outputHeight
                        if (it.width.toFloat() / it.height > whRatio) {
                            targetHeight = it.height
                            targetWidth = (targetHeight * whRatio).toInt()
                        } else {
                            targetWidth = it.width
                            targetHeight = (targetWidth / whRatio).toInt()
                        }
                        //glide加载的图片，这里不能回收，否则显示图片有问题
                        ImageUtils.clip(it, (it.width - targetWidth) / 2, (it.height - targetHeight) / 2, targetWidth, targetHeight)
                    }
                    ?.let {
                        val scale = outputWidth.toFloat() / it.width
                        ImageUtils.scale(it, scale, scale, true)
                    }
                val res = ImageUtils.save(bitmap, file, Bitmap.CompressFormat.JPEG, true)
                if (res) {
                    onProgress(100)
                    onSuccess()
                } else {
                    onFail(kotlin.Exception("图片下载失败 ${file.name}"))
                }
            } catch (e: Throwable) {
                onFail(kotlin.Exception("图片下载失败 ${file.name}", e))
            }
        }
    }

    override fun cancel() {}
    override fun toString(): String {
        return "ImageSourceMaterialItem(url='$url', outputWidth=$outputWidth, outputHeight=$outputHeight)"
    }
}