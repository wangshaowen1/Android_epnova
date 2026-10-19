package com.kuaigeglobal.epnova.feature.download.resource

import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ResourceUtils
import com.google.gson.reflect.TypeToken
import com.kuaigeglobal.epnova.core.network.apis.entity.SubtitleItem
import com.kuaige.videoedit.resource.Resource
import java.text.BreakIterator
import java.util.Locale

/**
 * 字幕转换（JSON -> ASS）
 */
class JsonToAssResource(
    private val storyboardId: String,
    private val json: String,
    private val assTemplate: String = "ass_template.ass",
    private val videoWidth: Int = 1280,
    private val videoHeight: Int = 720,
    private val fontSize: Int = 60,
    private val marginBottom: Int = 100,
) : Resource() {

    // 生成一个唯一的文件名
    override val filename by lazy {
        val styleKey = listOf(
            ResourceUtils.readAssets2String(assTemplate),
            videoWidth,
            videoHeight,
            fontSize,
            marginBottom,
            LAYOUT_VERSION,
        ).hashCode()
        "ass_${storyboardId}_${json.hashCode()}_${styleKey}.ass"
    }

    override suspend fun doWork(onProgress: ((Int) -> Unit)?) {
        val type = object : TypeToken<List<SubtitleItem>>() {}.type
        val subtitleList: List<SubtitleItem> = GsonUtils.fromJson(json, type)
        if (subtitleList.isEmpty()) {
            return
        }
        // 读取 ASS 模板
        val subtitle = runCatching {
            val template = ResourceUtils.readAssets2String(assTemplate)
                .replace("#PlayResX#", videoWidth.toString())
                .replace("#PlayResY#", videoHeight.toString())
                .replace("#Fontsize#", fontSize.toString())
                .replace("#MarginV#", marginBottom.toString())
            StringBuilder().appendLine(template)
        }.getOrNull() ?: throw IllegalStateException("Assets 中缺少 $assTemplate")

        // 遍历生成字幕行
        subtitleList.forEachIndexed { index, item ->
            subtitle.appendLine("Dialogue: 0,${item.startTime?.toAssTime()},${item.endTime?.toAssTime()},Default,${wrapText(item.text)}")
            // 简单的进度计算
            if (index % 5 == 0) {
                onProgress?.invoke((index * 100f / subtitleList.size).toInt())
            }
        }

        // 写入文件
        val saved = FileIOUtils.writeFileFromString(file, subtitle.toString())
        if (saved) {
            onProgress?.invoke(100)
        } else {
            throw Exception("歌词文件写入失败 ${file.name}")
        }
    }

    /**
     * 毫秒转 ASS 时间格式 (H:MM:SS.mm)
     */
    private fun Int.toAssTime(): String {
        val hours = this / DateUtils.HOUR_IN_MILLIS
        val remaining = this % DateUtils.HOUR_IN_MILLIS
        val minutes = remaining / DateUtils.MINUTE_IN_MILLIS
        val seconds = (remaining % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS
        val millis = (remaining % 1000) / 10 // ASS 使用百分秒 (cs)

        return String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", hours, minutes, seconds, millis)
    }

    /**
     * 字幕换行处理
     */
    private fun wrapText(text: String?): String {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSize.toFloat()
        }
        // 比 ASS 左右 80 像素边距再留少量余量，容纳字体回退和描边差异。
        val maxLineWidth = ((videoWidth - 80).coerceAtLeast(videoWidth / 2) * 0.9f)
        return text.orEmpty().replace("\r\n", "\n").replace('\r', '\n').split('\n').joinToString("\\N") { paragraph ->
            val lines = mutableListOf<String>()
            val line = StringBuilder()
            val characters = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(paragraph) }
            var start = characters.first()
            var end = characters.next()
            while (end != BreakIterator.DONE) {
                val character = paragraph.substring(start, end)
                if (line.isNotEmpty() && paint.measureText(line.toString() + character) > maxLineWidth) {
                    lines.add(line.toString().trimEnd())
                    line.setLength(0)
                }
                if (line.isNotEmpty() || !character.isBlank()) line.append(character)
                start = end
                end = characters.next()
            }
            if (line.isNotEmpty()) lines.add(line.toString().trimEnd())
            lines.joinToString("\\N")
        }
    }

    companion object {
        const val LAYOUT_VERSION = 2
    }
}