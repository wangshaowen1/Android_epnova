package com.kuaigeglobal.epnova.feature.download.transform

import android.view.View
import com.arthenica.ffmpegkit.FFmpegKit
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo
import com.kuaigeglobal.epnova.feature.download.DownloadRatioConfig
import com.kuaigeglobal.epnova.feature.download.material.MicroAnimSegmentSourceMaterialItem
import com.kuaigeglobal.epnova.feature.download.material.MicroAnimSourceMaterial
import com.kuaigeglobal.epnova.feature.download.material.ViewSourceMaterialItem
import java.io.File

/**
 * 短剧导出视频
 */
class MicroAnimShortDramaTransform(
    private val shortDrama: ShortDramaInfo,
    private val watermark: View?,
    private val config: DownloadRatioConfig
) : BaseFFmpegTransform() {
    companion object {
        private const val fps = 30
        private val blockDurationS = 0
        val durationRegex by lazy { Regex("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)") }
    }

    private val vw = config.vw
    override val downloadProgressRatio: Float = 0.5f
    override val sourceMaterial by lazy {
        MicroAnimSourceMaterial(
            segments = shortDrama.shotSegmentList?.mapIndexedNotNull { index, segment ->
                val imageUrl = segment.shotSegmentImageUrl ?: return@mapIndexedNotNull null
                val subtitle = segment.shotSegmentSubtitleList?.firstOrNull() ?: return@mapIndexedNotNull null
                val audioUrl = subtitle.subtitleAudioUrl ?: return@mapIndexedNotNull null
                MicroAnimSegmentSourceMaterialItem(imageUrl, audioUrl, subtitle.subtitleContent, config, fps, blockDurationS, index % 2)
            },
            watermark = watermark?.let { ViewSourceMaterialItem(it) }
        )
    }

    override suspend fun commandAndDuration(outputFile: File): Pair<String, Long> {
        val segments = sourceMaterial.segments
        if (segments.isNullOrEmpty()) {
            throw TransformException("下载失败，没有素材")
        }
        val command = StringBuilder()
        val filter = StringBuilder()
        var num = 0
        var duration = 0L
        val segmentList = StringBuilder()
        segments.forEachIndexed { index, segment ->
            if (canceled) throw Exception("已取消")
            val command = "-i ${segment.file.absolutePath} -hide_banner"
            val session = FFmpegKit.execute(command)
            val output = session.allLogsAsString
            val match = durationRegex.find(output)
            if (match == null) {
                throw TransformException("获取片段时长失败")
            }
            val (h, m, s) = match.destructured
            // 图片时长 = 时 * 3600 + 分 * 60 + 秒
            val segmentDuration = ((h.toInt() * 3600 + m.toInt() * 60 + s.toFloat()) * 1000).toLong()
            segmentList.appendLine("file '${segment.file.absolutePath}'")
            duration += segmentDuration
        }
        if (canceled) throw Exception("已取消")
        val imageListFile = File(outputFile.parentFile, outputFile.name.replace(".mp4", "_file_list.txt"))
        imageListFile.writeText(segmentList.toString())

        command.append(" -f concat -safe 0 -i '${imageListFile.absolutePath}'")
        var videoName = "$num:v"
        num++
        val overlay = sourceMaterial.watermark?.file
        if (overlay?.exists() == true) {
            command.append(" -i '${overlay.absolutePath}'")
            filter.append("[$num:v]scale=$vw:-1[overlay];")
            filter.append("[$videoName][overlay]overlay=0:0[video_with_overlay];")
            num++
            videoName = "video_with_overlay"
            command.append(" -filter_complex \"${filter}\" -map \"[$videoName]\" -map 0:a")
        }
        command.append(" -c:v libopenh264 -b:v 4000k -r $fps -pix_fmt yuv420p")
        command.append(" -y '${outputFile.absolutePath}'")
        return command.toString() to duration
    }

    override val outputFileName: String
        get() = "Video_${System.nanoTime()}_${shortDrama.dramasId}.mp4"
}