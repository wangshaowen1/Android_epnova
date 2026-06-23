package com.kuaigeglobal.epnova.feature.download.material

import android.text.format.DateUtils
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.blankj.utilcode.util.ResourceUtils
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.feature.download.DownloadRatioConfig
import com.kuaigeglobal.epnova.feature.download.transform.MicroAnimShortDramaTransform.Companion.durationRegex
import com.kuaigeglobal.epnova.feature.download.transform.TransformException
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MicroAnimSegmentSourceMaterialItem(
    private val imageUrl: String,
    private val audioUrl: String,
    private val subtitleContent: String?,
    private val config: DownloadRatioConfig,
    private val fps: Int,
    private val blockDurationS: Int,
    /**
     * 0: 向上移动
     * 1: 向下移动
     */
    private val direction: Int
) : SourceMaterialItem() {
    override val filename by lazy { "${toString()}.mp4".urlToFilename() }
    private val vw = config.vw
    private val vh = config.vh
    private val imageItem by lazy { MicroAnimImageSourceMaterialItem(imageUrl, config) }
    private val audioItem by lazy { UrlSourceMaterialItem(audioUrl) }
    private var imageDownloadProgress = 0
    private var imageDownloaded = false
    private var audioDownloadProgress = 0
    private var audioDownloaded = false
    private var composeProgress = 0
    private var canceled = false
    private var session: FFmpegSession? = null

    override fun prepare(listener: Listener) {
        super.prepare(listener)
        if (file.exists()) {
            MainScope().launch {
                onProgress(100)
                onSuccess()
            }
            return
        }
        imageItem.prepare(object : SourceMaterialItem.Listener {
            override fun onProgress(item: SourceMaterialItem, progress: Int) {
                imageDownloadProgress = progress
                updateProgress()
            }

            override fun onSuccess(item: SourceMaterialItem) {
                imageDownloaded = true
                generateVideoSegment()
            }

            override fun onFail(item: SourceMaterialItem, e: Throwable) {
                onPrepareFail(e)
            }
        })
        audioItem.prepare(object : SourceMaterialItem.Listener {
            override fun onProgress(item: SourceMaterialItem, progress: Int) {
                audioDownloadProgress = progress
                updateProgress()
            }

            override fun onSuccess(item: SourceMaterialItem) {
                audioDownloaded = true
                generateVideoSegment()
            }

            override fun onFail(item: SourceMaterialItem, e: Throwable) {
                onPrepareFail(e)
            }
        })
    }

    private fun generateVideoSegment() {
        if (!imageDownloaded || !audioDownloaded || canceled) {
            return
        }
        ioScope.launchNoCrash(
            block = {
                val session = FFmpegKit.execute("-i ${audioItem.file.absolutePath} -hide_banner")
                val output = session.allLogsAsString
                val match = durationRegex.find(output)
                if (match == null) {
                    throw TransformException("获取音频时长失败")
                }
                val (h, m, s) = match.destructured
                // 图片时长 = 时 * 3600 + 分 * 60 + 秒 + 空白 * 2
                val imageDuration = ((h.toInt() * 3600 + m.toInt() * 60 + s.toFloat() + blockDurationS * 2) * 1000).toLong()
                if (canceled) return@launchNoCrash

                val subtitle = runCatching {
                    val template = ResourceUtils.readAssets2String("lyrics_template.ass")
                        .replace("#PlayResX#", vw.toString())
                        .replace("#PlayResY#", vh.toString())
                        .replace("#Fontsize#", config.subtitleTextSize.toString())
                        .replace("#MarginV#", config.subtitleMarginBottom.toString())
                    StringBuilder().appendLine(template)
                }.getOrNull()
                val line = subtitleContent?.let {
                    if (it.length > config.subtitleLineMaxLength) it.windowed(config.subtitleLineMaxLength, config.subtitleLineMaxLength, true).joinToString("\\N") else it
                }
                subtitle?.appendLine("Dialogue: 0,${0L.assTime()},${imageDuration.assTime()},Karaoke,${line}")
                val subtitleFile = subtitle?.let {
                    val file = File(file.parentFile, file.name.replace(".mp4", "_subtitle.ass"))
                    file.writeText(it.toString())
                    file
                }

                val adelay = blockDurationS * 1000L
                val duration = imageDuration / 1000f
                val y = if (direction == 0) {
                    "max(${vh - imageItem.imageHeight},-n)"
                } else {
                    "min(${vh - imageItem.imageHeight}+n,0)"
                }
                val command = StringBuilder()
                command.append("-loop 1 -i '${imageItem.file.absolutePath}'")
                command.append(" -f lavfi -t ${duration} -i color=black:s=${vw}x${vh}:r=${fps}")
                command.append(" -i '${audioItem.file.absolutePath}'")
                val filter = StringBuilder()
                filter.append("[0:v]fps=${fps},scale=${vw}:-1[img];")
                filter.append("[1:v][img]overlay=x=0:y='$y':shortest=1[v];")
                val finalVideo = if (subtitleFile?.exists() == true) {
                    filter.append("[v]subtitles='${subtitleFile.absolutePath}':force_style='Outline=1,Shadow=0,PrimaryColour=&H00FFFFFF,SecondaryColour=&H80000000'[finalv];")
                    "finalv"
                } else "v"
                filter.append("[2:a]adelay=$adelay|$adelay[a];")
                command.append(" -filter_complex \"${filter}\"")
                command.append(" -map '[$finalVideo]' -map '[a]' -c:a aac -b:a 128k -c:v libopenh264 -b:v 4000k -r ${fps} -pix_fmt yuv420p -y '${file.absolutePath}'")
                Logger.i("$filename ffmpeg command:$command")
                this@MicroAnimSegmentSourceMaterialItem.session = FFmpegKit.executeAsync(
                    command.toString(),
                    { session ->
                        if (ReturnCode.isSuccess(session.returnCode)) {
                            // SUCCESS
                            Logger.i("$filename ffmpeg success ${session?.duration}")
                            MainScope().launch { onSuccess() }
                        } else if (ReturnCode.isCancel(session.returnCode)) {
                            // CANCEL
                        } else {
                            // FAILURE
                            onPrepareFail(Exception("$filename ffmpeg fail:\ncommand:$command\nlogs:${session.logsAsString}"))
                        }
                    },
                    {
                        Logger.i("$filename ffmpeg log:$it")
                    },
                    { statistics ->
                        MainScope().launch {
                            composeProgress = (statistics.time * 100 / imageDuration).toInt()
                            updateProgress()
                        }
                    })
            },
            onError = {
                onPrepareFail(it)
            }
        )
    }

    private fun Long.assTime() = run {
        val minutes = this / DateUtils.MINUTE_IN_MILLIS
        val seconds = (this % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS
        val millis = (this % 1000) / 10
        String.format(Locale.getDefault(), "0:%02d:%02d.%02d", minutes, seconds, millis)
    }

    private fun updateProgress() {
        //分步计算进度，下载图片+下载音频+合成=3步
        MainScope().launch { onProgress((imageDownloadProgress * 0.3f + audioDownloadProgress * 0.3f + composeProgress * 0.4f).toInt().coerceAtMost(100)) }
    }

    private fun onPrepareFail(e: Throwable) {
        cancel()
        MainScope().launch { onFail(e) }
    }

    override fun cancel() {
        canceled = true
        session?.cancel()
        imageItem.cancel()
        audioItem.cancel()
    }

    override fun toString(): String {
        return "MicroAnimSegmentSourceMaterialItem(imageUrl='$imageUrl', audioUrl='$audioUrl', config=$config, fps=$fps, blockDurationS=$blockDurationS, direction=$direction)"
    }
}