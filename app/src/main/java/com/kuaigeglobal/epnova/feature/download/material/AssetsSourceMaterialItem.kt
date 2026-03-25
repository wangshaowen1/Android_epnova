package com.kuaigeglobal.epnova.feature.download.material

import com.blankj.utilcode.util.ResourceUtils
import com.kuaigeglobal.epnova.common.ext.ioScope
import kotlinx.coroutines.launch

class AssetsSourceMaterialItem(private val assetsPath: String) : SourceMaterialItem() {

    override val filename by lazy { assetsPath.urlToFilename() }

    override fun prepare(listener: Listener) {
        super.prepare(listener)
        ioScope.launch {
            if (file.exists()) {
                onProgress(100)
                onSuccess()
                return@launch
            }
            try {
                val res = ResourceUtils.copyFileFromAssets(assetsPath, file.path)
                if (res) {
                    onProgress(100)
                    onSuccess()
                } else {
                    onFail(Exception("Assets转存文件失败 ${file.name}"))
                }
            } catch (e: Throwable) {
                onFail(Exception("Assets转存文件失败 ${file.name}", e))
            }
        }
    }

    override fun cancel() {}
}