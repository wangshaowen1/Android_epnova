package com.kuaigeglobal.epnova.feature.download.material

import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class UrlSourceMaterialItem(val url: String) : SourceMaterialItem() {

    override val filename by lazy { url.urlToFilename() }
    private val cacheFile by lazy {
        File("${PathUtils.getExternalAppCachePath()}${File.separator}source_material${File.separator}${filename}").apply {
            parentFile?.let { FileUtils.createOrExistsDir(it) }
        }
    }
    private var call: Call? = null
    private var preparing = false

    override fun prepare(listener: Listener) {
        super.prepare(listener)
        if (preparing) {
            return
        }
        preparing = true
        ioScope.launch {
            if (file.exists()) {
                onProgress(100)
                onSuccess()
                preparing = false
                return@launch
            }
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val request = Request.Builder().url(url).build()
                val call = RetrofitClient.httpClient.newCall(request)
                this@UrlSourceMaterialItem.call = call
                val body = call.execute().body!!
                val fileSize = body.contentLength()
                var fileSizeDownloaded = 0L
                inputStream = body.byteStream()
                outputStream = FileOutputStream(cacheFile)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    fileSizeDownloaded += bytesRead.toLong()
                    progress = ((fileSizeDownloaded * 100) / fileSize).toInt()
                    if (progress % 10 == 0) {
                        onProgress(progress)
                    }
                }
                val res = FileUtils.copy(cacheFile, file)
                if (res) {
                    onProgress(100)
                    onSuccess()
                    preparing = false
                } else {
                    onFail(Exception("素材下载失败 ${file.name}"))
                    preparing = false
                }
            } catch (e: Throwable) {
                onFail(Exception("素材下载失败 ${file.name}", e))
                preparing = false
            } finally {
                kotlin.runCatching {
                    outputStream?.close()
                    inputStream?.close()
                }
            }
        }
    }

    override fun cancel() {
        call?.cancel()
        preparing = false
    }
}