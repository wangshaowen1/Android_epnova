@file:JvmName("CommonUtils")

package com.kuaigeglobal.epnova.core.ext

import android.content.Intent
import android.net.Uri
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.ApiResponse
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

fun String.openWithBrowser() {
    val url = this
    if (url.isEmpty()) {
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        App.instance.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        "未找到浏览器应用".showToast()
    }
}

suspend fun ResponseBody.saveToFile(file: File): Boolean = withContext(Dispatchers.IO) {
    try {
        val inputStream = byteStream()
        val outputStream = FileOutputStream(file)

        val buffer = ByteArray(4096)
        var bytes: Int
        while (inputStream.read(buffer).also { bytes = it } != -1) {
            outputStream.write(buffer, 0, bytes)
        }

        outputStream.flush()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun ApiResponse<*>.exception() = ApiException(code, msg)

/**
 * 获取安装参数时请注意
 * 1. 不要在初始化之后，紧接着调用，容易导致获取数据失败；
 * 2. 在业务需要时再获取数据，保证SDK有充足的时间进行数据还原；
 * 3. 不要在返回AppData为空时，尝试使用此api进行重试获取数据。
 */
fun getInstall(delay: Int = 0, callback: ((installData: String?) -> Unit)? = null) {
    val installData = Config.get().installData
    if (installData.isNullOrEmpty()) {
        val getInstall = {
//            OpenInstall.getInstall(object : AppInstallAdapter() {
//                override fun onInstall(appData: AppData) {
//                    Logger.d(
//                        "OpenInstall",
//                        "getInstall : installData = $appData\n" +
//                                "opid = ${OpenInstall.getOpid()}"
//                    )
//                    Config.get().storeInstallData(appData)
//                    Config.get().storeOpid(OpenInstall.getOpid())
//                    callback?.invoke(Config.get().installData)
//                }
//            })
        }
        if (delay > 0) {
            App.instance.postDelay(getInstall, delay)
        } else {
            getInstall.invoke()
        }
    } else {
        callback?.invoke(installData)
    }
}


