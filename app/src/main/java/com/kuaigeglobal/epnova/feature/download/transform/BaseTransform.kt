package com.kuaigeglobal.epnova.feature.download.transform

import androidx.annotation.CallSuper
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.ext.report
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.launchNoCrash
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * 视频转换基类，封装通用转换代码
 */
abstract class BaseTransform() {
    protected val cacheDir by lazy {
        val dir = File("${PathUtils.getExternalAppCachePath()}${File.separator}$cacheDirName")
        if (FileUtils.createOrExistsDir(dir) && clearCache) {
            FileUtils.deleteAllInDir(dir)
        }
        dir
    }
    protected val fileDir by lazy {
        val dir = File("${PathUtils.getExternalAppFilesPath()}${File.separator}transform")
        FileUtils.createOrExistsDir(dir)
        dir
    }
    protected var generating = false
    var transformListener: TransformListener? = null
    protected open val cacheDirName: String = "transform"
    protected open val clearCache: Boolean = true
    protected open val saveOutputFile: Boolean = false
    protected abstract val outputFileName: String

    /**
     * 开始转换
     */
    open fun transform() {
        val savedFile = File(fileDir, outputFileName)
        if (savedFile.exists()) {
            transformListener?.onTransformSuccess(savedFile)
            return
        }
        generating = true
        transformListener?.onTransformStart()
        MainScope().launch {
            try {
                //视频文件输出路径
                val outputFile = File(cacheDir, outputFileName)
                transform(
                    outputFile,
                    onSuccess = {
                        if (outputFile.exists()) {
                            if (saveOutputFile) {
                                //生成成功的视频复制到文件目录
                                ioScope.launchNoCrash { FileUtils.copy(outputFile, savedFile) }
                            }
                            transformListener?.onTransformSuccess(outputFile)
                        } else {
                            transformListener?.onTransformFail(R.string.video_file_missing.getResString())
                        }
                        cancel()
                    },
                    onFail = {
                        it?.report()
                        transformListener?.onTransformFail((it as? TransformException)?.message ?: R.string.video_composition_failed.getResString())
                        cancel()
                    },
                    onProgress = {
                        transformListener?.onTransformProgress(it)
                    }
                )
            } catch (e: Throwable) {
                e.report()
                transformListener?.onTransformFail((e as? TransformException)?.message ?: R.string.video_composition_failed.getResString())
                cancel()
            }
        }
    }

    protected abstract suspend fun transform(
        outputFile: File,
        onSuccess: () -> Unit,
        onFail: (exportException: Throwable?) -> Unit,
        onProgress: (progress: Int) -> Unit
    )

    @CallSuper
    open fun cancel() {
        transformListener = null
        generating = false
    }

}