package com.kuaigeglobal.epnova.common

import android.graphics.Color
import android.util.LruCache
import com.kuaige.imageloader.CompressedImageProcessor
import com.kuaige.imageloader.ImageFormat
import com.kuaige.imageloader.OssImageProcessor
import com.kuaige.imageloader.Resolution
import com.nhsd.basic.utils.Logger
import com.kuaigeglobal.epnova.common.ext.client
import com.kuaigeglobal.epnova.common.ext.ioScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object OssHelp {
    private val colorCache by lazy { LruCache<String, Int>(500) }

    // suspend 方法用于获取颜色
    suspend fun fetchImageColor(imageUrl: String): Int? = runCatching {
        // 从缓存中获取颜色
        colorCache[imageUrl]?.let { return it }
        // 根据不同平台生成 URL
        val colorUrl = when {
            OssImageProcessor.isAliYunOssUrl(imageUrl) -> {
                "${imageUrl}?${OssImageProcessor.OSS_PROCESS}=image/average-hue"
            }

            OssImageProcessor.isVolcanoTosUrl(imageUrl) -> {
                "${imageUrl}?${OssImageProcessor.TOS_PROCESS}=image/average-hue"
            }

            else -> return null
        }
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
        if (imageUrl == null || (!OssImageProcessor.isAliYunOssUrl(imageUrl) && !OssImageProcessor.isVolcanoTosUrl(
                imageUrl
            ))
        ) {
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
        val compressedImageProcessor = CompressedImageProcessor(ImageFormat.JPEG, Resolution.LOW)
        return compressedImageProcessor.process(imageUrl!!)
    }

    fun getOssHighQualityThumbnail(imageUrl: String?): String {
        val compressedImageProcessor = CompressedImageProcessor(ImageFormat.JPEG, Resolution.HD)
        return compressedImageProcessor.process(imageUrl!!)
    }

    @JvmStatic
    fun asOssWebp(imageUrl: String?): String {
        if (ImageFormat.PNG.`is`(imageUrl)) {
            val compressedImageProcessor = CompressedImageProcessor(ImageFormat.WEBP)
            return compressedImageProcessor.process(imageUrl!!)
        }
        return imageUrl ?: ""
    }
}