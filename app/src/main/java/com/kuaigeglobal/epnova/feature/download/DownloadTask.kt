package com.kuaigeglobal.epnova.feature.download

import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.getUrlFileExtension
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.ext.report
import com.kuaigeglobal.epnova.core.network.HttpDownload
import com.kuaigeglobal.epnova.core.network.ProgressListener
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.launchNoCrash
import okhttp3.Call
import java.io.File

class DownloadTask(private val url: String, outputFileName: String = "") {
    private val cacheDir by lazy {
        val dir = File("${PathUtils.getExternalAppCachePath()}${File.separator}$cacheDirName")
        if (FileUtils.createOrExistsDir(dir) && clearCache) {
            FileUtils.deleteAllInDir(dir)
        }
        dir
    }
    private val fileDir by lazy {
        val dir = File("${PathUtils.getExternalAppFilesPath()}${File.separator}$cacheDirName")
        FileUtils.createOrExistsDir(dir)
        dir
    }
    private var generating = false
    var transformListener: DownloadListener? = null
    private val cacheDirName: String = "download"
    private val clearCache: Boolean = true
    private var call: Call? = null
    private val saveOutputFile: Boolean = true
    private val outputFileName: String by lazy { outputFileName.ifEmpty { url.urlToFilename() } }

    fun download() {
        val savedFile = File(fileDir, outputFileName)
        if (savedFile.exists()) {
            transformListener?.onDownloadSuccess(savedFile)
            return
        }
        generating = true
        transformListener?.onDownloadStart()
        try {
            //视频文件输出路径
            val outputFile = File(cacheDir, outputFileName)
            HttpDownload.download(url, outputFile, object : ProgressListener {
                override fun onProgress(bytesRead: Long, contentLength: Long) {
                    if (contentLength > 0) {
                        val progress = ((bytesRead * 100) / contentLength).toInt()
                        transformListener?.onDownloadProgress(progress)
                    }
                }

                override fun onResult(success: Boolean, reason: String?) {
                    if (success) {
                        if (outputFile.exists()) {
                            if (saveOutputFile) {
                                //生成成功的视频复制到文件目录
                                ioScope.launchNoCrash { FileUtils.copy(outputFile, savedFile) }
                            }
                            transformListener?.onDownloadSuccess(outputFile)
                        } else {
                            transformListener?.onDownloadFail(R.string.download_failed.getResString())
                        }
                    } else {
                        transformListener?.onDownloadFail(reason ?: R.string.download_failed.getResString())
                    }
                    cancel()
                }
            })
        } catch (e: Throwable) {
            e.report()
            transformListener?.onDownloadFail(R.string.download_failed.getResString())
            cancel()
        }
    }

    fun cancel() {
        transformListener = null
        generating = false
        call?.cancel()
    }

    protected fun String?.urlToFilename(): String {
        val extension = this?.getUrlFileExtension()
        val sha256 = kotlin.runCatching { EncryptUtils.encryptSHA256ToString(this) }.getOrNull()
        if (sha256.isNullOrEmpty()) {
            return "${System.nanoTime()}$extension"
        }
        return "$sha256$extension"
    }
}