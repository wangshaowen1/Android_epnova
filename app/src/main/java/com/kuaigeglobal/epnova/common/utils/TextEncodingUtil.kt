package com.kuaigeglobal.epnova.common.utils

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.kuaigeglobal.epnova.core.Config
import com.nhsd.basic.utils.Logger
import kotlinx.parcelize.Parcelize
import org.mozilla.universalchardet.UniversalDetector
import java.io.*
import java.nio.charset.Charset

object TextEncodingUtil {
    private const val TAG = "TextEncodingUtil"

    // 保留你原本的 50万字 熔断上限
    val MAX_CHAR_COUNT: Int
        get() = Config.get().appConfig.workNovelWordLimit ?: 600000

    val MIN_CHAR_COUNT: Int
        get() = 100000

    /**
     * 分析结果：同时包含 字符数、Token数、预估算力
     */
    @Parcelize
    data class AnalysisResult(
        val charCount: Int,
        val tokenCount: Int,
        var estimatedPower: Double
    ) : Parcelable

    /**
     * 整合后的处理结果
     */
    data class ProcessResult(
        val tempFile: File?,
        val analysisResult: AnalysisResult?
    )

    /**
     * 将用户选择的 TXT 转换为 UTF-8 编码的临时文件 边转换编码，边统计字数 (采用 CharArray 块读取，防止 OOM 并保留原格式)
     */
    fun createUtf8TempFileAndAnalyze(context: Context?, inputUri: Uri): ProcessResult {
        val ctx = context ?: return ProcessResult(null, null)
        val tempFile = File(ctx.cacheDir, "upload_temp_${System.currentTimeMillis()}.txt")

        var charCount = 0
        var tokenCount = 0
        var isExceedLimit = false // 标记是否超限

        try {
            // 1. 探测编码
            val encoding = detectEncoding(ctx, inputUri)
            val limit = MAX_CHAR_COUNT

            // 2. 块读取转换并统计
            ctx.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                tempFile.outputStream().use { fos ->
                    inputStream.bufferedReader(Charset.forName(encoding)).use { reader ->
                        fos.bufferedWriter(Charsets.UTF_8).use { writer ->

                            val buffer = CharArray(8192) // 每次读取 8K 字符
                            var readLen: Int
                            var isFirstRead = true

                            while (reader.read(buffer).also { readLen = it } != -1) {
                                var startIndex = 0

                                // 处理 UTF-8 的 BOM 头 (\uFEFF)
                                if (isFirstRead && readLen > 0 && buffer[0] == '\uFEFF') {
                                    startIndex = 1
                                }
                                isFirstRead = false

                                // 统计有效字数（剔除 \n 和 \r）
                                var validCharCountInBlock = 0
                                for (i in startIndex until readLen) {
                                    val c = buffer[i]
                                    if (c != '\n' && c != '\r') {
                                        validCharCountInBlock++
                                    }
                                }

                                charCount += validCharCountInBlock
                                tokenCount += validCharCountInBlock

                                // 熔断保护：如果已经超限
                                if (charCount > limit) {
                                    charCount = limit + 1
                                    tokenCount = charCount
                                    isExceedLimit = true
                                    break // 停止读取和写入
                                }

                                // 按照读取的实际长度，原样写入文件（完美保留原始换行符）
                                writer.write(buffer, startIndex, readLen - startIndex)
                            }
                        }
                    }
                }
            }

            val analysis = AnalysisResult(
                charCount = charCount,
                tokenCount = tokenCount,
                estimatedPower = calculateEstimatedPower(tokenCount)
            )

            // 3. 结果处理
            if (isExceedLimit) {
                // 如果超限了，反正也不会触发上传，直接删掉临时文件释放空间
                if (tempFile.exists()) tempFile.delete()
                // 返回 null 的文件路径，但保留超限的分析结果，供外部判断弹窗
                return ProcessResult(null, analysis)
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                return ProcessResult(tempFile, analysis)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }

        return ProcessResult(null, null)
    }

    private fun detectEncoding(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val detector = UniversalDetector(null)
            val buffer = ByteArray(8192)
            var bytesRead: Int

            Logger.d(TAG, "正在探测编码...")
            while (inputStream.read(buffer).also { bytesRead = it } > 0 && !detector.isDone) {
                detector.handleData(buffer, 0, bytesRead)
            }
            detector.dataEnd()

            val encoding = detector.detectedCharset
            detector.reset()

            val result = encoding ?: "UTF-8"
            Logger.d(TAG, "探测器返回结果: ${encoding ?: "Unknown (Fallback to UTF-8)"}")
            result
        } ?: "UTF-8"
    }

    /**
     * 计算预估算力值
     */
    fun calculateEstimatedPower(tokenCount: Int): Double {
        return (tokenCount.toDouble() / 1_000_000.0) * 50.0
    }
}