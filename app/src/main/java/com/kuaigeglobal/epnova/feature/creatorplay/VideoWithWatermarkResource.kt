package com.kuaigeglobal.epnova.feature.creatorplay

import android.media.MediaMetadataRetriever
import android.util.Size
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaige.videoedit.ffmpeg.FFmpegCommandBuilder
import com.kuaige.videoedit.ffmpeg.FFmpegHelper
import com.kuaige.videoedit.resource.Resource
import com.kuaige.videoedit.resource.UrlResource
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.BaseApp
import com.nhsd.basic.ext.getResString
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException

/**
 * Creator 播放器下载带水印版本的资源。
 *
 * 逻辑按 FastFilm 播放器处理：先下载当前视频，再把 AI 文案水印叠到右上角后输出到下载流程。
 */
class VideoWithWatermarkResource(private val url: String, private val httpClient: OkHttpClient) : Resource() {

    init {
        FFmpegKitConfig.setFontDirectoryList(BaseApp.getInstance(), listOf("/system/fonts"), emptyMap())
    }

    override val filename by lazy { "${url.urlToFilename()}_watermark.mp4" }

    private val cacheFile by lazy {
        val path = "${PathUtils.getExternalAppCachePath()}${File.separator}source_material${File.separator}${filename}"
        File(path).apply {
            parentFile?.let { FileUtils.createOrExistsDir(it) }
        }
    }

    override suspend fun doWork(onProgress: ((Int) -> Unit)?) {
        val watermark_text = R.string.video_download_watermark.getResString()
        val videoFile = UrlResource(url, httpClient).download { onProgress?.invoke(it / 2) }
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
            val margin = maxOf((videoWidth * 0.036f).toInt(), 12)
            val fontSize = maxOf((videoWidth * 0.032f).toInt(), 18)
            val borderWidth = maxOf((videoWidth * 0.0016f).toInt(), 1)
            val fontOption = getWatermarkFontFile()?.let {
                "fontfile='${escapeDrawTextValue(it.absolutePath)}':"
            }.orEmpty()
            builder.addFilter(
                "[$mapV]drawtext=${fontOption}text='${escapeDrawTextValue(watermark_text)}':" +
                    "fontcolor=white:fontsize=$fontSize:x=w-tw-$margin:y=$margin:" +
                    "borderw=$borderWidth:bordercolor=black@0.55[v_watermark]"
            )
            mapV = "v_watermark"
            finalV = "[$mapV]"
        }
        val extra = "-map $finalV -map 0:a? -c:v libopenh264 -b:v 4000k -r 30 -pix_fmt yuv420p"
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
    }

    private fun getWatermarkFontFile(): File? {
        return WATERMARK_FONT_PATHS
            .map { File(it) }
            .firstOrNull { it.exists() }
    }

    private fun escapeDrawTextValue(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(":", "\\:")
            .replace("'", "\\'")
            .replace(",", "\\,")
            .replace("%", "\\%")
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

    companion object {
        private val WATERMARK_FONT_PATHS = listOf(
            "/system/fonts/NotoSansCJK-Regular.ttc",
            "/system/fonts/NotoSansCJK-Regular.otf",
            "/system/fonts/DroidSansFallback.ttf",
            "/system/fonts/Roboto-Regular.ttf"
        )
    }
}
