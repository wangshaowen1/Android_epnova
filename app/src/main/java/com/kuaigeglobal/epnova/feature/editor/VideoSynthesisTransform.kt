package com.kuaigeglobal.epnova.feature.editor

import com.kuaigeglobal.epnova.core.network.RetrofitClient
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaige.videoedit.ffmpeg.BaseFFmpegTransform
import com.kuaige.videoedit.ffmpeg.FFmpegCommandBuilder
import com.kuaige.videoedit.ffmpeg.FFmpegHelper
import com.kuaige.videoedit.resource.Resource
import java.io.File
import kotlin.div

/**
 * 音乐视频 (FFmpeg 实现)
 */
class VideoSynthesisTransform(
    storyboards: List<SimpleStoryboardInfo>
) : BaseFFmpegTransform() {

    override val resourceManager by lazy {
        VideoSynthesisResourceManager(storyboards.mapNotNull { storyboard ->
            storyboard.videoUrl?.let { VideoStoryboardResource(it, storyboard, false, RetrofitClient.httpClient) }
        })
    }

    override val outputFileName: String
        get() = "VideoSynthesis_${System.nanoTime()}.mp4"

    override suspend fun generateCommand(outputFile: File): Pair<String, Long> {
        val videos = resourceManager.storyboards?.takeIf { it.isNotEmpty() }
            ?: throw kotlin.IllegalStateException("Video materials missing")
        val builder = FFmpegCommandBuilder()
        val fileListFile = generateConcatFile(videos, outputFile)
        builder.addInput(fileListFile.absolutePath, "-fflags +genpts -f concat -safe 0")
        val extra = "-vf 'fps=30' -c:v libx264 -preset veryfast -c:a aac -b:a 128k -pix_fmt yuv420p"
        val duration = videos.sumOf { FFmpegHelper.parseDuration(it.file.absolutePath) } / 1000
        return builder.build(outputFile.absolutePath, extra) to duration
    }

    private fun generateConcatFile(videos: List<Resource>, outputFile: File): File {
        val sb = kotlin.text.StringBuilder()
        for (video in videos) {
            sb.append("file '${video.file.absolutePath}'\n")
        }
        val fileListFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_file_list.txt")
        fileListFile.writeText(sb.toString())
        return fileListFile
    }
}