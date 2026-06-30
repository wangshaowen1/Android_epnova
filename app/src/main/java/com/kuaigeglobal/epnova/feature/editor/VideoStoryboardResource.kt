package com.kuaigeglobal.epnova.feature.editor

import android.media.MediaMetadataRetriever
import android.util.Size
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.feature.download.resource.JsonToAssResource
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
import kotlin.text.toInt

/**
 * 分镜下载
 */
class VideoStoryboardResource(private val url: String, private val storyboard: SimpleStoryboardInfo, private val watermark: Boolean, private val httpClient: OkHttpClient) : Resource() {
    init {
        // 配置字体目录，防止添加字幕时出错
        FFmpegKitConfig.setFontDirectoryList(BaseApp.getInstance(), listOf("/system/fonts"), emptyMap())
    }

    override val filename by lazy { "${url.urlToFilename()}_${storyboard.subtitle?.subtitleContent.hashCode()}_${watermark}.mp4" }
    private val cacheFile by lazy {
        val path = "${PathUtils.getExternalAppCachePath()}${File.separator}source_material${File.separator}${filename}"
        File(path).apply {
            parentFile?.let { FileUtils.createOrExistsDir(it) }
        }
    }

    override suspend fun doWork(onProgress: ((Int) -> Unit)?) {
        val videoFile = UrlResource(url, httpClient).download { onProgress?.invoke((it * 0.4f).toInt()) }
        val subtitleFile = runCatching {
            val videoSize = getVideoSize(videoFile.absolutePath)
            val videoWidth = videoSize?.width?.takeIf { it > 0 } ?: 1280
            val videoHeight = videoSize?.height?.takeIf { it > 0 } ?: 720
            var fontSize = videoWidth / 375f * 20f
            var marginBottom = videoHeight * 0.08f
            if (videoHeight > videoWidth) {
                fontSize = videoWidth / 375f * 30f
                marginBottom = videoHeight * 0.26f
            }
            JsonToAssResource(
                storyboard.storyboardId ?: "",
                storyboard.subtitle?.subtitleContent ?: "",
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                fontSize = fontSize.toInt(),
                marginBottom = marginBottom.toInt(),
            ).download { onProgress?.invoke((40 + it * 0.1f).toInt()) }
        }.getOrNull()
        val builder = FFmpegCommandBuilder()
        var mapV = ""
        var finalV = ""
        run {
            val idx = builder.addInput(videoFile.absolutePath)
            // 服务端返回的视频可能包含多条 video track，分镜合成只消费主视频轨。
            mapV = "$idx:v:0"
            finalV = mapV
        }
        if (subtitleFile?.exists() == true) {
            builder.addFilter("[$mapV]subtitles='${subtitleFile.absolutePath}':force_style='Outline=1,Shadow=0,PrimaryColour=&H00FFFFFF,SecondaryColour=&H80000000'[v_subtitle]")
            mapV = "v_subtitle"
            finalV = "[$mapV]"
        }
        if (watermark){
            val watermarkText = R.string.video_download_watermark.getResString()
            val videoSize = getVideoSize(videoFile.absolutePath)
            val videoWidth = videoSize?.width?.takeIf { it > 0 } ?: 1280
            val margin = maxOf((videoWidth * 0.036f).toInt(), 12)
            val fontSize = maxOf((videoWidth * 0.032f).toInt(), 18)
            val borderWidth = maxOf((videoWidth * 0.0016f).toInt(), 1)
            val fontOption = getWatermarkFontFile()?.let {
                "fontfile='${escapeDrawTextValue(it.absolutePath)}':"
            }.orEmpty()
            builder.addFilter(
                "[$mapV]drawtext=${fontOption}text='${escapeDrawTextValue(watermarkText)}':" +
                    "fontcolor=white:fontsize=$fontSize:x=w-tw-$margin:y=$margin:" +
                    "borderw=$borderWidth:bordercolor=black@0.55[v_watermark]"
            )
            mapV = "v_watermark"
            finalV = "[$mapV]"
        }
        val extra = "-map $finalV -map 0:a? -c:v libx264 -preset veryfast -r 30 -pix_fmt yuv420p"
        val command = builder.build(cacheFile.absolutePath, extra)
        val videoDuration = FFmpegHelper.parseDuration(videoFile.absolutePath) / 1000
        FFmpegHelper.execute(command, videoDuration) { ffmpegProgress ->
            onProgress?.invoke((50 + ffmpegProgress * 0.5f).toInt())
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

    fun getVideoSize(path: String): Size? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            // 获取原始宽高
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0

            // 关键点：处理旋转角度
            // 手机拍摄的视频可能是 1080x1920，但 Metadata 记为 1920x1080 + 旋转 90 度
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0

            return if (rotation == 90 || rotation == 270) {
                Size(height, width) // 交换宽高
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
