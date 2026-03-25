package com.kuaigeglobal.epnova.feature.download.transform

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.feature.download.material.Material
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 视频转换基类，封装通用转换代码
 */
abstract class BaseFFmpegTransform() : BaseTransform() {
    val projectId by lazy { System.nanoTime() }

    /**
     * 素材管理
     */
    protected abstract val sourceMaterial: Material

    /**
     * 下载素材进度占比，默认0.25f
     */
    protected open val downloadProgressRatio = 0.25f
    protected var session: FFmpegSession? = null
    protected var canceled = false

    /**
     * ffmpeg命令
     */
    protected abstract suspend fun commandAndDuration(outputFile: File): Pair<String, Long>

    init {
        FFmpegKitConfig.setFontDirectoryList(App.instance, listOf("/system/fonts"), emptyMap())
    }

    override suspend fun transform(outputFile: File, onSuccess: () -> Unit, onFail: (exportException: Throwable?) -> Unit, onProgress: (progress: Int) -> Unit) {
        val images = sourceMaterial.items
        if (images.isEmpty()) {
            onFail(TransformException("下载失败，没有素材"))
            return
        }
        //分步计算进度，下载+合成=2步
        var downloadProgress = 0
        var composeProgress = 0
        val updateProgress = { onProgress.invoke((downloadProgress * downloadProgressRatio + composeProgress * (1 - downloadProgressRatio)).toInt().coerceAtMost(100)) }
        //准备素材
        sourceMaterial.prepare(object : Material.Listener {
            override fun onProgress(progress: Int) {
                downloadProgress = progress
                updateProgress.invoke()
            }

            override fun onSuccess() {
                ioScope.launch {
                    try {
                        if (canceled) return@launch
                        val commandAndDuration = commandAndDuration(outputFile)
                        val command = commandAndDuration.first
                        val duration = commandAndDuration.second
                        Logger.i("ffmpeg command:$command")
                        session = FFmpegKit.executeAsync(
                            command,
                            { session ->
                                if (ReturnCode.isSuccess(session.returnCode)) {
                                    // SUCCESS
                                    Logger.i("ffmpeg success ${session?.duration}")
                                    MainScope().launch { onSuccess.invoke() }
                                } else if (ReturnCode.isCancel(session.returnCode)) {
                                    // CANCEL
                                } else {
                                    // FAILURE
                                    MainScope().launch { onFail.invoke(Exception("ffmpeg fail:\ncommand:$command\nlogs:${session.logsAsString}")) }
                                }
                            },
                            {
                                Logger.i("ffmpeg log:$it")
                            },
                            { statistics ->
                                MainScope().launch {
                                    composeProgress = (statistics.time * 100 / duration).toInt()
                                    updateProgress.invoke()
                                }
                            })
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) { onFail.invoke(e) }
                    }
                }
            }

            override fun onFail(e: Throwable) {
                onFail.invoke(e)
            }
        })
    }

    override fun cancel() {
        super.cancel()
        canceled = true
        sourceMaterial.cancel()
        session?.cancel()
        session = null
        ioScope.launchNoCrash {
            val srcDir = "${PathUtils.getExternalAppFilesPath()}${File.separator}source_material${File.separator}${projectId}"
            FileUtils.deleteAllInDir(srcDir)
        }
    }

}