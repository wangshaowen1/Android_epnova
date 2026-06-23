package com.kuaigeglobal.epnova.feature.download.transform

import android.text.format.DateUtils
import android.util.SparseLongArray
import android.view.View
import com.arthenica.ffmpegkit.FFmpegKit
import com.blankj.utilcode.util.ResourceUtils
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo
import com.kuaigeglobal.epnova.feature.download.DownloadRatioConfig
import com.kuaigeglobal.epnova.feature.download.material.ImageSourceMaterialItem
import com.kuaigeglobal.epnova.feature.download.material.SourceMaterial
import com.kuaigeglobal.epnova.feature.download.material.UrlSourceMaterialItem
import com.kuaigeglobal.epnova.feature.download.material.ViewSourceMaterialItem
import java.io.File
import java.util.Locale

/**
 * 短剧导出视频
 */
class ShortDramaTransform(
    private val shortDrama: ShortDramaInfo,
    private val overlay: View?,
    private val ratio: DownloadRatioConfig
) : BaseFFmpegTransform() {
    companion object {
        private const val fps = 25
        private val blockDurationS = 1
    }

    private val vw = ratio.vw
    private val vh = ratio.vh
    private val subtitleLineMaxLength = ratio.subtitleLineMaxLength

    override val sourceMaterial by lazy {
        SourceMaterial(
            images = shortDrama.shotSegmentList?.mapNotNull { it.shotSegmentImageUrl?.let { url -> ImageSourceMaterialItem(url, vw, vh) } },
            audios = shortDrama.shotSegmentList?.mapNotNull { it.shotSegmentSubtitleList?.firstOrNull()?.subtitleAudioUrl?.let { url -> UrlSourceMaterialItem(url) } },
            overlay = overlay?.let { ViewSourceMaterialItem(it) }
        )
    }

    override suspend fun commandAndDuration(outputFile: File): Pair<String, Long> {
        val images = sourceMaterial.images
        if (images.isNullOrEmpty()) {
            throw TransformException("下载失败，没有图片")
        }
        val audios = sourceMaterial.audios
        if (audios.isNullOrEmpty()) {
            throw TransformException("下载失败，没有音频")
        }
        if (images.size != audios.size) {
            throw TransformException("下载失败，图片和音频数量不一致")
        }
        val command = StringBuilder()
        val filter = StringBuilder()
        var num = 0
        var duration = 0L
        val imageList = StringBuilder()
        val subtitle = runCatching {
            val template = ResourceUtils.readAssets2String("lyrics_template.ass")
                .replace("#PlayResX#", vw.toString())
                .replace("#PlayResY#", vh.toString())
                .replace("#Fontsize#", ratio.subtitleTextSize.toString())
                .replace("#MarginV#", ratio.subtitleMarginBottom.toString())
            StringBuilder().appendLine(template)
        }.getOrNull()
        val durationRegex = Regex("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)")
        val imageDurations = SparseLongArray()
        images.forEachIndexed { index, video ->
            val audio = audios[index]
            val command = "-i ${audio.file.absolutePath} -hide_banner"
            val session = FFmpegKit.execute(command)
            val output = session.allLogsAsString
            val match = durationRegex.find(output)
            if (match == null) {
                throw Exception("获取音频时长失败")
            }
            val (h, m, s) = match.destructured
            // 图片时长 = 时 * 3600 + 分 * 60 + 秒 + 空白 * 2
            val imageDuration = ((h.toInt() * 3600 + m.toInt() * 60 + s.toFloat() + blockDurationS * 2) * 1000).toLong()
            imageDurations.put(index, imageDuration)
            imageList.appendLine("file '${video.file.absolutePath}'")
            imageList.appendLine("duration ${imageDuration / 1000f}")
            val line = shortDrama.shotSegmentList?.getOrNull(index)?.shotSegmentSubtitleList?.firstOrNull()?.subtitleContent?.let {
                if (it.length > subtitleLineMaxLength) it.windowed(subtitleLineMaxLength, subtitleLineMaxLength, true).joinToString("\\N") else it
            }
            subtitle?.appendLine("Dialogue: 0,${duration.assTime()},${(duration + imageDuration).assTime()},Karaoke,${line}")
            duration += imageDuration
        }
        val imageListFile = File(outputFile.parentFile, outputFile.name.replace(".mp4", "_file_list.txt"))
        imageListFile.writeText(imageList.toString())

        val subtitleFile = subtitle?.let {
            val file = File(outputFile.parentFile, outputFile.name.replace(".mp4", "_subtitle.ass"))
            file.writeText(it.toString())
            file
        }

        command.append(" -f concat -safe 0 -i '${imageListFile.absolutePath}'")
        //fps=fps=$fps 提高帧率
        filter.append("[$num:v]scale=$vw:$vh:force_original_aspect_ratio=increase,crop=$vw:$vh,setsar=1,fps=fps=$fps[video];")
        num++
        var videoName = "video"
        val overlay = sourceMaterial.overlay?.file
        if (overlay?.exists() == true) {
            command.append(" -i '${overlay.absolutePath}'")
            filter.append("[$num:v]scale=$vw:-1[overlay];")
            filter.append("[$videoName][overlay]overlay=0:0[video_with_overlay];")
            num++
            videoName = "video_with_overlay"
        }
        if (subtitleFile?.exists() == true) {
            filter.append("[$videoName]subtitles='${subtitleFile.absolutePath}'[video_with_subtitle];")
            videoName = "video_with_subtitle"
        }
        filter.append("[$videoName]trim=start=0:end=${duration / 1000f}[video_with_duration];")
        videoName = "video_with_duration"
        val audioList = arrayListOf<String>()
        var adelay = blockDurationS * 1000L
        audios.forEachIndexed { index, audio ->
            command.append(" -i '${audio.file.absolutePath}'")
            filter.append("[$num:a]adelay=$adelay|$adelay[a$num];")
            audioList.add("[a$num]")
            adelay += imageDurations[index]
            num++
        }
        filter.append("${audioList.joinToString("")}amix=inputs=${audioList.size}:normalize=0,aresample=async=1:first_pts=0[audio];")
        var audioName = "audio"
//        filter.append("[$audioName]atrim=start=0:end=${duration}[audio_with_duration];")
//        audioName = "audio_with_duration"
        var outputDuration = duration
//        val trailerAudio = sourceMaterial.trailerAudio?.file
//        if (trailerAudio?.exists() == true) {
//            command.append(" -i '${trailerAudio.absolutePath}'")
//            filter.append("[$audioName][${num}:a]concat=n=2:v=0:a=1[audio_with_trailer];")
//            num++
//            audioName = "audio_with_trailer"
//            outputDuration += 3
//        }
        command.append(" -filter_complex \"${filter}\" -map \"[$videoName]\" -map \"[$audioName]\"")
        //归途如虹 105258ms 17.01M -c:v libx264 -preset veryfast -b:v 1500k -r $fps -c:a aac -b:a 192k -pix_fmt yuv420p
        //归途如虹 148958ms 14.38M -c:v libx264 -b:v 1500k -r $fps -pix_fmt yuv420p -c:a aac -b:a 192k
        //归途如虹 156366ms 12.42M -c:v libx264 -b:v 1500k -r $fps -pix_fmt yuv420p
        //归途如虹 143556ms 6.85M  -c:v libx264 -r $fps -pix_fmt yuv420p
        //归途如虹 107927ms 6.13M  -c:v libx264 -r $fps -pix_fmt yuv420p -preset veryfast
        //归途如虹 101274ms 6.13M  -c:v libx264 -preset veryfast -r $fps -pix_fmt yuv420p
        command.append(" -c:v libopenh264 -b:v 4000k -r $fps -pix_fmt yuv420p")
        command.append(" -y '${outputFile.absolutePath}'")
        return command.toString() to outputDuration
    }

    override val outputFileName: String
        get() = "Book_${System.nanoTime()}_${shortDrama.dramasId}.mp4"

    private fun Long.assTime() = run {
        val minutes = this / DateUtils.MINUTE_IN_MILLIS
        val seconds = (this % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS
        val millis = (this % 1000) / 10
        String.format(Locale.getDefault(), "0:%02d:%02d.%02d", minutes, seconds, millis)
    }

}