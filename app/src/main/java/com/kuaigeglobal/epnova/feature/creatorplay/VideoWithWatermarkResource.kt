package com.kuaigeglobal.epnova.feature.creatorplay

import android.media.MediaMetadataRetriever
import android.util.Size
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaige.videoedit.ffmpeg.FFmpegCommandBuilder
import com.kuaige.videoedit.ffmpeg.FFmpegHelper
import com.kuaige.videoedit.resource.Resource
import com.kuaige.videoedit.resource.UrlResource
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.toCache
import com.nhsd.basic.ext.getBitmap
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException

/**
 * Creator 播放器下载带水印版本的资源。
 *
 * 逻辑按 FastFilm 播放器处理：先下载当前视频，再把 AI 水印叠到右上角后输出到下载流程。
 */
class VideoWithWatermarkResource(private val url: String, private val httpClient: OkHttpClient) : Resource() {

    override val filename by lazy { "${url.urlToFilename()}_watermark.mp4" }

    private val cacheFile by lazy {
        val path = "${PathUtils.getExternalAppCachePath()}${File.separator}source_material${File.separator}${filename}"
        File(path).apply {
            parentFile?.let { FileUtils.createOrExistsDir(it) }
        }
    }

    override suspend fun doWork(onProgress: ((Int) -> Unit)?) {
        val videoFile = UrlResource(url, httpClient).download { onProgress?.invoke(it / 2) }
        val watermarkFile = R.drawable.icon_ai_watermark.getBitmap().toCache()
        if (watermarkFile?.exists() == true) {
            val builder = FFmpegCommandBuilder()
            var mapV = ""
            var finalV = ""
            run {
                val idx = builder.addInput(videoFile.absolutePath)
                mapV = "$idx:v"
                finalV = mapV
            }
            run {
                val videoSize = getVideoSize(videoFile.absolutePath)
                val videoWidth = videoSize?.width?.takeIf { it > 0 } ?: 1280
                val overlayX = (videoWidth * 0.864f).toInt()
                val overlayY = (videoWidth * 0.036f).toInt()
                val idx = builder.addInput(watermarkFile.absolutePath)
                builder.addFilter("[$idx:v]scale=${videoWidth / 10}:-1[content]")
                builder.addFilter("[$mapV][content]overlay=$overlayX:$overlayY[v_watermark]")
                mapV = "v_watermark"
                finalV = "[$mapV]"
            }
            val extra = "-map $finalV -map 0:a? -c:v libx264 -preset veryfast -r 30 -pix_fmt yuv420p"
            val command = builder.build(cacheFile.absolutePath, extra)
            val videoDuration = FFmpegHelper.parseDuration(videoFile.absolutePath) / 1000
            FFmpegHelper.execute(command, videoDuration) { ffmpegProgress ->
                onProgress?.invoke(50 + ffmpegProgress / 2)
            }
            if (FileUtils.copy(cacheFile, file)) {
                onProgress?.invoke(100)
            } else {
                throw IOException("Copy cache file to target failed")
            }
        } else {
            if (FileUtils.copy(videoFile, file)) {
                onProgress?.invoke(100)
            } else {
                throw IOException("Copy cache file to target failed")
            }
        }
    }

    private fun getVideoSize(path: String): Size? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0

            return if (rotation == 90 || rotation == 270) {
                Size(height, width)
            } else {
                Size(width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            retriever.release()
        }
    }
}
