package com.kuaigeglobal.epnova.feature.download.material

import android.graphics.Bitmap
import com.blankj.utilcode.util.ImageUtils
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.toBitmap
import com.kuaigeglobal.epnova.feature.download.DownloadRatioConfig
import kotlinx.coroutines.launch

class MicroAnimImageSourceMaterialItem(
    private val imageUrl: String,
    private val config: DownloadRatioConfig,
    private val translationPixels: Int = 120
) : SourceMaterialItem() {

    override val filename by lazy { "${toString()}.jpg".urlToFilename() }
    val imageHeight by lazy { config.vh + translationPixels }

    override fun prepare(listener: Listener) {
        super.prepare(listener)
        ioScope.launch {
            if (file.exists()) {
                onProgress(100)
                onSuccess()
                return@launch
            }
            try {
                val bitmap = (runCatching { imageUrl.toBitmap() }.getOrNull())
                    ?.let {
                        val targetHeight: Int
                        val targetWidth: Int
                        val whRatio = config.vw.toFloat() / (config.vh + translationPixels)
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
                        val scale = config.vw.toFloat() / it.width
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
        return "MicroAnimImageSourceMaterialItem(imageUrl='$imageUrl', config=$config, translationPixels=$translationPixels)"
    }
}