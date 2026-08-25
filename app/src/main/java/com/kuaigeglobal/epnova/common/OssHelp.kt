package com.kuaigeglobal.epnova.common

import android.graphics.Color
import android.util.LruCache
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.client
import com.kuaige.imageloader.AliyunOssImageUrlStrategy
import com.kuaige.imageloader.AverageHueUrlStrategy
import com.kuaige.imageloader.CloudImageUrlProcessor
import com.kuaige.imageloader.ImageFormat
import com.kuaige.imageloader.ImageProcessingOptions
import com.kuaige.imageloader.Resolution
import com.kuaige.imageloader.TencentCosImageUrlStrategy
import com.kuaige.imageloader.VolcengineTosImageUrlStrategy
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object OssHelp {
    private val colorCache by lazy { LruCache<String, Int>(500) }
    private val imageUrlProcessor by lazy {
        CloudImageUrlProcessor(
            listOf(
                AliyunOssImageUrlStrategy(),
                VolcengineTosImageUrlStrategy(),
                TencentCosImageUrlStrategy()
            )
        )
    }

    // suspend 方法用于获取颜色
    suspend fun fetchImageColor(imageUrl: String): Int? = runCatching {
        // 从缓存中获取颜色
        colorCache[imageUrl]?.let { return it }
        // 根据不同平台生成 URL
        val colorUrl = buildAverageHueUrl(imageUrl) ?: return null
        // 创建网络请求
        val request = Request.Builder().url(colorUrl).build()
        // 执行网络请求
        val response = client.newCall(request).execute()
        response.use {
            if (!response.isSuccessful) return null
            val str = response.body?.string() ?: return null
            // 解析 JSON
            val jsonObject = JSONObject(str)
            val rgbValue = jsonObject.getString("RGB")
            val hexValue = rgbValue.substring(2) // 移除 "0x" 前缀
            return Color.parseColor("#$hexValue").apply {  // 缓存并返回结果
                colorCache.put(imageUrl, this)
            }
        }
    }.getOrElse {
        // 捕获异常并返回 null
        Logger.e("OssImageColorFetcher", "Error fetching image color")
        null
    }

    // 主调函数，调用 suspend 方法来获取颜色
    fun fetchImageColor(imageUrl: String?, callback: (color: Int?) -> Unit) {
        if (imageUrl.isNullOrBlank() || buildAverageHueUrl(imageUrl) == null) {
            callback.invoke(null)
            return
        }

        ioScope.launch {
            val color = fetchImageColor(imageUrl)
            withContext(Dispatchers.Main) {
                callback(color)
            }
        }
    }

    fun getOssThumbnail(imageUrl: String?): String {
        return process(imageUrl, ImageFormat.JPEG, Resolution.LOW)
    }

    fun getOssHighQualityThumbnail(imageUrl: String?): String {
        return process(imageUrl, ImageFormat.JPEG, Resolution.HD)
    }

    @JvmStatic
    fun asOssWebp(imageUrl: String?): String {
        if (ImageFormat.PNG.`is`(imageUrl)) {
            return process(imageUrl, ImageFormat.WEBP)
        }
        return imageUrl ?: ""
    }

    private fun process(
        imageUrl: String?,
        format: ImageFormat,
        resolution: Resolution? = null
    ): String {
        if (imageUrl.isNullOrBlank()) return ""
        return imageUrlProcessor.process(
            imageUrl,
            ImageProcessingOptions(format = format, resolution = resolution)
        )
    }

    private fun buildAverageHueUrl(imageUrl: String): String? {
        val strategy = imageUrlProcessor.findStrategy(imageUrl) as? AverageHueUrlStrategy
        return strategy?.buildAverageHueUrl(imageUrl)
    }
}