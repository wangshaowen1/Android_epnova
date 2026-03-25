package com.kuaigeglobal.epnova.feature.download.material

import androidx.annotation.CallSuper
import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.common.ext.getUrlFileExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 单个素材管理（下载）
 */
abstract class SourceMaterialItem() {

    protected abstract val filename: String
    open val file: File by lazy {
        File("${PathUtils.getExternalAppFilesPath()}${File.separator}source_material${File.separator}${filename}").apply {
            parentFile?.let { FileUtils.createOrExistsDir(it) }
        }
    }
    var progress = 0
        protected set
    var listener: Listener? = null
    open val width = 0
    open val height = 0

    @CallSuper
    open fun prepare(listener: Listener) {
        this.listener = listener
    }

    protected fun String?.urlToFilename(): String {
        val extension = this?.getUrlFileExtension()?.let { ".$it" } ?: ""
        val sha256 = kotlin.runCatching { EncryptUtils.encryptSHA256ToString(this) }.getOrNull()
        if (sha256.isNullOrEmpty()) {
            return "${System.nanoTime()}$extension"
        }
        return "$sha256$extension"
    }

    abstract fun cancel()

    protected suspend fun onProgress(progress: Int) {
        withContext(Dispatchers.Main) {
            this@SourceMaterialItem.progress = progress
            listener?.onProgress(this@SourceMaterialItem, progress)
        }
    }

    protected suspend fun onSuccess() {
        withContext(Dispatchers.Main) {
            listener?.onSuccess(this@SourceMaterialItem)
        }
    }

    protected suspend fun onFail(e: Throwable) {
        withContext(Dispatchers.Main) {
            listener?.onFail(this@SourceMaterialItem, e)
        }
    }

    interface Listener {
        fun onProgress(item: SourceMaterialItem, progress: Int)
        fun onSuccess(item: SourceMaterialItem)
        fun onFail(item: SourceMaterialItem, e: Throwable)
    }

}