package com.kuaigeglobal.epnova.common.ext

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.utils.RealPathUtil
import com.kuaigeglobal.epnova.common.utils.Utils
import java.io.File
import java.util.Locale

fun Uri.getRealPath(): String? {
    return try {
        RealPathUtil.getRealPath(App.instance, this)
    } catch (e: Exception) {
        null
    }
}

fun Uri.getFileName(): String? {
    return try {
        Utils.getFileName(RealPathUtil.getRealPath(App.instance, this))
    } catch (e: Exception) {
        System.nanoTime().toString()
    }
}

fun Uri.getFileMeta(context: Context?): Pair<String, String> {
    if (context == null) {
        return Pair("unknow_file", "")
    }

    var fullName = ""

    // 1. 尝试通过 ContentResolver 查询
    if (this.scheme == "content") {
        try {
            context?.contentResolver?.query(this, null, null, null, null)?.use { cursor ->
                // 自检建议：不要硬编码列名，先确认列是否存在
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fullName = cursor.getString(nameIndex) ?: ""
                }
            }
        } catch (e: Exception) {
            // 异常处理：部分魔改系统在 query 时可能抛出 SecurityException
            e.printStackTrace()
        }
    }

    // 2. 兜底方案：路径截取 + URL 解码
    if (fullName.isEmpty()) {
        val path = this.path ?: ""
        // 自检：使用 Uri.decode 修复路径中的 %20 等乱码问题
        val decodedPath = Uri.decode(path)
        fullName = decodedPath.substringAfterLast('/')
    }

    // 3. 极端的兜底：如果还是拿不到名字，通过 MimeType 补全
    if (fullName.isEmpty() || !fullName.contains(".")) {
        val mimeType = context?.contentResolver?.getType(this)
        val extFromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
        if (fullName.isEmpty()) {
            fullName = "file_${System.currentTimeMillis()}.$extFromMime"
        } else if (!fullName.endsWith(".$extFromMime")) {
            // 如果文件名里没后缀，补齐它
            fullName = "$fullName.$extFromMime"
        }
    }

    // 4. 最终拆分
    val extension = fullName.substringAfterLast(".", "").lowercase(Locale.getDefault())
    val nameWithoutExt = fullName.substringBeforeLast(".", fullName).replace("《", "").replace("》", "")

    return nameWithoutExt to extension
}

fun Uri.getTempFilePathFromUri(context: Context?): String? {
    if (context == null) {
        return null
    }
    val contentResolver = context.contentResolver
    val fileName = getFileMeta(context).let { it.first + "." + it.second }
    val tempFile = File(context.cacheDir, fileName) // 放在缓存目录

    try {
        contentResolver.openInputStream(this)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream) // 核心操作：流拷贝
            }
        }
        return tempFile.absolutePath // 返回临时文件路径
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}